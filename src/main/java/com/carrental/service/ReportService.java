package com.carrental.service;

import com.carrental.model.*;
import com.carrental.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating reports and analytics
 */
@Service
public class ReportService {
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private ContractRepository contractRepository;
    
    @Autowired
    private BookingRepository bookingRepository;
    
    @Autowired
    private VehicleRepository vehicleRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Generate revenue report for a date range
     */
    public RevenueReportDTO generateRevenueReport(LocalDateTime startDate, LocalDateTime endDate) {
        RevenueReportDTO report = new RevenueReportDTO();
        
        // Total revenue
        BigDecimal totalRevenue = paymentRepository.getTotalRevenue(startDate, endDate);
        report.setTotalRevenue(totalRevenue);
        
        // Revenue by payment type
        BigDecimal depositRevenue = paymentRepository.getRevenueByType(
            Payment.PaymentType.DEPOSIT, startDate, endDate);
        BigDecimal rentalRevenue = paymentRepository.getRevenueByType(
            Payment.PaymentType.RENTAL, startDate, endDate);
        BigDecimal refundAmount = paymentRepository.getRevenueByType(
            Payment.PaymentType.REFUND, startDate, endDate);
        
        report.setDepositRevenue(depositRevenue);
        report.setRentalRevenue(rentalRevenue);
        report.setRefundAmount(refundAmount);
        
        // Revenue by payment method
        BigDecimal cashRevenue = paymentRepository.getRevenueByMethod(
            Payment.PaymentMethod.CASH, startDate, endDate);
        BigDecimal cardRevenue = paymentRepository.getRevenueByMethod(
            Payment.PaymentMethod.CARD, startDate, endDate);
        BigDecimal transferRevenue = paymentRepository.getRevenueByMethod(
            Payment.PaymentMethod.TRANSFER, startDate, endDate);
        BigDecimal onlineRevenue = paymentRepository.getRevenueByMethod(
            Payment.PaymentMethod.ONLINE, startDate, endDate);
        
        report.setCashRevenue(cashRevenue);
        report.setCardRevenue(cardRevenue);
        report.setTransferRevenue(transferRevenue);
        report.setOnlineRevenue(onlineRevenue);
        
        // Payment counts
        Long completedPayments = paymentRepository.countPaymentsByStatus(
            Payment.PaymentStatus.COMPLETED, startDate, endDate);
        Long pendingPayments = paymentRepository.countPaymentsByStatus(
            Payment.PaymentStatus.PENDING, startDate, endDate);
        
        report.setCompletedPayments(completedPayments);
        report.setPendingPayments(pendingPayments);
        report.setTotalPayments(completedPayments + pendingPayments);
        
        // Daily revenue trend for charts
        Map<String, BigDecimal> dailyRevenue = calculateDailyRevenue(startDate, endDate);
        report.setDailyRevenue(dailyRevenue);
        
        return report;
    }
    
    /**
     * Calculate daily revenue for trend charts
     */
    private Map<String, BigDecimal> calculateDailyRevenue(LocalDateTime startDate, LocalDateTime endDate) {
        List<Payment> payments = paymentRepository.findCompletedPaymentsInRange(startDate, endDate);
        Map<String, BigDecimal> dailyRevenue = new LinkedHashMap<>();
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        // Group payments by date
        Map<String, List<Payment>> paymentsByDate = payments.stream()
            .collect(Collectors.groupingBy(p -> p.getPaymentDate().format(formatter)));
        
        // Calculate revenue for each date
        for (Map.Entry<String, List<Payment>> entry : paymentsByDate.entrySet()) {
            BigDecimal dayRevenue = entry.getValue().stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            dailyRevenue.put(entry.getKey(), dayRevenue);
        }
        
        return dailyRevenue;
    }
    
    /**
     * Generate vehicle usage report for a date range
     */
    public List<VehicleUsageDTO> generateVehicleUsageReport(LocalDateTime startDate, LocalDateTime endDate) {
        List<Contract> contracts = contractRepository.findActiveAndCompletedContractsWithVehicle(startDate, endDate);
        
        // Group contracts by vehicle
        Map<Long, List<Contract>> contractsByVehicle = contracts.stream()
            .collect(Collectors.groupingBy(c -> c.getVehicle().getId()));
        
        List<VehicleUsageDTO> usageList = new ArrayList<>();
        
        for (Map.Entry<Long, List<Contract>> entry : contractsByVehicle.entrySet()) {
            Long vehicleId = entry.getKey();
            List<Contract> vehicleContracts = entry.getValue();
            
            if (vehicleContracts.isEmpty()) continue;
            
            Vehicle vehicle = vehicleContracts.get(0).getVehicle();
            
            // Calculate statistics
            Long rentalCount = (long) vehicleContracts.size();
            Long totalDaysRented = vehicleContracts.stream()
                .mapToLong(Contract::getTotalDays)
                .sum();
            BigDecimal totalRevenue = vehicleContracts.stream()
                .map(Contract::getTotalRentalFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Calculate utilization rate
            long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
            BigDecimal utilizationRate = BigDecimal.ZERO;
            if (daysBetween > 0) {
                utilizationRate = BigDecimal.valueOf(totalDaysRented)
                    .divide(BigDecimal.valueOf(daysBetween), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            }
            
            VehicleUsageDTO dto = new VehicleUsageDTO(
                vehicleId,
                vehicle.getLicensePlate(),
                vehicle.getModel().getBrand().getBrandName(),
                vehicle.getModel().getModelName(),
                vehicle.getModel().getCategory(),
                rentalCount,
                totalDaysRented,
                totalRevenue
            );
            dto.setUtilizationRate(utilizationRate);
            dto.setCurrentStatus(vehicle.getStatus().name());
            
            usageList.add(dto);
        }
        
        // Sort by total revenue descending
        usageList.sort((a, b) -> b.getTotalRevenue().compareTo(a.getTotalRevenue()));
        
        return usageList;
    }
    
    /**
     * Generate customer statistics report
     */
    public List<CustomerStatsDTO> generateCustomerStatsReport(LocalDateTime startDate, LocalDateTime endDate) {
        List<User> customers = userRepository.findAllCustomers();
        List<CustomerStatsDTO> statsList = new ArrayList<>();
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        
        for (User customer : customers) {
            List<Booking> bookings = bookingRepository.findByCustomerIdWithRelations(customer.getId());
            
            // Filter bookings by date range
            List<Booking> rangeBookings = bookings.stream()
                .filter(b -> b.getCreatedAt().isAfter(startDate) && b.getCreatedAt().isBefore(endDate))
                .collect(Collectors.toList());
            
            if (rangeBookings.isEmpty() && customer.getCreatedAt().isBefore(startDate)) {
                continue; // Skip customers with no activity in range
            }
            
            Long totalBookings = (long) rangeBookings.size();
            Long completedBookings = rangeBookings.stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.APPROVED)
                .count();
            Long cancelledBookings = rangeBookings.stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.CANCELLED)
                .count();
            
            // Calculate total spent from contracts
            BigDecimal totalSpent = rangeBookings.stream()
                .map(b -> contractRepository.findByBookingId(b.getId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Contract::getTotalRentalFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            CustomerStatsDTO dto = new CustomerStatsDTO(
                customer.getId(),
                customer.getFullName(),
                customer.getEmail(),
                customer.getPhone(),
                totalBookings,
                completedBookings,
                totalSpent
            );
            dto.setCancelledBookings(cancelledBookings);
            dto.setRegistrationDate(customer.getCreatedAt().format(formatter));
            
            statsList.add(dto);
        }
        
        // Sort by total spent descending
        statsList.sort((a, b) -> b.getTotalSpent().compareTo(a.getTotalSpent()));
        
        return statsList;
    }
    
    /**
     * Generate dashboard overview statistics
     */
    public DashboardStatsDTO generateDashboardStats() {
        DashboardStatsDTO stats = new DashboardStatsDTO();
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfWeek = now.minusDays(7);
        LocalDateTime startOfMonth = now.minusDays(30);
        LocalDateTime startOfYear = now.minusYears(1);
        
        // Revenue metrics
        stats.setTotalRevenue(paymentRepository.getTotalRevenue(startOfYear, now));
        stats.setMonthlyRevenue(paymentRepository.getTotalRevenue(startOfMonth, now));
        stats.setWeeklyRevenue(paymentRepository.getTotalRevenue(startOfWeek, now));
        
        // Booking metrics
        stats.setTotalBookings(bookingRepository.count());
        stats.setPendingBookings(bookingRepository.findByStatusStringWithRelations("Pending").size() + 0L);
        
        // Contract metrics
        stats.setTotalContracts(contractRepository.count());
        stats.setActiveContracts(contractRepository.countByStatus(Contract.ContractStatus.ACTIVE));
        stats.setCompletedContracts(contractRepository.countByStatus(Contract.ContractStatus.COMPLETED));
        stats.setActiveRentals(stats.getActiveContracts());
        stats.setCompletedRentals(stats.getCompletedContracts());
        
        // Vehicle metrics
        stats.setTotalVehicles(vehicleRepository.count());
        stats.setAvailableVehicles(vehicleRepository.findByStatus(Vehicle.VehicleStatus.Available).size() + 0L);
        stats.setRentedVehicles(vehicleRepository.findByStatus(Vehicle.VehicleStatus.Rented).size() + 0L);
        stats.setMaintenanceVehicles(vehicleRepository.findByStatus(Vehicle.VehicleStatus.Maintenance).size() + 0L);
        
        // Customer metrics
        stats.setTotalCustomers(userRepository.countCustomers());
        stats.setNewCustomersThisMonth(userRepository.countNewCustomers(startOfMonth, now));
        stats.setActiveCustomers(userRepository.countActiveCustomers());
        
        // Payment metrics
        stats.setPendingPayments(paymentRepository.countPaymentsByStatus(
            Payment.PaymentStatus.PENDING, startOfYear, now));
        stats.setPendingPaymentAmount(paymentRepository.getTotalPendingAmount());
        
        // Chart data - Bookings by status
        Map<String, Long> bookingsByStatus = new LinkedHashMap<>();
        bookingsByStatus.put("Pending", bookingRepository.findByStatusStringWithRelations("Pending").size() + 0L);
        bookingsByStatus.put("Approved", bookingRepository.findByStatusStringWithRelations("Approved").size() + 0L);
        bookingsByStatus.put("Rejected", bookingRepository.findByStatusStringWithRelations("Rejected").size() + 0L);
        bookingsByStatus.put("Cancelled", bookingRepository.findByStatusStringWithRelations("Cancelled").size() + 0L);
        stats.setBookingsByStatus(bookingsByStatus);
        
        // Chart data - Vehicles by status
        Map<String, Long> vehiclesByStatus = new LinkedHashMap<>();
        vehiclesByStatus.put("Available", stats.getAvailableVehicles());
        vehiclesByStatus.put("Rented", stats.getRentedVehicles());
        vehiclesByStatus.put("Maintenance", stats.getMaintenanceVehicles());
        stats.setVehiclesByStatus(vehiclesByStatus);
        
        // Chart data - Revenue by month (last 6 months)
        Map<String, BigDecimal> revenueByMonth = new LinkedHashMap<>();
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMM yyyy");
        for (int i = 5; i >= 0; i--) {
            LocalDateTime monthStart = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);
            String monthLabel = monthStart.format(monthFormatter);
            BigDecimal monthRevenue = paymentRepository.getTotalRevenue(monthStart, monthEnd);
            revenueByMonth.put(monthLabel, monthRevenue);
        }
        stats.setRevenueByMonth(revenueByMonth);
        
        return stats;
    }
}
