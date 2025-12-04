package com.carrental.controller;

import com.carrental.service.BookingService;
import com.carrental.service.SupportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/staff")
public class StaffController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private SupportService supportService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("bookings", bookingService.getAllBookings());
        return "staff/dashboard";
    }

    @GetMapping("/bookings")
    public String manageBookings(Model model) {
        model.addAttribute("bookings", bookingService.getAllBookings());
        return "staff/bookings";
    }

    @GetMapping("/support")
    public String manageSupport(Model model) {
        model.addAttribute("tickets", supportService.getAllTickets());
        return "staff/support";
    }
}
