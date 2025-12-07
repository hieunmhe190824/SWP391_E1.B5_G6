package com.carrental.repository;

import com.carrental.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {
    /**
     * Tìm location theo city
     */
    List<Location> findByCity(String city);
    
    /**
     * Tìm location theo tên (case insensitive)
     */
    List<Location> findByLocationNameContainingIgnoreCase(String locationName);
}
