package com.carrental.controller;

import com.carrental.model.Contract;
import com.carrental.service.ContractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/contracts")
public class ContractController {

    @Autowired
    private ContractService contractService;

    @GetMapping("/{id}")
    public String viewContract(@PathVariable Long id, Model model) {
        Contract contract = contractService.getContractById(id)
                .orElseThrow(() -> new RuntimeException("Contract not found"));
        model.addAttribute("contract", contract);
        return "customer/contract-detail";
    }
}
