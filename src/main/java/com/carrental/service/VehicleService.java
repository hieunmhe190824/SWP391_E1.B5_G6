package com.carrental.service;

import com.carrental.model.Vehicle;
import com.carrental.model.Vehicle.VehicleStatus;
import com.carrental.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class VehicleService {

    @Autowired
    private VehicleRepository vehicleRepository;

    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAll();
    }

    public Optional<Vehicle> getVehicleById(Long id) {
        return vehicleRepository.findById(id);
    }

    public List<Vehicle> getAvailableVehicles() {
        // Sử dụng method với JOIN FETCH để load tất cả relationships (model, brand, location)
        // Đảm bảo dữ liệu có sẵn khi render template, tránh LazyInitializationException
        return vehicleRepository.findByStatusWithRelations(VehicleStatus.Available);
    }
    
    /**
     * UC04: Browse Vehicles - Tìm kiếm và lọc xe
     * Tìm kiếm xe theo nhiều tiêu chí: brand, category, giá, số chỗ, transmission, fuel, keyword
     */
    public List<Vehicle> searchVehicles(
            Long brandId,
            String category,
            BigDecimal maxPrice,
            Integer minSeats,
            String transmission,
            String fuelType,
            String searchKeyword) {
        return vehicleRepository.searchVehicles(
            VehicleStatus.Available,
            brandId,
            category,
            maxPrice,
            minSeats,
            transmission,
            fuelType,
            searchKeyword
        );
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

    public Vehicle createVehicle(Vehicle vehicle) {
        vehicle.setStatus(VehicleStatus.Available);
        return vehicleRepository.save(vehicle);
    }

    public Vehicle updateVehicle(Long id, Vehicle vehicleDetails) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));

        vehicle.setModel(vehicleDetails.getModel());
        vehicle.setLocation(vehicleDetails.getLocation());
        vehicle.setLicensePlate(vehicleDetails.getLicensePlate());
        vehicle.setDailyRate(vehicleDetails.getDailyRate());
        vehicle.setStatus(vehicleDetails.getStatus());
        vehicle.setDepositAmount(vehicleDetails.getDepositAmount());
        vehicle.setImages(vehicleDetails.getImages());

        return vehicleRepository.save(vehicle);
    }

    public void deleteVehicle(Long id) {
        vehicleRepository.deleteById(id);
    }

    public Vehicle updateVehicleStatus(Long id, VehicleStatus status) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        vehicle.setStatus(status);
        return vehicleRepository.save(vehicle);
    }
}
