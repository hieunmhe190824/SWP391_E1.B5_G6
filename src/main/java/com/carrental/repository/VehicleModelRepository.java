package com.carrental.repository;

import com.carrental.model.VehicleModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleModelRepository extends JpaRepository<VehicleModel, Long> {
    /**
     * Tìm model theo brand ID
     */
    List<VehicleModel> findByBrandId(Long brandId);
    
    /**
     * Tìm model theo category
     */
    List<VehicleModel> findByCategory(String category);
    
    /**
     * Tìm model với brand được load sẵn (JOIN FETCH)
     */
    @Query("SELECT DISTINCT m FROM VehicleModel m " +
           "LEFT JOIN FETCH m.brand b")
    List<VehicleModel> findAllWithBrand();
}
