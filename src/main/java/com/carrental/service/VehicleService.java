package com.carrental.service;

import com.carrental.model.Vehicle;
import com.carrental.model.VehicleModel;
import com.carrental.model.Location;
import com.carrental.model.Vehicle.VehicleStatus;
import com.carrental.repository.VehicleRepository;
import com.carrental.repository.VehicleModelRepository;
import com.carrental.repository.LocationRepository;
import com.carrental.repository.BookingRepository;
import com.carrental.repository.ContractRepository;
import com.carrental.model.Booking;
import com.carrental.model.Contract;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class VehicleService {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private VehicleModelRepository vehicleModelRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ContractRepository contractRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // Đường dẫn lưu ảnh - sử dụng thư mục ngoài classpath để tránh vấn đề với DevTools
    // Thư mục này sẽ được Spring Boot tự động serve như static resource
    private static final String UPLOAD_DIR = "uploads/images/";

    public List<Vehicle> getAllVehicles() {
        // DISTINCT is now handled in the JPQL query itself
        return vehicleRepository.findAllWithRelations();
    }

    public Optional<Vehicle> getVehicleById(Long id) {
        // Sử dụng findByIdWithRelations để load tất cả relationships
        // Tránh LazyInitializationException khi render template
        return vehicleRepository.findByIdWithRelations(id);
    }

    public List<Vehicle> getAvailableVehicles() {
        // Sử dụng method với JOIN FETCH để load tất cả relationships (model, brand, location)
        // Đảm bảo dữ liệu có sẵn khi render template, tránh LazyInitializationException
        List<Vehicle> vehicles = vehicleRepository.findByStatusWithRelations(VehicleStatus.Available);
        // Remove duplicates caused by JOIN FETCH
        // Filter out vehicles that have active bookings or contracts
        LocalDateTime now = LocalDateTime.now();
        return vehicles.stream()
                .distinct()
                .filter(v -> isVehicleAvailableNow(v.getId()))
                .collect(Collectors.toList());
    }
    
    /**
     * UC04: Browse Vehicles - Tìm kiếm và lọc xe (chỉ Available - giữ lại cho backward compatibility)
     * Tìm kiếm xe theo nhiều tiêu chí: brand, category, giá, số chỗ, transmission, fuel, keyword
     * @deprecated Sử dụng searchAllVehiclesForCustomer() để hiển thị tất cả xe
     */
    @Deprecated
    public List<Vehicle> searchVehicles(
            Long brandId,
            String category,
            BigDecimal maxPrice,
            Integer minSeats,
            String transmission,
            String fuelType,
            String searchKeyword) {
        List<Vehicle> vehicles = vehicleRepository.searchVehicles(
            VehicleStatus.Available,
            brandId,
            category,
            maxPrice,
            minSeats,
            transmission,
            fuelType,
            searchKeyword
        );
        // Remove duplicates caused by JOIN FETCH
        // Filter out vehicles that have active bookings or contracts
        LocalDateTime now = LocalDateTime.now();
        return vehicles.stream()
                .distinct()
                .filter(v -> isVehicleAvailableNow(v.getId()))
                .collect(Collectors.toList());
    }

    /**
     * UC04: Browse Vehicles - Tìm kiếm và lọc xe (tất cả trạng thái)
     * Tìm kiếm xe theo nhiều tiêu chí: brand, category, giá, số chỗ, transmission, fuel, keyword
     * Hiển thị tất cả xe ở mọi trạng thái (Available, Rented, Maintenance)
     */
    public List<Vehicle> searchAllVehiclesForCustomer(
            Long brandId,
            String category,
            BigDecimal maxPrice,
            Integer minSeats,
            String transmission,
            String fuelType,
            String searchKeyword) {
        // Không filter theo status (null = tất cả status)
        List<Vehicle> vehicles = vehicleRepository.searchVehicles(
            null, // status = null để lấy tất cả
            brandId,
            category,
            maxPrice,
            minSeats,
            transmission,
            fuelType,
            searchKeyword
        );
        // Remove duplicates caused by JOIN FETCH
        return vehicles.stream()
                .distinct()
                .collect(Collectors.toList());
    }
    
    /**
     * UC05: Manage Vehicles - Admin tìm kiếm tất cả xe (không chỉ Available)
     */
    public List<Vehicle> searchAllVehicles(
            Long brandId,
            String category,
            VehicleStatus status,
            String searchKeyword) {
        // Sử dụng query có điều kiện
        List<Vehicle> vehicles = vehicleRepository.searchVehicles(
            status,
            brandId,
            category,
            null, // maxPrice
            null, // minSeats
            null, // transmission
            null, // fuelType
            searchKeyword
        );
        // Remove duplicates caused by JOIN FETCH
        return vehicles.stream()
                .distinct()
                .collect(Collectors.toList());
    }
    
    /**
     * Lấy danh sách các category có trong hệ thống để hiển thị trong filter
     */
    public List<String> getAllCategories() {
        return vehicleRepository.findAllCategories();
    }
    
    /**
     * Lấy danh sách các loại transmission để hiển thị trong filter
     */
    public List<String> getAllTransmissions() {
        return vehicleRepository.findAllTransmissions();
    }
    
    /**
     * Lấy danh sách các loại fuel type để hiển thị trong filter
     */
    public List<String> getAllFuelTypes() {
        return vehicleRepository.findAllFuelTypes();
    }

    public List<Vehicle> getVehiclesByLocation(Long locationId) {
        return vehicleRepository.findByLocationId(locationId);
    }
    
    /**
     * UC05: Manage Vehicles - Lấy tất cả vehicle models
     */
    public List<VehicleModel> getAllVehicleModels() {
        return vehicleModelRepository.findAllWithBrand();
    }
    
    /**
     * UC05: Manage Vehicles - Lấy tất cả locations
     */
    public List<Location> getAllLocations() {
        return locationRepository.findAll();
    }

    /**
     * UC05: Manage Vehicles - Tạo xe mới
     */
    @Transactional
    public Vehicle createVehicle(Vehicle vehicle, List<MultipartFile> imageFiles) throws IOException {
        // Validate license plate
        if (vehicle.getLicensePlate() == null || vehicle.getLicensePlate().trim().isEmpty()) {
            throw new IllegalArgumentException("Biển số xe không được để trống");
        }

        // Check duplicate license plate
        if (vehicleRepository.existsByLicensePlate(vehicle.getLicensePlate().trim())) {
            throw new IllegalArgumentException("Biển số xe " + vehicle.getLicensePlate() + " đã tồn tại trong hệ thống");
        }

        // Validate model và location
        if (vehicle.getModel() == null || vehicle.getModel().getId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn mẫu xe");
        }
        if (vehicle.getLocation() == null || vehicle.getLocation().getId() == null) {
            throw new IllegalArgumentException("Vui lòng chọn địa điểm");
        }

        // Validate daily rate
        if (vehicle.getDailyRate() == null || vehicle.getDailyRate().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá thuê phải lớn hơn 0");
        }

        // Validate deposit amount
        if (vehicle.getDepositAmount() == null || vehicle.getDepositAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Tiền đặt cọc phải lớn hơn 0");
        }

        // Validate deposit > daily rate
        if (vehicle.getDepositAmount().compareTo(vehicle.getDailyRate()) <= 0) {
            throw new IllegalArgumentException("Tiền đặt cọc phải lớn hơn giá thuê/ngày");
        }

        // Load model và location từ database
        VehicleModel model = vehicleModelRepository.findById(vehicle.getModel().getId())
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mẫu xe"));
        Location location = locationRepository.findById(vehicle.getLocation().getId())
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy địa điểm"));

        vehicle.setModel(model);
        vehicle.setLocation(location);

        // Set status mặc định là Available
        if (vehicle.getStatus() == null) {
            vehicle.setStatus(VehicleStatus.Available);
        }

        // Upload và lưu ảnh
        if (imageFiles != null && !imageFiles.isEmpty()) {
            List<String> imageUrls = uploadImages(imageFiles);
            vehicle.setImages(convertListToJson(imageUrls));
        }

        // Save the vehicle
        return vehicleRepository.save(vehicle);
    }

    /**
     * UC05: Manage Vehicles - Cập nhật thông tin xe
     */
    @Transactional
    public Vehicle updateVehicle(Long id, Vehicle vehicleDetails, List<MultipartFile> imageFiles) throws IOException {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + id));

        // Validate and check duplicate license plate (if changed)
        if (vehicleDetails.getLicensePlate() != null && 
            !vehicleDetails.getLicensePlate().trim().isEmpty() &&
            !vehicleDetails.getLicensePlate().equals(vehicle.getLicensePlate())) {
            
            if (vehicleRepository.existsByLicensePlateAndIdNot(vehicleDetails.getLicensePlate().trim(), id)) {
                throw new IllegalArgumentException("Biển số xe " + vehicleDetails.getLicensePlate() + " đã tồn tại trong hệ thống");
            }
            vehicle.setLicensePlate(vehicleDetails.getLicensePlate());
        }

        // Cập nhật model nếu có
        if (vehicleDetails.getModel() != null && vehicleDetails.getModel().getId() != null) {
            VehicleModel model = vehicleModelRepository.findById(vehicleDetails.getModel().getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mẫu xe"));
            vehicle.setModel(model);
        }
        
        // Cập nhật location nếu có
        if (vehicleDetails.getLocation() != null && vehicleDetails.getLocation().getId() != null) {
            Location location = locationRepository.findById(vehicleDetails.getLocation().getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy địa điểm"));
            vehicle.setLocation(location);
        }
        
        // Cập nhật các trường khác
        if (vehicleDetails.getColor() != null) {
            vehicle.setColor(vehicleDetails.getColor());
        }
        if (vehicleDetails.getDailyRate() != null) {
            if (vehicleDetails.getDailyRate().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Giá thuê phải lớn hơn 0");
            }
            vehicle.setDailyRate(vehicleDetails.getDailyRate());
        }
        if (vehicleDetails.getDepositAmount() != null) {
            if (vehicleDetails.getDepositAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Tiền đặt cọc phải lớn hơn 0");
            }
            if (vehicleDetails.getDepositAmount().compareTo(vehicle.getDailyRate()) <= 0) {
                throw new IllegalArgumentException("Tiền đặt cọc phải lớn hơn giá thuê/ngày");
            }
            vehicle.setDepositAmount(vehicleDetails.getDepositAmount());
        }
        if (vehicleDetails.getStatus() != null) {
            vehicle.setStatus(vehicleDetails.getStatus());
        }
        
        // Upload ảnh mới nếu có
        if (imageFiles != null && !imageFiles.isEmpty()) {
            List<String> imageUrls = uploadImages(imageFiles);
            vehicle.setImages(convertListToJson(imageUrls));
        }

        // Save the vehicle
        return vehicleRepository.save(vehicle);
    }

    /**
     * UC05: Manage Vehicles - Xóa xe
     */
    @Transactional
    public void deleteVehicle(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + id));
        
        // Kiểm tra xe có đang được thuê hay không
        if (vehicle.getStatus() == VehicleStatus.Rented) {
            throw new IllegalStateException("Cannot delete vehicle that is currently rented");
        }
        
        vehicleRepository.deleteById(id);
    }

    /**
     * UC05: Manage Vehicles - Cập nhật trạng thái xe
     */
    @Transactional
    public Vehicle updateVehicleStatus(Long id, VehicleStatus status) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + id));
        vehicle.setStatus(status);
        return vehicleRepository.save(vehicle);
    }
    
    /**
     * Check if vehicle is available for a specific date range
     * Returns true if vehicle has no active bookings or contracts overlapping the date range
     * Allows advance booking for vehicles that are currently Rented, as long as the requested
     * date range doesn't overlap with existing bookings/contracts
     */
    public boolean isVehicleAvailableForDateRange(Long vehicleId, LocalDateTime startDate, LocalDateTime endDate) {
        System.out.println("=== VEHICLE AVAILABILITY CHECK ===");
        System.out.println("Vehicle ID: " + vehicleId);
        System.out.println("Requested Start Date: " + startDate);
        System.out.println("Requested End Date: " + endDate);
        
        Optional<Vehicle> vehicleOpt = vehicleRepository.findById(vehicleId);
        if (vehicleOpt.isEmpty()) {
            System.out.println("Vehicle not found");
            return false;
        }
        Vehicle vehicle = vehicleOpt.get();
        System.out.println("Vehicle Status: " + vehicle.getStatus());
        
        // If vehicle is in Maintenance, it's never available
        if (vehicle.getStatus() == VehicleStatus.Maintenance) {
            System.out.println("Vehicle is in Maintenance - NOT AVAILABLE");
            return false;
        }

        // Check for active bookings that overlap with the requested date range
        // This works for both Available and Rented vehicles
        long activeBookings = bookingRepository.countActiveBookingsForDateRange(vehicleId, startDate, endDate);
        System.out.println("Active bookings overlapping: " + activeBookings);
        if (activeBookings > 0) {
            System.out.println("Found overlapping bookings - NOT AVAILABLE");
            return false;
        }

        // Check for active contracts that overlap with the requested date range
        long activeContracts = contractRepository.countActiveContractsForDateRange(vehicleId, startDate, endDate);
        System.out.println("Active contracts overlapping: " + activeContracts);
        if (activeContracts > 0) {
            System.out.println("Found overlapping contracts - NOT AVAILABLE");
            return false;
        }

        // Vehicle is available for the requested date range
        // (even if currently Rented, as long as there's no overlap)
        System.out.println("Vehicle is AVAILABLE for the requested date range");
        return true;
    }

    /**
     * Check if vehicle is available right now (at current time)
     * Returns true if vehicle has no active bookings or contracts at current time
     */
    public boolean isVehicleAvailableNow(Long vehicleId) {
        // Check if vehicle status is not Available or Maintenance
        Optional<Vehicle> vehicleOpt = vehicleRepository.findById(vehicleId);
        if (vehicleOpt.isEmpty()) {
            return false;
        }
        Vehicle vehicle = vehicleOpt.get();
        if (vehicle.getStatus() != VehicleStatus.Available) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();

        // Check for active bookings
        long activeBookings = bookingRepository.countActiveBookingsAtTime(vehicleId, now);
        if (activeBookings > 0) {
            return false;
        }

        // Check for active contracts
        long activeContracts = contractRepository.countActiveContractsAtTime(vehicleId, now);
        if (activeContracts > 0) {
            return false;
        }

        return true;
    }

    /**
     * Get effective availability status of vehicle based on bookings and contracts
     * This calculates the actual status dynamically
     */
    public VehicleStatus getEffectiveVehicleStatus(Long vehicleId) {
        Optional<Vehicle> vehicleOpt = vehicleRepository.findById(vehicleId);
        if (vehicleOpt.isEmpty()) {
            return VehicleStatus.Maintenance; // Default to unavailable if not found
        }
        Vehicle vehicle = vehicleOpt.get();

        // If status is Maintenance, always return Maintenance
        if (vehicle.getStatus() == VehicleStatus.Maintenance) {
            return VehicleStatus.Maintenance;
        }

        LocalDateTime now = LocalDateTime.now();

        // Check for active bookings
        long activeBookings = bookingRepository.countActiveBookingsAtTime(vehicleId, now);
        if (activeBookings > 0) {
            return VehicleStatus.Rented;
        }

        // Check for active contracts
        long activeContracts = contractRepository.countActiveContractsAtTime(vehicleId, now);
        if (activeContracts > 0) {
            return VehicleStatus.Rented;
        }

        // If no active bookings/contracts and status is Available, return Available
        return VehicleStatus.Available;
    }

    /**
     * Get next available date for a vehicle
     * Returns the first date when vehicle becomes available after current bookings/contracts
     */
    public LocalDateTime getNextAvailableDate(Long vehicleId) {
        LocalDateTime now = LocalDateTime.now();
        
        // Get all active bookings and contracts
        List<Booking> activeBookings = bookingRepository.findActiveBookingsInRange(vehicleId, now);
        List<Contract> activeContracts = contractRepository.findActiveContractsInRange(vehicleId, now);
        
        // Find the latest end date
        LocalDateTime latestEndDate = now;
        
        for (Booking booking : activeBookings) {
            if (booking.getEndDate().isAfter(latestEndDate)) {
                latestEndDate = booking.getEndDate();
            }
        }
        
        for (Contract contract : activeContracts) {
            if (contract.getEndDate().isAfter(latestEndDate)) {
                latestEndDate = contract.getEndDate();
            }
        }
        
        // If no active bookings/contracts, return now
        if (latestEndDate.equals(now)) {
            return now;
        }
        
        // Return the day after the latest end date
        return latestEndDate.plusDays(1).withHour(0).withMinute(0).withSecond(0);
    }

    /**
     * Get availability periods for a vehicle
     * Returns a list of date ranges showing when vehicle is available/unavailable
     * Format: List of maps with "start", "end", "available" keys
     */
    public List<java.util.Map<String, Object>> getAvailabilityPeriods(Long vehicleId, int daysAhead) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = now.plusDays(daysAhead);
        
        // Get all active bookings and contracts
        List<Booking> activeBookings = bookingRepository.findActiveBookingsInRange(vehicleId, now);
        List<Contract> activeContracts = contractRepository.findActiveContractsInRange(vehicleId, now);
        
        // Combine all blocked periods
        List<java.util.Map<String, Object>> blockedPeriods = new ArrayList<>();
        
        for (Booking booking : activeBookings) {
            java.util.Map<String, Object> period = new java.util.HashMap<>();
            period.put("start", booking.getStartDate());
            period.put("end", booking.getEndDate());
            period.put("type", "booking");
            blockedPeriods.add(period);
        }
        
        for (Contract contract : activeContracts) {
            java.util.Map<String, Object> period = new java.util.HashMap<>();
            period.put("start", contract.getStartDate());
            period.put("end", contract.getEndDate());
            period.put("type", "contract");
            blockedPeriods.add(period);
        }
        
        // Sort by start date
        blockedPeriods.sort(Comparator.comparing(p -> (LocalDateTime) p.get("start")));
        
        // Build availability periods
        List<java.util.Map<String, Object>> availabilityPeriods = new ArrayList<>();
        LocalDateTime currentDate = now;
        
        for (java.util.Map<String, Object> blocked : blockedPeriods) {
            LocalDateTime blockStart = (LocalDateTime) blocked.get("start");
            LocalDateTime blockEnd = (LocalDateTime) blocked.get("end");
            
            // If there's a gap before this block, add available period
            if (currentDate.isBefore(blockStart)) {
                java.util.Map<String, Object> available = new java.util.HashMap<>();
                available.put("start", currentDate);
                available.put("end", blockStart.minusSeconds(1));
                available.put("available", true);
                availabilityPeriods.add(available);
            }
            
            // Add blocked period
            java.util.Map<String, Object> unavailable = new java.util.HashMap<>();
            unavailable.put("start", blockStart);
            unavailable.put("end", blockEnd);
            unavailable.put("available", false);
            unavailable.put("type", blocked.get("type"));
            availabilityPeriods.add(unavailable);
            
            currentDate = blockEnd.plusSeconds(1);
        }
        
        // Add remaining available period if any
        if (currentDate.isBefore(endDate)) {
            java.util.Map<String, Object> available = new java.util.HashMap<>();
            available.put("start", currentDate);
            available.put("end", endDate);
            available.put("available", true);
            availabilityPeriods.add(available);
        }
        
        return availabilityPeriods;
    }

    /**
     * Get list of blocked dates (dates that cannot be selected for start date)
     * Returns list of date strings in format "YYYY-MM-DD"
     */
    public List<String> getBlockedDates(Long vehicleId, int daysAhead) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = now.plusDays(daysAhead);
        
        // Get all active bookings and contracts
        List<Booking> activeBookings = bookingRepository.findActiveBookingsInRange(vehicleId, now);
        List<Contract> activeContracts = contractRepository.findActiveContractsInRange(vehicleId, now);
        
        List<String> blockedDates = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        // Add dates from bookings
        for (Booking booking : activeBookings) {
            LocalDateTime start = booking.getStartDate();
            LocalDateTime end = booking.getEndDate();
            LocalDateTime current = start;
            
            while (!current.isAfter(end) && !current.isAfter(endDate)) {
                blockedDates.add(current.format(formatter));
                current = current.plusDays(1);
            }
        }
        
        // Add dates from contracts
        for (Contract contract : activeContracts) {
            LocalDateTime start = contract.getStartDate();
            LocalDateTime end = contract.getEndDate();
            LocalDateTime current = start;
            
            while (!current.isAfter(end) && !current.isAfter(endDate)) {
                String dateStr = current.format(formatter);
                if (!blockedDates.contains(dateStr)) {
                    blockedDates.add(dateStr);
                }
                current = current.plusDays(1);
            }
        }
        
        return blockedDates;
    }

    /**
     * Sync vehicle statuses based on current bookings and contracts
     * Updates vehicles that should be Rented but are still marked as Available
     * This method should be called periodically or when needed
     */
    @Transactional
    public int syncVehicleStatuses() {
        List<Vehicle> allVehicles = vehicleRepository.findAll();
        int updatedCount = 0;
        LocalDateTime now = LocalDateTime.now();
        
        for (Vehicle vehicle : allVehicles) {
            // Skip maintenance vehicles
            if (vehicle.getStatus() == VehicleStatus.Maintenance) {
                continue;
            }
            
            // Get effective status based on bookings and contracts
            VehicleStatus effectiveStatus = getEffectiveVehicleStatus(vehicle.getId());
            
            // If effective status differs from stored status, update it
            if (vehicle.getStatus() != effectiveStatus) {
                vehicle.setStatus(effectiveStatus);
                vehicleRepository.save(vehicle);
                updatedCount++;
            }
        }
        
        return updatedCount;
    }

    /**
     * Sync status for a single vehicle
     * Useful when booking is created/approved/cancelled
     */
    @Transactional
    public void syncVehicleStatus(Long vehicleId) {
        Optional<Vehicle> vehicleOpt = vehicleRepository.findById(vehicleId);
        if (vehicleOpt.isEmpty()) {
            return;
        }
        
        Vehicle vehicle = vehicleOpt.get();
        
        // Skip maintenance vehicles
        if (vehicle.getStatus() == VehicleStatus.Maintenance) {
            return;
        }
        
        // Get effective status
        VehicleStatus effectiveStatus = getEffectiveVehicleStatus(vehicleId);
        
        // Update if different
        if (vehicle.getStatus() != effectiveStatus) {
            vehicle.setStatus(effectiveStatus);
            vehicleRepository.save(vehicle);
        }
    }

    /**
     * Check if license plate exists (for duplicate validation)
     */
    public boolean checkLicensePlateExists(String licensePlate, Long vehicleId) {
        if (licensePlate == null || licensePlate.trim().isEmpty()) {
            return false;
        }
        
        String trimmedPlate = licensePlate.trim();
        
        if (vehicleId != null) {
            // Edit mode: check if license plate exists excluding current vehicle
            return vehicleRepository.existsByLicensePlateAndIdNot(trimmedPlate, vehicleId);
        } else {
            // Create mode: check if license plate exists
            return vehicleRepository.existsByLicensePlate(trimmedPlate);
        }
    }
    
    /**
     * Upload nhiều ảnh và trả về danh sách URL
     */
    private List<String> uploadImages(List<MultipartFile> files) throws IOException {
        List<String> imageUrls = new ArrayList<>();
        
        // Tạo thư mục nếu chưa có
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }
            
            // Tạo tên file unique
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String newFilename = UUID.randomUUID().toString() + extension;
            
            // Lưu file
            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Thêm tên file vào list (không cần đường dẫn đầy đủ)
            imageUrls.add(newFilename);
        }
        
        return imageUrls;
    }
    
    /**
     * Convert List<String> thành JSON array string
     */
    private String convertListToJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting list to JSON", e);
        }
    }
}
