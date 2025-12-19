package com.carrental.controller;

import com.carrental.model.Vehicle;
import com.carrental.model.VehicleBrand;
import com.carrental.repository.VehicleBrandRepository;
import com.carrental.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * UC04: Browse Vehicles Controller
 * Xử lý việc xem, tìm kiếm, lọc danh sách xe và xem chi tiết xe
 */
@Controller
@RequestMapping("/vehicles")
public class VehicleController {

    @Autowired
    private VehicleService vehicleService;
    
    @Autowired
    private VehicleBrandRepository vehicleBrandRepository;

    /**
     * UC04: Browse Vehicles - View list + Search/Filter
     * Hiển thị danh sách xe với tính năng tìm kiếm và lọc
     * Phân trang: 9 xe mỗi trang (3 dòng x 3 card)
     */
    @GetMapping
    public String listVehicles(
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer minSeats,
            @RequestParam(required = false) String transmission,
            @RequestParam(required = false) String fuelType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        
        // Validate page number (không cho page âm)
        if (page < 0) {
            page = 0;
        }
        
        // Chuẩn hóa các tham số empty string thành null
        category = (category != null && category.trim().isEmpty()) ? null : category;
        transmission = (transmission != null && transmission.trim().isEmpty()) ? null : transmission;
        fuelType = (fuelType != null && fuelType.trim().isEmpty()) ? null : fuelType;
        keyword = (keyword != null && keyword.trim().isEmpty()) ? null : keyword;
        
        // Tạo Pageable: 9 xe mỗi trang (3 dòng x 3 card)
        int pageSize = 9;
        
        // Tìm kiếm xe theo các tiêu chí - hiển thị tất cả xe ở mọi trạng thái
        List<Vehicle> allVehicles;
        if (brandId != null || category != null || maxPrice != null || 
            minSeats != null || transmission != null || fuelType != null || 
            keyword != null) {
            // Có filter/search -> sử dụng search method (không filter theo status)
            allVehicles = vehicleService.searchAllVehiclesForCustomer(
                brandId, category, maxPrice, minSeats, 
                transmission, fuelType, keyword
            );
        } else {
            // Không có filter -> hiển thị tất cả xe ở mọi trạng thái
            allVehicles = vehicleService.getAllVehicles();
        }
        
        // Thực hiện phân trang thủ công (vì searchVehicles trả về List, không phải Page)
        int totalElements = allVehicles.size();
        int totalPages = totalElements > 0 ? (int) Math.ceil((double) totalElements / pageSize) : 0;
        
        // Validate page number (không cho vượt quá totalPages)
        if (page >= totalPages && totalPages > 0) {
            page = totalPages - 1;
        }
        
        int start = page * pageSize;
        int end = Math.min(start + pageSize, totalElements);
        
        List<Vehicle> vehicles = (start < totalElements) 
            ? allVehicles.subList(start, end) 
            : List.of();
        
        // Lấy dữ liệu cho các filter dropdown
        List<VehicleBrand> brands = vehicleBrandRepository.findAll();
        List<String> categories = vehicleService.getAllCategories();
        List<String> transmissions = vehicleService.getAllTransmissions();
        List<String> fuelTypes = vehicleService.getAllFuelTypes();
        
        // Truyền dữ liệu ra view
        model.addAttribute("vehicles", vehicles);
        model.addAttribute("brands", brands);
        model.addAttribute("categories", categories);
        model.addAttribute("transmissions", transmissions);
        model.addAttribute("fuelTypes", fuelTypes);
        
        // Giữ lại giá trị filter đã chọn
        model.addAttribute("selectedBrandId", brandId);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedMaxPrice", maxPrice);
        model.addAttribute("selectedMinSeats", minSeats);
        model.addAttribute("selectedTransmission", transmission);
        model.addAttribute("selectedFuelType", fuelType);
        model.addAttribute("searchKeyword", keyword);
        
        // Pagination data
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalElements", totalElements);
        
        return "customer/vehicles";
    }

    /**
     * UC04: Browse Vehicles - View details
     * Xem chi tiết thông tin xe
     */
    @GetMapping("/{id}")
    public String viewVehicle(@PathVariable Long id, Model model) {
        Vehicle vehicle = vehicleService.getVehicleById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        
        // Get effective status based on bookings and contracts
        Vehicle.VehicleStatus effectiveStatus = vehicleService.getEffectiveVehicleStatus(id);
        
        // Update vehicle status for display (temporary, doesn't save to DB)
        Vehicle displayVehicle = vehicle;
        displayVehicle.setStatus(effectiveStatus);
        
        // Get next available date and availability periods
        LocalDateTime nextAvailableDate = vehicleService.getNextAvailableDate(id);
        List<java.util.Map<String, Object>> availabilityPeriods = vehicleService.getAvailabilityPeriods(id, 60);
        
        // Ensure boolean value is always set (never null)
        boolean isAvailable = (effectiveStatus == Vehicle.VehicleStatus.Available);
        
        model.addAttribute("vehicle", displayVehicle);
        model.addAttribute("nextAvailableDate", nextAvailableDate);
        model.addAttribute("availabilityPeriods", availabilityPeriods != null ? availabilityPeriods : java.util.Collections.emptyList());
        model.addAttribute("isAvailable", isAvailable);
        
        return "customer/vehicle-detail";
    }
}
