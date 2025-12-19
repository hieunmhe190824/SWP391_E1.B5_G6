package com.carrental.controller;

import com.carrental.model.Vehicle;
import com.carrental.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Controller xử lý các trang chính của website
 */
@Controller
public class HomeController {

    @Autowired
    private VehicleService vehicleService;

    /**
     * Hiển thị trang chủ với danh sách xe
     *
     * @param model Model để truyền dữ liệu ra view
     * @return Tên template trang chủ
     */
    @GetMapping({"/", "/home"})
    public String home(Model model) {
        try {
            // Lấy danh sách tất cả xe ở mọi trạng thái (tối đa 9 xe để hiển thị trên trang chủ)
            List<Vehicle> allVehicles = vehicleService.getAllVehicles();
            List<Vehicle> vehicles = (allVehicles != null && allVehicles.size() > 9)
                ? allVehicles.subList(0, 9)
                : (allVehicles != null ? allVehicles : List.of());

            model.addAttribute("vehicles", vehicles);
        } catch (Exception e) {
            // Nếu có lỗi, vẫn hiển thị trang home với danh sách rỗng
            model.addAttribute("vehicles", List.of());
        }

        return "customer/home";
    }

    /**
     * Hiển thị trang giới thiệu
     * 
     * @return Tên template trang giới thiệu
     */
    @GetMapping("/about")
    public String about() {
        return "customer/about";
    }
}
