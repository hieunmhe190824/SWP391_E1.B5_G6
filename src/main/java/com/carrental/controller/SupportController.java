package com.carrental.controller;

import com.carrental.model.SupportTicket;
import com.carrental.service.SupportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/support")
public class SupportController {

    @Autowired
    private SupportService supportService;

    @GetMapping("/tickets")
    public String listTickets(Model model) {
        // TODO: Get current user ID from security context
        Long currentUserId = 1L;
        model.addAttribute("tickets", supportService.getTicketsByCustomer(currentUserId));
        return "customer/support-tickets";
    }

    @GetMapping("/tickets/create")
    public String createTicketPage(Model model) {
        model.addAttribute("ticket", new SupportTicket());
        return "customer/support-create";
    }

    @PostMapping("/tickets/create")
    public String createTicket(@ModelAttribute SupportTicket ticket) {
        supportService.createTicket(ticket);
        return "redirect:/support/tickets";
    }

    @GetMapping("/tickets/{id}")
    public String viewTicket(@PathVariable Long id, Model model) {
        SupportTicket ticket = supportService.getTicketById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        model.addAttribute("ticket", ticket);
        return "customer/support-detail";
    }
}
