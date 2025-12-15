package com.carrental.service;

import com.carrental.model.Contract;
import com.carrental.model.Handover;
import com.carrental.model.User;
import com.carrental.model.Vehicle;
import com.carrental.repository.ContractRepository;
import com.carrental.repository.HandoverRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class HandoverService {

    @Autowired
    private HandoverRepository handoverRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private ContractService contractService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private NotificationService notificationService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // File upload configuration
    private static final String UPLOAD_DIR = "uploads/handovers/";
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp", "gif");
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    public List<Handover> getAllHandovers() {
        return handoverRepository.findAll();
    }

    public Optional<Handover> getHandoverById(Long id) {
        return handoverRepository.findById(id);
    }

    public List<Handover> getHandoversByContract(Long contractId) {
        return handoverRepository.findByContractId(contractId);
    }

    public Handover createHandover(Handover handover) {
        handover.setHandoverTime(LocalDateTime.now());
        return handoverRepository.save(handover);
    }

    public Handover updateHandover(Long id, Handover handoverDetails) {
        Handover handover = handoverRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Handover not found"));

        handover.setOdometer(handoverDetails.getOdometer());
        handover.setFuelLevel(handoverDetails.getFuelLevel());
        handover.setConditionNotes(handoverDetails.getConditionNotes());
        handover.setImages(handoverDetails.getImages());

        return handoverRepository.save(handover);
    }

    /**
     * Perform vehicle pickup (check-in)
     * Creates handover record, uploads images, and updates vehicle status to Rented
     * 
     * @param contractId Contract ID
     * @param odometer Odometer reading
     * @param fuelLevel Fuel level (0-100)
     * @param conditionNotes Condition notes
     * @param images Vehicle condition images
     * @param staff Staff performing the pickup
     * @return Created handover record
     * @throws RuntimeException if validation fails
     * @throws IOException if image upload fails
     */
    public Handover performPickup(Long contractId, Integer odometer, Integer fuelLevel, 
                                  String conditionNotes, MultipartFile[] images, User staff) 
            throws IOException {
        // Validate contract
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        // Check contract status
        if (contract.getStatus() != Contract.ContractStatus.ACTIVE) {
            throw new RuntimeException("Contract must be in ACTIVE status for pickup. Current status: " + contract.getStatus());
        }

        // Check if pickup already done
        Optional<Handover> existingPickup = handoverRepository.findByContractIdAndType(contractId, Handover.HandoverType.PICKUP);
        if (existingPickup.isPresent()) {
            throw new RuntimeException("Pickup already completed for this contract");
        }

        // Check start date (should be today or in the past)
        LocalDateTime now = LocalDateTime.now();
        if (contract.getStartDate().isAfter(now)) {
            throw new RuntimeException("Cannot perform pickup before contract start date");
        }

        // Validate pickup data
        validatePickupData(odometer, fuelLevel, conditionNotes);

        // Upload images (optional - allow pickup without images)
        List<String> imageUrls = uploadHandoverImages(images, contractId);

        // Create handover record
        Handover handover = new Handover();
        handover.setContract(contract);
        handover.setStaff(staff);
        handover.setType(Handover.HandoverType.PICKUP);
        handover.setHandoverTime(now);
        handover.setOdometer(odometer);
        handover.setFuelLevel(fuelLevel);
        handover.setConditionNotes(conditionNotes);
        
        // Convert image URLs to JSON array if any images were uploaded
        if (!imageUrls.isEmpty()) {
            try {
                handover.setImages(objectMapper.writeValueAsString(imageUrls));
            } catch (Exception e) {
                throw new RuntimeException("Failed to save image URLs", e);
            }
        }

        // Save handover
        Handover savedHandover = handoverRepository.save(handover);

        // Update vehicle status to Rented
        vehicleService.updateVehicleStatus(contract.getVehicle().getId(), Vehicle.VehicleStatus.Rented);

        // Send notification to customer about successful pickup
        try {
            notificationService.createPickupCompletedNotification(
                contract.getCustomer().getId(),
                contract.getContractNumber(),
                contract.getVehicle().getModel().getModelName()
            );
        } catch (Exception e) {
            System.err.println("Failed to send pickup notification: " + e.getMessage());
        }

        return savedHandover;
    }

    /**
     * Validate pickup data
     */
    private void validatePickupData(Integer odometer, Integer fuelLevel, String conditionNotes) {
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
     * Upload handover images for a specific contract
     * 
     * @param files Array of image files to upload
     * @param contractId Contract ID for organizing files
     * @return List of relative URLs to uploaded images
     * @throws IOException if upload fails
     */
    private List<String> uploadHandoverImages(MultipartFile[] files, Long contractId) throws IOException {
        List<String> imageUrls = new ArrayList<>();

        if (files == null || files.length == 0) {
            return imageUrls;
        }

        // Create directory if it doesn't exist
        String contractDir = UPLOAD_DIR + contractId + "/";
        Path uploadPath = Paths.get(contractDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            // Validate file
            validateFile(file);

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String uniqueFilename = UUID.randomUUID().toString() + "." + extension;

            // Save file
            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Add relative URL to list
            imageUrls.add("/" + contractDir + uniqueFilename);
        }

        return imageUrls;
    }

    /**
     * Validate uploaded file
     */
    private void validateFile(MultipartFile file) {
        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of 5MB");
        }

        // Check file extension
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("Invalid filename");
        }

        String extension = getFileExtension(filename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("File type not allowed. Allowed types: " + ALLOWED_EXTENSIONS);
        }

        // Check content type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image");
        }
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }

    /**
     * Get contracts ready for pickup
     * Criteria: ACTIVE status and no pickup handover yet
     * Previously we hid future-dated contracts; however staff needs to prep as
     * soon as the customer has paid the deposit, so we no longer gate by startDate.
     *
     * @return List of contracts ready for pickup
     */
    public List<Contract> getContractsReadyForPickup() {
        List<Contract> activeContracts = contractService.getContractsByStatus(Contract.ContractStatus.ACTIVE);

        return activeContracts.stream()
                .filter(contract -> {
                    Optional<Handover> pickup = handoverRepository.findByContractIdAndType(
                            contract.getId(), Handover.HandoverType.PICKUP);
                    return pickup.isEmpty();
                })
                .toList();
    }

    /**
     * Get all active rentals (contracts with pickup completed)
     * 
     * @return List of active rental contracts
     */
    public List<Contract> getActiveRentals() {
        List<Contract> activeContracts = contractService.getContractsByStatus(Contract.ContractStatus.ACTIVE);

        return activeContracts.stream()
                .filter(contract -> {
                    Optional<Handover> pickup = handoverRepository.findByContractIdAndType(
                            contract.getId(), Handover.HandoverType.PICKUP);
                    return pickup.isPresent();
                })
                .toList();
    }

    /**
     * Get active rentals for a specific customer
     * 
     * @param customerId Customer ID
     * @return List of customer's active rental contracts
     */
    public List<Contract> getActiveRentalsByCustomer(Long customerId) {
        List<Contract> customerContracts = contractService.getContractsByCustomer(customerId);

        return customerContracts.stream()
                .filter(contract -> contract.getStatus() == Contract.ContractStatus.ACTIVE)
                .filter(contract -> {
                    Optional<Handover> pickup = handoverRepository.findByContractIdAndType(
                            contract.getId(), Handover.HandoverType.PICKUP);
                    return pickup.isPresent();
                })
                .toList();
    }

    /**
     * Cancel contract and initiate refund (used when pickup fails due to vehicle issues)
     * 
     * @param contractId Contract ID to cancel
     * @param reason Cancellation reason
     * @return Updated contract
     */
    public Contract cancelContractAndRefund(Long contractId, String reason) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        // Verify no pickup has been done
        Optional<Handover> existingPickup = handoverRepository.findByContractIdAndType(contractId, Handover.HandoverType.PICKUP);
        if (existingPickup.isPresent()) {
            throw new RuntimeException("Cannot cancel contract after pickup has been completed");
        }

        // Update contract status to CANCELLED
        contract.setStatus(Contract.ContractStatus.CANCELLED);
        Contract cancelledContract = contractRepository.save(contract);

        // Create refund payment
        try {
            paymentService.createRefundPayment(contractId, contract.getDepositAmount(), reason);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create refund payment: " + e.getMessage(), e);
        }

        // Vehicle status should remain AVAILABLE (it was never changed to Rented)

        return cancelledContract;
    }

    /**
     * Get pickup handover for a contract
     * 
     * @param contractId Contract ID
     * @return Pickup handover if exists
     */
    public Optional<Handover> getPickupHandover(Long contractId) {
        return handoverRepository.findByContractIdAndType(contractId, Handover.HandoverType.PICKUP);
    }

    /**
     * Check if contract has pickup completed
     * 
     * @param contractId Contract ID
     * @return true if pickup completed
     */
    public boolean hasPickupCompleted(Long contractId) {
        return handoverRepository.findByContractIdAndType(contractId, Handover.HandoverType.PICKUP).isPresent();
    }
}
