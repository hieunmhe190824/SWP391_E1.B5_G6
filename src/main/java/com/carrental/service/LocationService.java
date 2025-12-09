package com.carrental.service;

import com.carrental.model.Location;
import com.carrental.repository.LocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing locations
 * Used for booking creation and vehicle management
 */
@Service
public class LocationService {
    
    @Autowired
    private LocationRepository locationRepository;
    
    /**
     * Get all locations
     */
    public List<Location> getAllLocations() {
        return locationRepository.findAll();
    }
    
    /**
     * Get location by ID
     */
    public Optional<Location> getLocationById(Long id) {
        return locationRepository.findById(id);
    }
    
    /**
     * Get locations by city
     */
    public List<Location> getLocationsByCity(String city) {
        return locationRepository.findByCity(city);
    }
    
    /**
     * Search locations by name
     */
    public List<Location> searchLocationsByName(String name) {
        return locationRepository.findByLocationNameContainingIgnoreCase(name);
    }
    
    /**
     * Create new location
     */
    public Location createLocation(Location location) {
        return locationRepository.save(location);
    }
    
    /**
     * Update location
     */
    public Location updateLocation(Long id, Location locationDetails) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Location not found with id: " + id));
        
        location.setLocationName(locationDetails.getLocationName());
        location.setAddress(locationDetails.getAddress());
        location.setCity(locationDetails.getCity());
        location.setPhoneNumber(locationDetails.getPhoneNumber());
        
        return locationRepository.save(location);
    }
    
    /**
     * Delete location
     */
    public void deleteLocation(Long id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Location not found with id: " + id));
        locationRepository.delete(location);
    }
    
    /**
     * Check if location exists
     */
    public boolean locationExists(Long id) {
        return locationRepository.existsById(id);
    }
}

