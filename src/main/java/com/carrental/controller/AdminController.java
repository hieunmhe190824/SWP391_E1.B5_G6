package com.carrental.controller;

import com.carrental.service.BookingService;
import com.carrental.service.UserService;
import com.carrental.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private BookingService bookingService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalUsers", userService.getAllUsers().size());
        model.addAttribute("totalVehicles", vehicleService.getAllVehicles().size());
        model.addAttribute("totalBookings", bookingService.getAllBookings().size());
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String manageUsers(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        return "admin/users";
    }

    @GetMapping("/vehicles")
    public String manageVehicles(Model model) {
        model.addAttribute("vehicles", vehicleService.getAllVehicles());
        return "admin/vehicles";
    }
}
