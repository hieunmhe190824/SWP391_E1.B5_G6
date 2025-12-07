package com.carrental.repository;

import com.carrental.model.Vehicle;
import com.carrental.model.Vehicle.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findByStatus(VehicleStatus status);
    List<Vehicle> findByLocationId(Long locationId);
    
    /**
     * Lấy danh sách xe có sẵn với tất cả relationships được load (model, brand, location)
     * Sử dụng JOIN FETCH để tránh N+1 query problem và đảm bảo dữ liệu có sẵn khi render template
     */
    @Query("SELECT DISTINCT v FROM Vehicle v " +
           "LEFT JOIN FETCH v.model m " +
           "LEFT JOIN FETCH m.brand b " +
           "LEFT JOIN FETCH v.location l " +
           "WHERE v.status = :status")
    List<Vehicle> findByStatusWithRelations(VehicleStatus status);
}
