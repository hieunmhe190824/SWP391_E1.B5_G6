package com.carrental.repository;

import com.carrental.model.Vehicle;
import com.carrental.model.Vehicle.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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
    
    /**
     * Tìm kiếm và lọc xe theo nhiều tiêu chí
     * UC04: Browse Vehicles - Search & Filter
     * Hỗ trợ tìm kiếm theo: brand, category, giá tối đa, số chỗ ngồi, transmission, fuel type
     */
    @Query("SELECT DISTINCT v FROM Vehicle v " +
           "LEFT JOIN FETCH v.model m " +
           "LEFT JOIN FETCH m.brand b " +
           "LEFT JOIN FETCH v.location l " +
           "WHERE v.status = :status " +
           "AND (:brandId IS NULL OR b.id = :brandId) " +
           "AND (:category IS NULL OR m.category = :category) " +
           "AND (:maxPrice IS NULL OR v.dailyRate <= :maxPrice) " +
           "AND (:minSeats IS NULL OR m.seats >= :minSeats) " +
           "AND (:transmission IS NULL OR m.transmission = :transmission) " +
           "AND (:fuelType IS NULL OR m.fuelType = :fuelType) " +
           "AND (:searchKeyword IS NULL OR LOWER(CONCAT(b.brandName, ' ', m.modelName, ' ', v.licensePlate)) LIKE LOWER(CONCAT('%', :searchKeyword, '%')))")
    List<Vehicle> searchVehicles(
        @Param("status") VehicleStatus status,
        @Param("brandId") Long brandId,
        @Param("category") String category,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("minSeats") Integer minSeats,
        @Param("transmission") String transmission,
        @Param("fuelType") String fuelType,
        @Param("searchKeyword") String searchKeyword
    );
    
    /**
     * Lấy danh sách các category có trong hệ thống
     */
    @Query("SELECT DISTINCT m.category FROM VehicleModel m ORDER BY m.category")
    List<String> findAllCategories();
    
    /**
     * Lấy danh sách các loại transmission có trong hệ thống
     */
    @Query("SELECT DISTINCT m.transmission FROM VehicleModel m ORDER BY m.transmission")
    List<String> findAllTransmissions();
    
    /**
     * Lấy danh sách các loại fuel type có trong hệ thống
     */
    @Query("SELECT DISTINCT m.fuelType FROM VehicleModel m ORDER BY m.fuelType")
    List<String> findAllFuelTypes();
}
