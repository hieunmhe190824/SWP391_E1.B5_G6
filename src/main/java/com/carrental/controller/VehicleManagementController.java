package com.carrental.controller;

import com.carrental.model.Vehicle;
import com.carrental.model.VehicleModel;
import com.carrental.model.VehicleBrand;
import com.carrental.model.Location;
import com.carrental.model.Vehicle.VehicleStatus;
import com.carrental.repository.VehicleBrandRepository;
import com.carrental.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UC05: Vehicle Management Controller
 * Controller cho chức năng quản lý xe (Admin và Staff)
 * Xử lý CRUD operations, cập nhật trạng thái, và upload hình ảnh
 * 
 * Routes: Cả /admin/vehicles/** và /staff/vehicles/**
 * Templates: Shared tại admin folder (cả Admin và Staff đều dùng)
 */
@Controller
public class VehicleManagementController {

    private static final Logger logger = LoggerFactory.getLogger(VehicleManagementController.class);

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private VehicleBrandRepository vehicleBrandRepository;
    
    // ========================================
    // SHARED METHOD - Danh sách xe
    // ========================================
    private String listVehicles(
            Long brandId,
            String category,
            String status,
            String keyword,
            int page,
            Model model) {

        logger.info("=== LIST VEHICLES START ===");
        logger.info("Filters - brandId: {}, category: {}, status: {}, keyword: {}, page: {}",
                    brandId, category, status, keyword, page);

        // Validate page number
        if (page < 0) {
            page = 0;
        }

        // Chuẩn hóa các tham số empty string thành null
        category = (category != null && category.trim().isEmpty()) ? null : category;
        keyword = (keyword != null && keyword.trim().isEmpty()) ? null : keyword;

        // Parse status
        VehicleStatus vehicleStatus = null;
        if (status != null && !status.isEmpty()) {
            try {
                vehicleStatus = VehicleStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid status value: {}", status);
            }
        }

        // Tìm kiếm xe
        List<Vehicle> allVehicles;
        if (brandId != null || category != null || vehicleStatus != null ||
            (keyword != null && !keyword.trim().isEmpty())) {
            logger.info("Searching vehicles with filters...");
            allVehicles = vehicleService.searchAllVehicles(brandId, category, vehicleStatus, keyword);
        } else {
            logger.info("Getting all vehicles...");
            allVehicles = vehicleService.getAllVehicles();
        }

        logger.info("Found {} vehicles", allVehicles != null ? allVehicles.size() : 0);

        // Phân trang thủ công: 9 xe mỗi trang (3 dòng x 3 card)
        int pageSize = 9;
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

        // Log first few vehicles for debugging
        if (vehicles != null && !vehicles.isEmpty()) {
            logger.info("First vehicle: ID={}, License={}, Model={}",
                       vehicles.get(0).getId(),
                       vehicles.get(0).getLicensePlate(),
                       vehicles.get(0).getModel() != null ? vehicles.get(0).getModel().getModelName() : "NULL");
        }

        // Lấy dữ liệu cho filters
        List<VehicleBrand> brands = vehicleBrandRepository.findAll();
        List<String> categories = vehicleService.getAllCategories();

        logger.info("Brands: {}, Categories: {}", brands.size(), categories.size());

        // Truyền dữ liệu ra view
        model.addAttribute("vehicles", vehicles);
        model.addAttribute("brands", brands);
        model.addAttribute("categories", categories);
        model.addAttribute("statuses", VehicleStatus.values());

        // Giữ lại giá trị filter
        model.addAttribute("selectedBrandId", brandId);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("searchKeyword", keyword);

        // Pagination data
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalElements", totalElements);

        logger.info("=== LIST VEHICLES END ===");

        return "admin/vehicles";
    }

    // ========================================
    // ADMIN ROUTES
    // ========================================
    
    @GetMapping("/admin/vehicles")
    public String listVehiclesAdmin(
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        return listVehicles(brandId, category, status, keyword, page, model);
    }
    
    /**
     * API endpoint to check if license plate already exists
     */
    @GetMapping("/admin/vehicles/check-license-plate")
    @ResponseBody
    public Map<String, Boolean> checkLicensePlateAdmin(
            @RequestParam String licensePlate,
            @RequestParam(required = false) Long vehicleId) {
        
        boolean exists = vehicleService.checkLicensePlateExists(licensePlate, vehicleId);
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);
        return response;
    }

    @GetMapping("/admin/vehicles/create")
    public String showCreateFormAdmin(Model model) {
        model.addAttribute("vehicle", new Vehicle());
        model.addAttribute("vehicleModels", vehicleService.getAllVehicleModels());
        model.addAttribute("locations", vehicleService.getAllLocations());
        model.addAttribute("statuses", VehicleStatus.values());
        model.addAttribute("isEdit", false);
        return "admin/vehicle-form";
    }

    @PostMapping("/admin/vehicles/create")
    public String createVehicleAdmin(
            @ModelAttribute Vehicle vehicle,
            @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
            RedirectAttributes redirectAttributes,
            Model model) {
        try {
            Vehicle savedVehicle = vehicleService.createVehicle(vehicle, imageFiles);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Xe " + savedVehicle.getLicensePlate() + " đã được tạo thành công!");
            return "redirect:/admin/vehicles";
        } catch (IOException e) {
            logger.error("Error uploading images", e);
            model.addAttribute("errorMessage", "Lỗi khi upload ảnh: " + e.getMessage());
            model.addAttribute("vehicle", vehicle);
            model.addAttribute("vehicleModels", vehicleService.getAllVehicleModels());
            model.addAttribute("locations", vehicleService.getAllLocations());
            model.addAttribute("statuses", VehicleStatus.values());
            model.addAttribute("isEdit", false);
            return "admin/vehicle-form";
        } catch (IllegalArgumentException e) {
            logger.error("Validation error", e);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("vehicle", vehicle);
            model.addAttribute("vehicleModels", vehicleService.getAllVehicleModels());
            model.addAttribute("locations", vehicleService.getAllLocations());
            model.addAttribute("statuses", VehicleStatus.values());
            model.addAttribute("isEdit", false);
            return "admin/vehicle-form";
        } catch (Exception e) {
            logger.error("Unexpected error", e);
            model.addAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
            model.addAttribute("vehicle", vehicle);
            model.addAttribute("vehicleModels", vehicleService.getAllVehicleModels());
            model.addAttribute("locations", vehicleService.getAllLocations());
            model.addAttribute("statuses", VehicleStatus.values());
            model.addAttribute("isEdit", false);
            return "admin/vehicle-form";
        }
    }

    @GetMapping("/admin/vehicles/{id}")
    public String viewVehicleDetailAdmin(@PathVariable Long id, Model model) {
        Vehicle vehicle = vehicleService.getVehicleById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy xe với ID: " + id));
        model.addAttribute("vehicle", vehicle);
        return "admin/vehicle-detail";
    }

    @GetMapping("/admin/vehicles/{id}/edit")
    public String showEditFormAdmin(@PathVariable Long id, Model model) {
        Vehicle vehicle = vehicleService.getVehicleById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy xe với ID: " + id));
        
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("vehicleModels", vehicleService.getAllVehicleModels());
        model.addAttribute("locations", vehicleService.getAllLocations());
        model.addAttribute("statuses", VehicleStatus.values());
        model.addAttribute("isEdit", true);
        return "admin/vehicle-form";
    }

    @PostMapping("/admin/vehicles/{id}/edit")
    public String updateVehicleAdmin(
            @PathVariable Long id,
            @ModelAttribute Vehicle vehicle,
            @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
            RedirectAttributes redirectAttributes,
            Model model) {
        try {
            Vehicle updatedVehicle = vehicleService.updateVehicle(id, vehicle, imageFiles);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Xe " + updatedVehicle.getLicensePlate() + " đã được cập nhật thành công!");
            return "redirect:/admin/vehicles/" + id;
        } catch (IOException e) {
            logger.error("Error uploading images", e);
            model.addAttribute("errorMessage", "Lỗi khi upload ảnh: " + e.getMessage());
            model.addAttribute("vehicle", vehicle);
            model.addAttribute("vehicleModels", vehicleService.getAllVehicleModels());
            model.addAttribute("locations", vehicleService.getAllLocations());
            model.addAttribute("statuses", VehicleStatus.values());
            model.addAttribute("isEdit", true);
            return "admin/vehicle-form";
        } catch (IllegalArgumentException e) {
            logger.error("Validation error", e);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("vehicle", vehicle);
            model.addAttribute("vehicleModels", vehicleService.getAllVehicleModels());
            model.addAttribute("locations", vehicleService.getAllLocations());
            model.addAttribute("statuses", VehicleStatus.values());
            model.addAttribute("isEdit", true);
            return "admin/vehicle-form";
        } catch (Exception e) {
            logger.error("Unexpected error", e);
            model.addAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
            model.addAttribute("vehicle", vehicle);
            model.addAttribute("vehicleModels", vehicleService.getAllVehicleModels());
            model.addAttribute("locations", vehicleService.getAllLocations());
            model.addAttribute("statuses", VehicleStatus.values());
            model.addAttribute("isEdit", true);
            return "admin/vehicle-form";
        }
    }

    @PostMapping("/admin/vehicles/{id}/delete")
    public String deleteVehicleAdmin(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Vehicle vehicle = vehicleService.getVehicleById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy xe với ID: " + id));
            String licensePlate = vehicle.getLicensePlate();
            
            vehicleService.deleteVehicle(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Xe " + licensePlate + " đã được xóa thành công!");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Không thể xóa xe đang được thuê!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Lỗi khi xóa xe: " + e.getMessage());
        }
        return "redirect:/admin/vehicles";
    }

    @PostMapping("/admin/vehicles/{id}/status")
    public String updateVehicleStatusAdmin(
            @PathVariable Long id,
            @RequestParam String status,
            RedirectAttributes redirectAttributes) {
        try {
            VehicleStatus vehicleStatus = VehicleStatus.valueOf(status);
            Vehicle updatedVehicle = vehicleService.updateVehicleStatus(id, vehicleStatus);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Trạng thái xe " + updatedVehicle.getLicensePlate() + 
                " đã được cập nhật thành " + vehicleStatus);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Trạng thái không hợp lệ!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/vehicles/" + id;
    }
    
    // ========================================
    // STAFF ROUTES (same functionality, different paths)
    // ========================================
    
    @GetMapping("/staff/vehicles")
    public String listVehiclesStaff(
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        return listVehicles(brandId, category, status, keyword, page, model);
    }
    
    /**
     * API endpoint to check if license plate already exists (Staff)
     */
    @GetMapping("/staff/vehicles/check-license-plate")
    @ResponseBody
    public Map<String, Boolean> checkLicensePlateStaff(
            @RequestParam String licensePlate,
            @RequestParam(required = false) Long vehicleId) {
        
        boolean exists = vehicleService.checkLicensePlateExists(licensePlate, vehicleId);
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);
        return response;
    }

    @GetMapping("/staff/vehicles/create")
    public String showCreateFormStaff(Model model) {
        model.addAttribute("vehicle", new Vehicle());
        model.addAttribute("vehicleModels", vehicleService.getAllVehicleModels());
        model.addAttribute("locations", vehicleService.getAllLocations());
        model.addAttribute("statuses", VehicleStatus.values());
        model.addAttribute("isEdit", false);
        return "admin/vehicle-form";
    }

    @PostMapping("/staff/vehicles/create")
    public String createVehicleStaff(
            @ModelAttribute Vehicle vehicle,
            @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
            RedirectAttributes redirectAttributes,
            Model model) {
        try {
            Vehicle savedVehicle = vehicleService.createVehicle(vehicle, imageFiles);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Xe " + savedVehicle.getLicensePlate() + " đã được tạo thành công!");
            return "redirect:/staff/vehicles";
        } catch (IOException e) {
            logger.error("Error uploading images", e);
            model.addAttribute("errorMessage", "Lỗi khi upload ảnh: " + e.getMessage());
            model.addAttribute("vehicle", vehicle);
            model.addAttribute("vehicleModels", vehicleService.getAllVehicleModels());
            model.addAttribute("locations", vehicleService.getAllLocations());
            model.addAttribute("statuses", VehicleStatus.values());
            model.addAttribute("isEdit", false);
            return "admin/vehicle-form";
        } catch (IllegalArgumentException e) {
            logger.error("Validation error", e);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("vehicle", vehicle);
            model.addAttribute("vehicleModels", vehicleService.getAllVehicleModels());
            model.addAttribute("locations", vehicleService.getAllLocations());
            model.addAttribute("statuses", VehicleStatus.values());
            model.addAttribute("isEdit", false);
            return "admin/vehicle-form";
        } catch (Exception e) {
            logger.error("Unexpected error", e);
            model.addAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
            model.addAttribute("vehicle", vehicle);
            model.addAttribute("vehicleModels", vehicleService.getAllVehicleModels());
            model.addAttribute("locations", vehicleService.getAllLocations());
            model.addAttribute("statuses", VehicleStatus.values());
            model.addAttribute("isEdit", false);
            return "admin/vehicle-form";
        }
    }

    @GetMapping("/staff/vehicles/{id}")
    public String viewVehicleDetailStaff(@PathVariable Long id, Model model) {
        Vehicle vehicle = vehicleService.getVehicleById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy xe với ID: " + id));
        model.addAttribute("vehicle", vehicle);
        return "admin/vehicle-detail";
    }

    @GetMapping("/staff/vehicles/{id}/edit")
    public String showEditFormStaff(@PathVariable Long id, Model model) {
        Vehicle vehicle = vehicleService.getVehicleById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy xe với ID: " + id));
        
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("vehicleModels", vehicleService.getAllVehicleModels());
        model.addAttribute("locations", vehicleService.getAllLocations());
        model.addAttribute("statuses", VehicleStatus.values());
        model.addAttribute("isEdit", true);
        return "admin/vehicle-form";
    }

    @PostMapping("/staff/vehicles/{id}/edit")
    public String updateVehicleStaff(
            @PathVariable Long id,
            @ModelAttribute Vehicle vehicle,
            @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
            RedirectAttributes redirectAttributes,
            Model model) {
        try {
            Vehicle updatedVehicle = vehicleService.updateVehicle(id, vehicle, imageFiles);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Xe " + updatedVehicle.getLicensePlate() + " đã được cập nhật thành công!");
            return "redirect:/staff/vehicles/" + id;
        } catch (IOException e) {
            logger.error("Error uploading images", e);
            model.addAttribute("errorMessage", "Lỗi khi upload ảnh: " + e.getMessage());
            model.addAttribute("vehicle", vehicle);
            model.addAttribute("vehicleModels", vehicleService.getAllVehicleModels());
            model.addAttribute("locations", vehicleService.getAllLocations());
            model.addAttribute("statuses", VehicleStatus.values());
            model.addAttribute("isEdit", true);
            return "admin/vehicle-form";
        } catch (IllegalArgumentException e) {
            logger.error("Validation error", e);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("vehicle", vehicle);
            model.addAttribute("vehicleModels", vehicleService.getAllVehicleModels());
            model.addAttribute("locations", vehicleService.getAllLocations());
            model.addAttribute("statuses", VehicleStatus.values());
            model.addAttribute("isEdit", true);
            return "admin/vehicle-form";
        } catch (Exception e) {
            logger.error("Unexpected error", e);
            model.addAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
            model.addAttribute("vehicle", vehicle);
            model.addAttribute("vehicleModels", vehicleService.getAllVehicleModels());
            model.addAttribute("locations", vehicleService.getAllLocations());
            model.addAttribute("statuses", VehicleStatus.values());
            model.addAttribute("isEdit", true);
            return "admin/vehicle-form";
        }
    }

    @PostMapping("/staff/vehicles/{id}/delete")
    public String deleteVehicleStaff(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Vehicle vehicle = vehicleService.getVehicleById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy xe với ID: " + id));
            String licensePlate = vehicle.getLicensePlate();
            
            vehicleService.deleteVehicle(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Xe " + licensePlate + " đã được xóa thành công!");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Không thể xóa xe đang được thuê!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Lỗi khi xóa xe: " + e.getMessage());
        }
        return "redirect:/staff/vehicles";
    }

    @PostMapping("/staff/vehicles/{id}/status")
    public String updateVehicleStatusStaff(
            @PathVariable Long id,
            @RequestParam String status,
            RedirectAttributes redirectAttributes) {
        try {
            VehicleStatus vehicleStatus = VehicleStatus.valueOf(status);
            Vehicle updatedVehicle = vehicleService.updateVehicleStatus(id, vehicleStatus);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Trạng thái xe " + updatedVehicle.getLicensePlate() + 
                " đã được cập nhật thành " + vehicleStatus);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Trạng thái không hợp lệ!");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/staff/vehicles/" + id;
    }
}
