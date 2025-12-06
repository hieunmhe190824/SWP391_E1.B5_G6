package com.carrental.service;

import com.carrental.model.Vehicle;
import com.carrental.model.Vehicle.VehicleStatus;
import com.carrental.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
        return vehicleRepository.findByStatus(VehicleStatus.Available);
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
