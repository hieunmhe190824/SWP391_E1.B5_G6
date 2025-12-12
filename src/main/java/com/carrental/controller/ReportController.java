package com.carrental.controller;

import com.carrental.model.*;
import com.carrental.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for Reports & Analytics
 * UC22: View Reports - For Admin and Staff roles
 */
@Controller
public class ReportController {
    
    @Autowired
    private ReportService reportService;
    
    /**
     * Admin Reports Page
     * GET /admin/reports
     */
    @GetMapping("/admin/reports")
    @PreAuthorize("hasAuthority('ADMIN')")
    public String adminReports(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Model model) {
        
        // Calculate date range
        DateRange dateRange = calculateDateRange(period, startDate, endDate);
        
        // Generate reports
        RevenueReportDTO revenueReport = reportService.generateRevenueReport(
            dateRange.start, dateRange.end);
        List<VehicleUsageDTO> vehicleUsage = reportService.generateVehicleUsageReport(
            dateRange.start, dateRange.end);
        List<CustomerStatsDTO> customerStats = reportService.generateCustomerStatsReport(
            dateRange.start, dateRange.end);
        DashboardStatsDTO dashboardStats = reportService.generateDashboardStats(dateRange.start, dateRange.end);
        
        // Add to model
        model.addAttribute("revenueReport", revenueReport);
        model.addAttribute("vehicleUsage", vehicleUsage);
        model.addAttribute("customerStats", customerStats);
        model.addAttribute("dashboardStats", dashboardStats);
        model.addAttribute("selectedPeriod", period != null ? period : "month");
        model.addAttribute("startDate", dateRange.start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        model.addAttribute("endDate", dateRange.end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        
        return "admin/reports";
    }
    
    /**
     * Staff Reports Page
     * GET /staff/reports
     */
    @GetMapping("/staff/reports")
    @PreAuthorize("hasAuthority('STAFF')")
    public String staffReports(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Model model) {
        
        // Calculate date range
        DateRange dateRange = calculateDateRange(period, startDate, endDate);
        
        // Generate reports (same as admin)
        RevenueReportDTO revenueReport = reportService.generateRevenueReport(
            dateRange.start, dateRange.end);
        List<VehicleUsageDTO> vehicleUsage = reportService.generateVehicleUsageReport(
            dateRange.start, dateRange.end);
        List<CustomerStatsDTO> customerStats = reportService.generateCustomerStatsReport(
            dateRange.start, dateRange.end);
        DashboardStatsDTO dashboardStats = reportService.generateDashboardStats(dateRange.start, dateRange.end);
        
        // Add to model
        model.addAttribute("revenueReport", revenueReport);
        model.addAttribute("vehicleUsage", vehicleUsage);
        model.addAttribute("customerStats", customerStats);
        model.addAttribute("dashboardStats", dashboardStats);
        model.addAttribute("selectedPeriod", period != null ? period : "month");
        model.addAttribute("startDate", dateRange.start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        model.addAttribute("endDate", dateRange.end.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        
        return "staff/reports";
    }
    
    /**
     * Calculate date range based on period or custom dates
     */
    private DateRange calculateDateRange(String period, String startDateStr, String endDateStr) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start;
        LocalDateTime end = now;
        
        if (startDateStr != null && endDateStr != null && !startDateStr.isEmpty() && !endDateStr.isEmpty()) {
            // Custom date range
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            start = LocalDateTime.parse(startDateStr + "T00:00:00");
            end = LocalDateTime.parse(endDateStr + "T23:59:59");
        } else if (period != null) {
            // Predefined period
            switch (period) {
                case "today":
                    start = now.withHour(0).withMinute(0).withSecond(0);
                    end = now.withHour(23).withMinute(59).withSecond(59);
                    break;
                case "week":
                    start = now.minusDays(7);
                    break;
                case "month":
                    start = now.minusDays(30);
                    break;
                case "year":
                    start = now.minusYears(1);
                    break;
                default:
                    start = now.minusDays(30); // Default to month
            }
        } else {
            // Default to last 30 days
            start = now.minusDays(30);
        }
        
        return new DateRange(start, end);
    }
    
    /**
     * Helper class for date range
     */
    private static class DateRange {
        LocalDateTime start;
        LocalDateTime end;
        
        DateRange(LocalDateTime start, LocalDateTime end) {
            this.start = start;
            this.end = end;
        }
    }
}
