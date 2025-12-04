package com.carrental.controller;

import com.carrental.model.Vehicle;
import com.carrental.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/vehicles")
public class VehicleController {

    @Autowired
    private VehicleService vehicleService;

    @GetMapping
    public String listVehicles(Model model) {
        model.addAttribute("vehicles", vehicleService.getAvailableVehicles());
        return "customer/vehicles";
    }

    @GetMapping("/{id}")
    public String viewVehicle(@PathVariable Long id, Model model) {
        Vehicle vehicle = vehicleService.getVehicleById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found"));
        model.addAttribute("vehicle", vehicle);
        return "customer/vehicle-detail";
    }
}
