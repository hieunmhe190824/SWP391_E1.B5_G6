package com.carrental.controller;

import com.carrental.model.Handover;
import com.carrental.service.HandoverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/handovers")
public class HandoverController {

    @Autowired
    private HandoverService handoverService;

    @GetMapping("/create")
    public String createHandoverPage(@RequestParam Long contractId, Model model) {
        model.addAttribute("contractId", contractId);
        model.addAttribute("handover", new Handover());
        return "staff/handover";
    }

    @PostMapping("/create")
    public String createHandover(@ModelAttribute Handover handover) {
        handoverService.createHandover(handover);
        return "redirect:/staff/dashboard";
    }
}
