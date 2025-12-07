package com.carrental.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne
    @JoinColumn(name = "pickup_location_id", nullable = false)
    private Location pickupLocation;

    @ManyToOne
    @JoinColumn(name = "return_location_id", nullable = false)
    private Location returnLocation;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "total_days", nullable = false)
    private Integer totalDays;

    @Transient
    private BookingStatus status;

    @Column(name = "status", nullable = false)
    private String statusString;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum BookingStatus {
        PENDING("Pending"),
        APPROVED("Approved"),
        REJECTED("Rejected"),
        CANCELLED("Cancelled");

        private final String value;

        BookingStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        // Phương thức để parse từ database value (hỗ trợ cả uppercase và capitalized)
        public static BookingStatus fromValue(String value) {
            if (value == null) {
                return null;
            }
            // Chuẩn hóa về uppercase để so sánh
            String upperValue = value.toUpperCase();
            for (BookingStatus status : BookingStatus.values()) {
                if (status.name().equals(upperValue)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Unknown booking status: " + value);
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getCustomer() {
        return customer;
    }

    public void setCustomer(User customer) {
        this.customer = customer;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public Location getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(Location pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public Location getReturnLocation() {
        return returnLocation;
    }

    public void setReturnLocation(Location returnLocation) {
        this.returnLocation = returnLocation;
    }

    public Integer getTotalDays() {
        return totalDays;
    }

    public void setTotalDays(Integer totalDays) {
        this.totalDays = totalDays;
    }

    public BookingStatus getStatus() {
        // Khi get status, convert từ string sang enum
        if (status == null && statusString != null) {
            status = BookingStatus.fromValue(statusString);
        }
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
        // Khi set status, cũng update statusString để lưu vào DB
        if (status != null) {
            this.statusString = status.getValue();
        }
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
