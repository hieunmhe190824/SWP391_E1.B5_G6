package com.carrental.controller;

import com.carrental.model.Booking;
import com.carrental.service.BookingService;
import com.carrental.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/bookings")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private VehicleService vehicleService;

    @GetMapping("/create")
    public String createBookingPage(@RequestParam Long vehicleId, Model model) {
        model.addAttribute("vehicle", vehicleService.getVehicleById(vehicleId)
                .orElseThrow(() -> new RuntimeException("Vehicle not found")));
        model.addAttribute("booking", new Booking());
        return "customer/booking-create";
    }

    @PostMapping("/create")
    public String createBooking(@ModelAttribute Booking booking) {
        bookingService.createBooking(booking);
        return "redirect:/bookings/my-bookings";
    }

    @GetMapping("/my-bookings")
    public String myBookings(Model model) {
        // TODO: Get current user ID from security context
        Long currentUserId = 1L;
        model.addAttribute("bookings", bookingService.getBookingsByCustomer(currentUserId));
        return "customer/my-bookings";
    }

    @PostMapping("/{id}/cancel")
    public String cancelBooking(@PathVariable Long id) {
        bookingService.cancelBooking(id);
        return "redirect:/bookings/my-bookings";
    }
}
