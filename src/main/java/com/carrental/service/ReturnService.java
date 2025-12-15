package com.carrental.service;

import com.carrental.config.FileUploadConfig;
import com.carrental.model.*;
import com.carrental.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ReturnService {

    @Autowired
    private HandoverRepository handoverRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ReturnFeeRepository returnFeeRepository;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private DepositHoldRepository depositHoldRepository;

    @Autowired
    private SystemSettingsRepository systemSettingsRepository;

    @Autowired
    private FileUploadConfig fileUploadConfig;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private BookingRepository bookingRepository;

    /**
     * Get contracts ready for return (active rentals with pickup completed but no return)
     */
    public List<Contract> getContractsReadyForReturn() {
        List<Contract> activeContracts = contractRepository.findByStatus(Contract.ContractStatus.ACTIVE);
        List<Contract> readyForReturn = new ArrayList<>();

        for (Contract contract : activeContracts) {
            // Check if pickup completed
            Optional<Handover> pickupHandover = handoverRepository
                    .findByContractIdAndType(contract.getId(), Handover.HandoverType.PICKUP);
            
            // Check if return NOT completed
            Optional<Handover> returnHandover = handoverRepository
                    .findByContractIdAndType(contract.getId(), Handover.HandoverType.RETURN);

            if (pickupHandover.isPresent() && returnHandover.isEmpty()) {
                readyForReturn.add(contract);
            }
        }

        return readyForReturn;
    }

    /**
     * Process vehicle return
     */
    @Transactional
    public Handover performReturn(Long contractId, LocalDateTime actualReturnDate, Integer odometer, Integer fuelLevel,
                                   String conditionNotes, Boolean hasDamage, String damageDescription,
                                   BigDecimal damageFee, Long returnLocationId, MultipartFile[] images,
                                   User staff) throws IOException {
        
        // Validate input
        validateReturnData(odometer, fuelLevel, conditionNotes, hasDamage, damageDescription, damageFee);

        // Get contract
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        // Verify contract is active
        if (contract.getStatus() != Contract.ContractStatus.ACTIVE) {
            throw new IllegalStateException("Contract is not active");
        }

        // Verify pickup completed
        Optional<Handover> pickupHandover = handoverRepository
                .findByContractIdAndType(contractId, Handover.HandoverType.PICKUP);
        if (pickupHandover.isEmpty()) {
            throw new IllegalStateException("Pickup not completed for this contract");
        }

        // Verify return not already done
        Optional<Handover> existingReturn = handoverRepository
                .findByContractIdAndType(contractId, Handover.HandoverType.RETURN);
        if (existingReturn.isPresent()) {
            throw new IllegalStateException("Return already completed for this contract");
        }

        // Validate actual return date is after start date
        if (actualReturnDate.isBefore(contract.getStartDate())) {
            throw new IllegalArgumentException("Actual return date cannot be before contract start date");
        }

        LocalDateTime returnTime = actualReturnDate;

        // Upload images
        List<String> imageUrls = uploadReturnImages(images, contractId);
        String imagesJson = convertImagesToJson(imageUrls);

        // Create return handover
        Handover returnHandover = new Handover();
        returnHandover.setContract(contract);
        returnHandover.setStaff(staff);
        returnHandover.setType(Handover.HandoverType.RETURN);
        returnHandover.setHandoverTime(returnTime);
        returnHandover.setOdometer(odometer);
        returnHandover.setFuelLevel(fuelLevel);
        returnHandover.setConditionNotes(conditionNotes);
        returnHandover.setImages(imagesJson);
        
        Handover savedHandover = handoverRepository.save(returnHandover);

        // Calculate actual rental days and fee adjustment
        Duration actualRentalDuration = Duration.between(contract.getStartDate(), returnTime);
        long actualDays = actualRentalDuration.toDays();
        if (actualDays < 1) {
            actualDays = 1; // Minimum 1 day
        }
        
        BigDecimal actualRentalFee = contract.getDailyRate().multiply(new BigDecimal(actualDays));
        BigDecimal rentalAdjustment = actualRentalFee.subtract(contract.getTotalRentalFee());
        
        // Calculate fees
        BigDecimal lateFee = BigDecimal.ZERO;
        BigDecimal daysLate = BigDecimal.ZERO;
        boolean isLate = false;

        if (returnTime.isAfter(contract.getEndDate())) {
            isLate = true;
            daysLate = calculateDaysLate(contract.getEndDate(), returnTime);
            lateFee = calculateLateFee(daysLate);
        }

        // Check if different location
        Vehicle vehicle = contract.getVehicle();
        boolean isDifferentLocation = !returnLocationId.equals(vehicle.getLocation().getId());
        BigDecimal oneWayFee = BigDecimal.ZERO;
        
        if (isDifferentLocation) {
            oneWayFee = calculateOneWayFee(actualRentalFee);
        }

        // Validate damage fee
        if (hasDamage && (damageDescription == null || damageDescription.trim().isEmpty())) {
            throw new IllegalArgumentException("Damage description is required when damage is reported");
        }
        if (hasDamage && (damageFee == null || damageFee.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new IllegalArgumentException("Damage fee must be greater than 0 when damage is reported");
        }
        if (!hasDamage) {
            damageFee = BigDecimal.ZERO;
            damageDescription = null;
        }

        // Calculate total fees (rental adjustment + late fee + damage fee + one-way fee)
        // Note: rentalAdjustment can be negative (early return) or positive (late return)
        BigDecimal totalFees = rentalAdjustment.add(lateFee).add(damageFee).add(oneWayFee);

        // Create return fee record
        ReturnFee returnFee = new ReturnFee();
        returnFee.setContract(contract);
        returnFee.setHandover(savedHandover);
        returnFee.setLate(isLate);
        returnFee.setHoursLate(daysLate); // Store days late in hoursLate field (for backward compatibility)
        returnFee.setLateFee(lateFee);
        returnFee.setHasDamage(hasDamage);
        returnFee.setDamageDescription(damageDescription);
        returnFee.setDamageFee(damageFee);
        returnFee.setDifferentLocation(isDifferentLocation);
        returnFee.setOneWayFee(oneWayFee);
        returnFee.setTotalFees(totalFees);
        
        returnFeeRepository.save(returnFee);

        // Update vehicle status and location
        vehicle.setStatus(Vehicle.VehicleStatus.AVAILABLE);
        if (isDifferentLocation) {
            Location newLocation = new Location();
            newLocation.setId(returnLocationId);
            vehicle.setLocation(newLocation);
        }
        vehicleService.updateVehicle(vehicle.getId(), vehicle, null);

        // Create deposit hold (14 days from return)
        createDepositHold(contract, totalFees, returnTime);

        // Update contract status to bill pending until customer pays the return bill
        contract.setStatus(Contract.ContractStatus.BILL_PENDING);
        contractRepository.save(contract);

        // Also mark the linked booking as completed so customer filters work
        Booking linkedBooking = contract.getBooking();
        if (linkedBooking != null) {
            linkedBooking.setStatus(Booking.BookingStatus.COMPLETED);
            bookingRepository.save(linkedBooking);
        }

        // Create bill payment after return (replaces Bill)
        Payment billPayment = paymentService.createBillPaymentAfterReturn(contractId, returnFee);

        // Create notification for customer to pay bill
        notificationService.createPaymentNotification(
            contract.getCustomer().getId(),
            contract.getContractNumber(),
            billPayment.getBillNumber(),
            billPayment.getAmount(), // totalAmount
            billPayment.getDepositAmount()
        );

        return savedHandover;
    }

    /**
     * Validate return data
     */
    private void validateReturnData(Integer odometer, Integer fuelLevel, String conditionNotes,
                                     Boolean hasDamage, String damageDescription, BigDecimal damageFee) {
        if (odometer == null || odometer < 0) {
            throw new IllegalArgumentException("Valid odometer reading is required");
        }
        if (fuelLevel == null || fuelLevel < 0 || fuelLevel > 100) {
            throw new IllegalArgumentException("Fuel level must be between 0 and 100");
        }
        if (conditionNotes == null || conditionNotes.trim().isEmpty()) {
            throw new IllegalArgumentException("Condition notes are required");
        }
    }

    /**
     * Calculate days late (rounded up)
     */
    private BigDecimal calculateDaysLate(LocalDateTime expectedReturn, LocalDateTime actualReturn) {
        Duration duration = Duration.between(expectedReturn, actualReturn);
        long days = duration.toDays();
        // If there are any remaining hours/minutes, round up to next day
        if (duration.toHours() % 24 > 0 || duration.toMinutes() % (24 * 60) > 0) {
            days += 1;
        }
        return new BigDecimal(days);
    }

    /**
     * Calculate late fee based on days late
     */
    private BigDecimal calculateLateFee(BigDecimal daysLate) {
        // Try to get late_fee_per_day setting, fallback to late_fee_per_hour * 24
        String lateFeePerDayStr = systemSettingsRepository.findById("late_fee_per_day")
                .map(s -> s.getSettingValue())
                .orElse(null);
        
        BigDecimal lateFeePerDay;
        if (lateFeePerDayStr != null) {
            lateFeePerDay = new BigDecimal(lateFeePerDayStr);
        } else {
            // Fallback: use late_fee_per_hour * 24
            String lateFeePerHourStr = systemSettingsRepository.findById("late_fee_per_hour")
                    .map(s -> s.getSettingValue())
                    .orElse("50000");
            BigDecimal lateFeePerHour = new BigDecimal(lateFeePerHourStr);
            lateFeePerDay = lateFeePerHour.multiply(new BigDecimal(24));
        }
        
        return daysLate.multiply(lateFeePerDay).setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Calculate one-way fee
     */
    private BigDecimal calculateOneWayFee(BigDecimal totalRentalFee) {
        String oneWayPercentStr = systemSettingsRepository.findById("one_way_fee_percent")
                .map(s -> s.getSettingValue())
                .orElse("5");
        
        BigDecimal oneWayPercent = new BigDecimal(oneWayPercentStr);
        return totalRentalFee.multiply(oneWayPercent)
                .divide(new BigDecimal(100), 0, RoundingMode.HALF_UP);
    }

    /**
     * Upload return images
     */
    private List<String> uploadReturnImages(MultipartFile[] files, Long contractId) throws IOException {
        List<String> imageUrls = new ArrayList<>();
        
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("At least one image is required");
        }

        String uploadDir = fileUploadConfig.getUploadDir() + "/handovers/return/" + contractId;
        Path uploadPath = Paths.get(uploadDir);
        
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            validateFile(file);

            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String filename = UUID.randomUUID().toString() + extension;
            
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            String relativeUrl = "/uploads/handovers/return/" + contractId + "/" + filename;
            imageUrls.add(relativeUrl);
        }

        return imageUrls;
    }

    /**
     * Validate uploaded file
     */
    private void validateFile(MultipartFile file) {
        // Check file size (max 5MB)
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 5MB: " + file.getOriginalFilename());
        }

        // Check file type
        String contentType = file.getContentType();
        List<String> allowedTypes = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/webp");
        if (contentType == null || !allowedTypes.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid file type. Only JPG, PNG, and WEBP are allowed: " + file.getOriginalFilename());
        }
    }

    /**
     * Get file extension
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }

    /**
     * Convert image URLs to JSON array string
     */
    private String convertImagesToJson(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return "[]";
        }
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < imageUrls.size(); i++) {
            json.append("\"").append(imageUrls.get(i)).append("\"");
            if (i < imageUrls.size() - 1) {
                json.append(",");
            }
        }
        json.append("]");
        return json.toString();
    }

    /**
     * Create deposit hold record
     */
    private void createDepositHold(Contract contract, BigDecimal totalFees, LocalDateTime returnTime) {
        String holdDaysStr = systemSettingsRepository.findById("deposit_hold_days")
                .map(s -> s.getSettingValue())
                .orElse("14");

        int holdDays = Integer.parseInt(holdDaysStr);
        LocalDateTime holdEndDate = returnTime.plusDays(holdDays);

        DepositHold depositHold = new DepositHold();
        depositHold.setContract(contract);
        depositHold.setDepositAmount(contract.getDepositAmount());

        // Calculate actual deduction from deposit:
        // - If totalFees is negative (early return refund), deduction is 0
        // - If totalFees is positive but less than deposit, deduction equals totalFees
        // - If totalFees exceeds deposit, deduction equals deposit (customer owes more)
        BigDecimal actualDeduction;
        if (totalFees.compareTo(BigDecimal.ZERO) <= 0) {
            // Early return or no fees - no deduction from deposit
            actualDeduction = BigDecimal.ZERO;
        } else if (totalFees.compareTo(contract.getDepositAmount()) <= 0) {
            // Fees within deposit amount
            actualDeduction = totalFees;
        } else {
            // Fees exceed deposit - deduct entire deposit
            actualDeduction = contract.getDepositAmount();
        }

        depositHold.setDeductedAtReturn(actualDeduction);
        depositHold.setHoldStartDate(returnTime);
        depositHold.setHoldEndDate(holdEndDate);
        depositHold.setStatus(DepositHold.DepositStatus.HOLDING);

        // Debug logging
        System.out.println("=== Creating DepositHold ===");
        System.out.println("Contract ID: " + contract.getId());
        System.out.println("Deposit Amount: " + contract.getDepositAmount());
        System.out.println("Total Fees: " + totalFees);
        System.out.println("Actual Deduction: " + actualDeduction);
        System.out.println("===========================");

        depositHoldRepository.save(depositHold);
    }

    /**
     * Get return fee by contract
     */
    public Optional<ReturnFee> getReturnFeeByContract(Long contractId) {
        return returnFeeRepository.findByContractId(contractId);
    }

    /**
     * Check if return completed for contract
     */
    public boolean hasReturnCompleted(Long contractId) {
        return handoverRepository.findByContractIdAndType(contractId, Handover.HandoverType.RETURN).isPresent();
    }
}
