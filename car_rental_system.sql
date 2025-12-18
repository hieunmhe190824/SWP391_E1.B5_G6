-- ===================================================================
-- HỆ THỐNG THUÊ XE - DATABASE SCHEMA (MySQL 8.0+)
-- ===================================================================
--
-- BOOKING AND CONTRACT WORKFLOW:
-- 1. Customer creates booking and uploads documents (GPLX, CMND)
-- 2. Staff reviews documents and approves/rejects booking
-- 3. When approved, system automatically creates contract with PENDING_PAYMENT status
-- 4. Contract is sent to customer for review
-- 5. Customer confirms and pays fixed deposit of 50,000,000 VND
-- 6. After payment, contract status changes to ACTIVE
--
-- FIXED DEPOSIT AMOUNT: 50,000,000 VND for all cars
-- ===================================================================

DROP DATABASE IF EXISTS car_rental_system;
CREATE DATABASE car_rental_system
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE car_rental_system;

-- ===================================================================
-- 1. USERS & AUTH (2 bảng)
-- ===================================================================

-- Bảng: users - Người dùng
CREATE TABLE users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(100) NOT NULL UNIQUE,                  -- Email đăng nhập
    password_hash VARCHAR(255) NOT NULL,                 -- Mật khẩu mã hóa
    full_name VARCHAR(100) NOT NULL,                     -- Họ tên
    phone VARCHAR(20) NOT NULL,                          -- SĐT
    address TEXT,                                        -- Địa chỉ
    role ENUM('Admin', 'Staff', 'Customer') NOT NULL,    -- Vai trò (mỗi user 1 role)
    status ENUM('Active', 'Inactive') DEFAULT 'Active',  -- Trạng thái
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_role (role)
) ENGINE=InnoDB;

-- Bảng: user_documents - Giấy tờ (CMND, GPLX)
CREATE TABLE user_documents (
    document_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    document_type ENUM('ID_Card', 'Driver_License') NOT NULL,
    document_number VARCHAR(50) NOT NULL,
    image_url VARCHAR(255),                              -- Ảnh giấy tờ
    expiry_date DATE,
    status ENUM('Pending', 'Approved', 'Rejected') DEFAULT 'Pending',
    verified_by INT,                                     -- Staff duyệt
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (verified_by) REFERENCES users(user_id) ON DELETE SET NULL,
    INDEX idx_user (user_id)
) ENGINE=InnoDB;

-- ===================================================================
-- 2. VEHICLES (4 bảng)
-- ===================================================================

-- Bảng: vehicle_brands - Hãng xe
CREATE TABLE vehicle_brands (
    brand_id INT PRIMARY KEY AUTO_INCREMENT,
    brand_name VARCHAR(50) NOT NULL UNIQUE,              -- Toyota, Honda...
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Bảng: vehicle_models - Model xe
CREATE TABLE vehicle_models (
    model_id INT PRIMARY KEY AUTO_INCREMENT,
    brand_id INT NOT NULL,
    model_name VARCHAR(100) NOT NULL,                    -- Camry, Civic...
    category ENUM('Economy', 'Sedan', 'SUV', 'Luxury') NOT NULL, -- Loại xe
    year INT NOT NULL,
    seats INT NOT NULL,
    transmission ENUM('Manual', 'Automatic') NOT NULL,
    fuel_type ENUM('Gasoline', 'Diesel', 'Electric') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (brand_id) REFERENCES vehicle_brands(brand_id) ON DELETE CASCADE,
    INDEX idx_brand (brand_id),
    INDEX idx_category (category)
) ENGINE=InnoDB;

-- Bảng: locations - Địa điểm
CREATE TABLE locations (
    location_id INT PRIMARY KEY AUTO_INCREMENT,
    location_name VARCHAR(100) NOT NULL,
    address TEXT NOT NULL,
    city VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_city (city)
) ENGINE=InnoDB;

-- Bảng: vehicles - Xe
CREATE TABLE vehicles (
    vehicle_id INT PRIMARY KEY AUTO_INCREMENT,
    model_id INT NOT NULL,
    location_id INT NOT NULL,                            -- Vị trí hiện tại
    license_plate VARCHAR(20) NOT NULL UNIQUE,           -- Biển số
    color VARCHAR(30),
    status ENUM('Available', 'Rented', 'Maintenance') DEFAULT 'Available',
    daily_rate DECIMAL(18, 2) NOT NULL,                  -- Giá/ngày
    deposit_amount DECIMAL(18, 2) NOT NULL,              -- Tiền cọc (Fixed: 50,000,000 VND for all vehicles)
    images JSON,                                         -- Array URL ảnh: ["url1", "url2"]
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (model_id) REFERENCES vehicle_models(model_id) ON DELETE CASCADE,
    FOREIGN KEY (location_id) REFERENCES locations(location_id) ON DELETE CASCADE,
    INDEX idx_status (status),
    INDEX idx_location (location_id),
    CONSTRAINT chk_deposit CHECK (deposit_amount > daily_rate)
) ENGINE=InnoDB;

-- ===================================================================
-- 3. BOOKING & CONTRACT (4 bảng)
-- ===================================================================

-- Bảng: bookings - Đặt xe
-- WORKFLOW STEP 1: Customer creates booking and uploads documents (GPLX, CMND)
CREATE TABLE bookings (
    booking_id INT PRIMARY KEY AUTO_INCREMENT,
    customer_id INT NOT NULL,
    vehicle_id INT NOT NULL,
    pickup_location_id INT NOT NULL,
    return_location_id INT NOT NULL,
    start_date DATETIME NOT NULL,
    end_date DATETIME NOT NULL,
    total_days INT NOT NULL,
    status ENUM('Pending', 'Approved', 'Rejected', 'Cancelled', 'Completed') DEFAULT 'Pending',
    rejection_reason VARCHAR(500) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id) ON DELETE CASCADE,
    FOREIGN KEY (pickup_location_id) REFERENCES locations(location_id) ON DELETE CASCADE,
    FOREIGN KEY (return_location_id) REFERENCES locations(location_id) ON DELETE CASCADE,
    INDEX idx_customer (customer_id),
    INDEX idx_vehicle (vehicle_id),
    INDEX idx_status (status)
) ENGINE=InnoDB;

-- Bảng: booking_documents - Giấy tờ đính kèm booking (Many-to-Many)
CREATE TABLE booking_documents (
    booking_id INT NOT NULL,
    document_id INT NOT NULL,
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (booking_id, document_id),
    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE,
    FOREIGN KEY (document_id) REFERENCES user_documents(document_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Bảng: contracts - Hợp đồng thuê xe
-- WORKFLOW STEP 3: Auto-created when staff approves booking
-- WORKFLOW STEP 4: Contract sent to customer with PENDING_PAYMENT status
-- WORKFLOW STEP 5: After vehicle return, status moves to BILL_PENDING until bill is paid
-- WORKFLOW STEP 5: Customer pays deposit, status changes to ACTIVE
-- FIXED DEPOSIT: 50,000,000 VND for all cars
-- NOTE: booking_id is nullable to allow contracts to exist independently (e.g., when booking is cancelled)
CREATE TABLE contracts (
    contract_id INT PRIMARY KEY AUTO_INCREMENT,
    booking_id INT UNIQUE,                               -- Nullable: 1 booking có thể không có hoặc có 1 contract
    contract_number VARCHAR(50) NOT NULL UNIQUE,
    customer_id INT NOT NULL,
    vehicle_id INT NOT NULL,
    staff_id INT NOT NULL,                               -- Staff tạo
    start_date DATETIME NOT NULL,
    end_date DATETIME NOT NULL,
    total_days INT NOT NULL,
    daily_rate DECIMAL(18, 2) NOT NULL,
    total_rental_fee DECIMAL(18, 2) NOT NULL,
    deposit_amount DECIMAL(18, 2) NOT NULL,              -- Fixed: 50,000,000 VND
    status ENUM('Pending_Payment', 'Active', 'Bill_Pending', 'Completed', 'Cancelled') DEFAULT 'Pending_Payment',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE SET NULL,
    FOREIGN KEY (customer_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id) ON DELETE CASCADE,
    FOREIGN KEY (staff_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_customer (customer_id),
    INDEX idx_booking (booking_id),
    INDEX idx_status (status),
    CONSTRAINT chk_deposit_fee CHECK (deposit_amount > total_rental_fee)
) ENGINE=InnoDB;

-- WORKFLOW STEP 5: Customer pays deposit of 50,000,000 VND
-- Payment type 'DEPOSIT' is created when customer confirms contract
-- Supports online payment gateway integration (default ONLINE)
-- Also includes bill details for RENTAL payment type
CREATE TABLE payments (
    payment_id INT PRIMARY KEY AUTO_INCREMENT,
    contract_id INT NOT NULL,
    payment_type ENUM('DEPOSIT', 'RENTAL', 'REFUND') NOT NULL,  -- Deposit = 50M VND
    amount DECIMAL(18, 2) NOT NULL,
    method ENUM('CASH', 'CARD', 'TRANSFER', 'ONLINE') NOT NULL DEFAULT 'ONLINE',  -- Default: ONLINE
    status ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED') DEFAULT 'PENDING',  -- Added PROCESSING and CANCELLED
    payment_date DATETIME,

    -- Online payment gateway fields
    transaction_ref VARCHAR(50) UNIQUE,              -- Unique transaction reference (e.g., DEPOSIT123_12345678)
    gateway_transaction_id VARCHAR(50),              -- Payment gateway's transaction ID
    gateway_response_code VARCHAR(10),               -- Response code from gateway (00 = success)
    gateway_transaction_status VARCHAR(10),          -- Transaction status from gateway
    gateway_bank_code VARCHAR(20),                   -- Bank code used for payment
    gateway_card_type VARCHAR(20),                   -- Card type (ATM, Credit, etc.)
    gateway_pay_date VARCHAR(14),                    -- Payment date from gateway (yyyyMMddHHmmss)
    gateway_secure_hash VARCHAR(255),                -- Security hash from gateway
    payment_url TEXT,                                -- Payment gateway URL (for pending payments)

    -- Bill details (for RENTAL payment type, nullable for other types)
    bill_number VARCHAR(50) UNIQUE,                  -- Bill number (e.g., BILL-20241213-1234)
    original_rental_fee DECIMAL(18, 2),              -- Original rental fee from contract
    rental_adjustment DECIMAL(18, 2),                -- Adjustment (negative if early return, positive if late)
    actual_rental_fee DECIMAL(18, 2),                -- Actual rental fee after adjustment
    late_fee DECIMAL(18, 2) DEFAULT 0,              -- Late return fee
    damage_fee DECIMAL(18, 2) DEFAULT 0,             -- Damage fee
    one_way_fee DECIMAL(18, 2) DEFAULT 0,            -- One-way return fee
    total_additional_fees DECIMAL(18, 2) DEFAULT 0,  -- Sum of late_fee + damage_fee + one_way_fee
    deposit_amount DECIMAL(18, 2),                   -- 50,000,000 VND - still held (for RENTAL payment)
    amount_paid DECIMAL(18, 2) DEFAULT 0,            -- Amount paid so far
    amount_due DECIMAL(18, 2),                       -- Amount due (total_amount - amount_paid)
    notes TEXT,                                       -- Additional notes

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (contract_id) REFERENCES contracts(contract_id) ON DELETE CASCADE,
    INDEX idx_contract (contract_id),
    INDEX idx_type (payment_type),
    INDEX idx_status (status),
    INDEX idx_transaction_ref (transaction_ref),
    INDEX idx_bill_number (bill_number)
) ENGINE=InnoDB;

-- ===================================================================
-- 4. RENTAL PROCESS (2 bảng)
-- ===================================================================

CREATE TABLE handovers (
    handover_id INT PRIMARY KEY AUTO_INCREMENT,
    contract_id INT NOT NULL,
    handover_type ENUM('PICKUP', 'RETURN') NOT NULL,     -- Nhận hoặc trả
    staff_id INT NOT NULL,
    handover_time DATETIME NOT NULL,                     -- Thời gian thực tế
    odometer INT,                                        -- Số km
    fuel_level INT CHECK (fuel_level >= 0 AND fuel_level <= 100),
    condition_notes TEXT,                                -- Ghi chú tình trạng
    images JSON,                                         -- Array URL ảnh kiểm tra
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (contract_id) REFERENCES contracts(contract_id) ON DELETE CASCADE,
    FOREIGN KEY (staff_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_contract_type (contract_id, handover_type)
) ENGINE=InnoDB;

-- Bảng: return_fees - Phí phát sinh khi trả xe
CREATE TABLE return_fees (
    fee_id INT PRIMARY KEY AUTO_INCREMENT,
    contract_id INT NOT NULL UNIQUE,                     -- 1 contract chỉ 1 bản ghi
    handover_id INT NOT NULL,                            -- Link đến handover return
    
    is_late BOOLEAN NOT NULL,
    hours_late DECIMAL(18, 2) DEFAULT 0,
    late_fee DECIMAL(18, 2) DEFAULT 0,
    
    has_damage BOOLEAN DEFAULT FALSE,
    damage_description TEXT,
    damage_fee DECIMAL(18, 2) DEFAULT 0,
    
    is_different_location BOOLEAN DEFAULT FALSE,
    one_way_fee DECIMAL(18, 2) DEFAULT 0,
    
    total_fees DECIMAL(18, 2) NOT NULL,                  -- Tổng phí
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (contract_id) REFERENCES contracts(contract_id) ON DELETE CASCADE,
    FOREIGN KEY (handover_id) REFERENCES handovers(handover_id) ON DELETE CASCADE,
    INDEX idx_contract (contract_id)
) ENGINE=InnoDB;

-- ===================================================================
-- 5. DEPOSIT REFUND (3 bảng)
-- ===================================================================

-- Bảng: deposit_holds - Giữ cọc 14 ngày
CREATE TABLE deposit_holds (
    hold_id INT PRIMARY KEY AUTO_INCREMENT,
    contract_id INT NOT NULL UNIQUE,
    deposit_amount DECIMAL(18, 2) NOT NULL,
    deducted_at_return DECIMAL(18, 2) DEFAULT 0,        -- Đã trừ khi trả xe
    hold_start_date DATETIME NOT NULL,
    hold_end_date DATETIME NOT NULL,
    status ENUM('HOLDING', 'READY', 'REFUNDED') DEFAULT 'HOLDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (contract_id) REFERENCES contracts(contract_id) ON DELETE CASCADE,
    INDEX idx_status_date (status, hold_end_date)
) ENGINE=InnoDB;

-- Bảng: traffic_violations - Vi phạm giao thông (TÁCH RIÊNG)
CREATE TABLE traffic_violations (
    violation_id INT PRIMARY KEY AUTO_INCREMENT,
    hold_id INT NOT NULL,                                -- Link đến deposit_hold
    contract_id INT NOT NULL,
    violation_type VARCHAR(100) NOT NULL,                -- Loại vi phạm
    violation_date DATETIME NOT NULL,
    fine_amount DECIMAL(18, 2) NOT NULL,
    description TEXT,
    evidence_url VARCHAR(255),                           -- Link ảnh chứng cứ
    status ENUM('PENDING', 'CONFIRMED') DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (hold_id) REFERENCES deposit_holds(hold_id) ON DELETE CASCADE,
    FOREIGN KEY (contract_id) REFERENCES contracts(contract_id) ON DELETE CASCADE,
    INDEX idx_hold (hold_id),
    INDEX idx_contract (contract_id)
) ENGINE=InnoDB;

-- Bảng: refunds - Hoàn tiền cọc
CREATE TABLE refunds (
    refund_id INT PRIMARY KEY AUTO_INCREMENT,
    hold_id INT NOT NULL UNIQUE,
    contract_id INT NOT NULL,
    customer_id INT NOT NULL,

    original_deposit DECIMAL(18, 2) NOT NULL,
    deducted_at_return DECIMAL(18, 2) DEFAULT 0,
    traffic_fines DECIMAL(18, 2) DEFAULT 0,             -- Tổng phạt (tính từ traffic_violations)

    refund_amount DECIMAL(18, 2) NOT NULL,
    refund_method ENUM('TRANSFER', 'CASH') NOT NULL,
    status ENUM('PENDING', 'COMPLETED') DEFAULT 'PENDING',
    processed_at DATETIME,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (hold_id) REFERENCES deposit_holds(hold_id) ON DELETE CASCADE,
    FOREIGN KEY (contract_id) REFERENCES contracts(contract_id) ON DELETE CASCADE,
    FOREIGN KEY (customer_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_status (status)
) ENGINE=InnoDB;

-- ===================================================================
-- 6. SUPPORT (2 bảng)
-- ===================================================================

-- Bảng: support_tickets - Yêu cầu hỗ trợ
CREATE TABLE support_tickets (
    ticket_id INT PRIMARY KEY AUTO_INCREMENT,
    ticket_number VARCHAR(50) NOT NULL UNIQUE,
    customer_id INT,                                     -- NULL nếu guest
    category ENUM('BOOKING', 'PAYMENT', 'VEHICLE', 'GENERAL') NOT NULL,
    subject VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    status ENUM('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED') DEFAULT 'OPEN',
    assigned_to INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL DEFAULT NULL,
    FOREIGN KEY (customer_id) REFERENCES users(user_id) ON DELETE SET NULL,
    FOREIGN KEY (assigned_to) REFERENCES users(user_id) ON DELETE SET NULL,
    INDEX idx_status (status)
) ENGINE=InnoDB;

-- Bảng: support_messages - Tin nhắn + Rating
CREATE TABLE support_messages (
    message_id INT PRIMARY KEY AUTO_INCREMENT,
    ticket_id INT NOT NULL,
    sender_id INT NOT NULL,
    message_text TEXT NOT NULL,
    
    -- Rating (chỉ message cuối có rating)
    rating INT CHECK (rating >= 1 AND rating <= 5),     -- NULL nếu không phải rating message
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ticket_id) REFERENCES support_tickets(ticket_id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_ticket (ticket_id)
) ENGINE=InnoDB;

-- ===================================================================
-- 7. SYSTEM (2 bảng)
-- ===================================================================

-- Bảng: notifications - Thông báo
CREATE TABLE notifications (
    notification_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    title VARCHAR(200),
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_unread (user_id, is_read)
) ENGINE=InnoDB;

-- Note: Bills are now integrated into payments table (payment_type = 'RENTAL')

-- Bảng: system_settings - Cấu hình
CREATE TABLE system_settings (
    setting_key VARCHAR(100) PRIMARY KEY,
    setting_value TEXT NOT NULL,
    description TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ===================================================================
-- INSERT DEFAULT DATA
-- ===================================================================

-- System settings
INSERT INTO system_settings (setting_key, setting_value, description) VALUES
('deposit_hold_days', '14', 'Số ngày giữ cọc'),
('late_fee_per_hour', '50000', 'Phí trễ/giờ (VND)'),
('one_way_fee_percent', '5', 'Phí trả khác địa điểm (%)'),
('min_rental_days', '1', 'Số ngày thuê tối thiểu');

-- ===================================================================
-- SAMPLE DATA - VIETNAMESE
-- ===================================================================

-- 1. USERS (Admin, Staff, Customers)
-- Password for all: password123 (hashed with BCryptPasswordEncoder - Spring Security)
-- Hash: $2a$10$KNxt6bJBJgFC2iXKu7YpXu.TbsraYkW95TF5JswmL4pkSEhP0sWsG
INSERT INTO users (email, password_hash, full_name, phone, address, role, status) VALUES
-- Admin
('admin@carrental.vn', '$2a$10$KNxt6bJBJgFC2iXKu7YpXu.TbsraYkW95TF5JswmL4pkSEhP0sWsG', 'Nguyễn Văn Admin', '0901234567', '123 Phố Tràng Tiền, Quận Hoàn Kiếm, Hà Nội', 'Admin', 'Active'),

-- Staff
('nhanvien1@carrental.vn', '$2a$10$KNxt6bJBJgFC2iXKu7YpXu.TbsraYkW95TF5JswmL4pkSEhP0sWsG', 'Trần Thị Hương', '0912345678', '456 Phố Bà Triệu, Quận Hai Bà Trưng, Hà Nội', 'Staff', 'Active'),
('nhanvien2@carrental.vn', '$2a$10$KNxt6bJBJgFC2iXKu7YpXu.TbsraYkW95TF5JswmL4pkSEhP0sWsG', 'Lê Văn Tùng', '0923456789', '789 Đường Láng Hạ, Quận Đống Đa, Hà Nội', 'Staff', 'Active'),
('nhanvien3@carrental.vn', '$2a$10$KNxt6bJBJgFC2iXKu7YpXu.TbsraYkW95TF5JswmL4pkSEhP0sWsG', 'Phạm Thị Mai', '0934567890', '321 Đường Cầu Giấy, Quận Cầu Giấy, Hà Nội', 'Staff', 'Active'),

-- Active Customers
('khachhang1@gmail.com', '$2a$10$KNxt6bJBJgFC2iXKu7YpXu.TbsraYkW95TF5JswmL4pkSEhP0sWsG', 'Nguyễn Minh Tuấn', '0945678901', '234 Phố Huế, Quận Hai Bà Trưng, Hà Nội', 'Customer', 'Active'),
('khachhang2@gmail.com', '$2a$10$KNxt6bJBJgFC2iXKu7YpXu.TbsraYkW95TF5JswmL4pkSEhP0sWsG', 'Lê Thị Lan Anh', '0956789012', '567 Đường Giải Phóng, Quận Hai Bà Trưng, Hà Nội', 'Customer', 'Active'),
('khachhang3@gmail.com', '$2a$10$KNxt6bJBJgFC2iXKu7YpXu.TbsraYkW95TF5JswmL4pkSEhP0sWsG', 'Trần Đức Minh', '0967890123', '890 Đường Láng, Quận Đống Đa, Hà Nội', 'Customer', 'Active'),
('khachhang4@gmail.com', '$2a$10$KNxt6bJBJgFC2iXKu7YpXu.TbsraYkW95TF5JswmL4pkSEhP0sWsG', 'Phạm Thị Thu Hà', '0978901234', '123 Đường Nguyễn Chí Thanh, Quận Đống Đa, Hà Nội', 'Customer', 'Active'),
('khachhang5@gmail.com', '$2a$10$KNxt6bJBJgFC2iXKu7YpXu.TbsraYkW95TF5JswmL4pkSEhP0sWsG', 'Hoàng Văn Hải', '0989012345', '456 Đường Trần Duy Hưng, Quận Cầu Giấy, Hà Nội', 'Customer', 'Active'),
('khachhang6@gmail.com', '$2a$10$KNxt6bJBJgFC2iXKu7YpXu.TbsraYkW95TF5JswmL4pkSEhP0sWsG', 'Đặng Thị Ngọc', '0990123456', '789 Phố Chùa Bộc, Quận Đống Đa, Hà Nội', 'Customer', 'Active'),
('khachhang7@gmail.com', '$2a$10$KNxt6bJBJgFC2iXKu7YpXu.TbsraYkW95TF5JswmL4pkSEhP0sWsG', 'Bùi Văn Nam', '0901234568', '321 Đường Nguyễn Văn Cừ, Quận Long Biên, Hà Nội', 'Customer', 'Active'),
('khachhang8@gmail.com', '$2a$10$KNxt6bJBJgFC2iXKu7YpXu.TbsraYkW95TF5JswmL4pkSEhP0sWsG', 'Võ Thị Kim Loan', '0912345679', '654 Đường Lê Duẩn, Quận Hoàn Kiếm, Hà Nội', 'Customer', 'Active'),

-- Customers with pending documents
('khachhang9@gmail.com', '$2a$10$KNxt6bJBJgFC2iXKu7YpXu.TbsraYkW95TF5JswmL4pkSEhP0sWsG', 'Nguyễn Thị Bích', '0923456780', '987 Đường Xuân Thủy, Quận Cầu Giấy, Hà Nội', 'Customer', 'Active'),
('khachhang10@gmail.com', '$2a$10$KNxt6bJBJgFC2iXKu7YpXu.TbsraYkW95TF5JswmL4pkSEhP0sWsG', 'Trần Văn Phúc', '0934567891', '135 Đường Tôn Đức Thắng, Quận Đống Đa, Hà Nội', 'Customer', 'Active'),

-- Inactive customer
('khachhang11@gmail.com', '$2a$10$KNxt6bJBJgFC2iXKu7YpXu.TbsraYkW95TF5JswmL4pkSEhP0sWsG', 'Lê Thị Hồng', '0945678902', '246 Đường Tây Sơn, Quận Đống Đa, Hà Nội', 'Customer', 'Inactive');

-- 2. USER DOCUMENTS
INSERT INTO user_documents (user_id, document_type, document_number, image_url, expiry_date, status, verified_by) VALUES
-- Customer 1 - Approved
(5, 'ID_Card', '001234567890', '/uploads/docs/cmnd_nguyen_minh_tuan.webp', '2030-05-15', 'Approved', 2),
(5, 'Driver_License', 'B2-123456', '/uploads/docs/gplx_nguyen_minh_tuan.jpg', '2028-03-20', 'Approved', 2),

-- Customer 2 - Approved
(6, 'ID_Card', '001234567891', '/uploads/docs/cmnd_le_thi_lan_anh.jpg', '2029-08-10', 'Approved', 2),
(6, 'Driver_License', 'B2-234567', '/uploads/docs/gplx_le_thi_lan_anh.webp', '2027-11-30', 'Approved', 3),

-- Customer 3 - Approved
(7, 'ID_Card', '001234567892', '/uploads/docs/cmnd_tran_duc_minh.jpg', '2031-02-25', 'Approved', 2),
(7, 'Driver_License', 'B2-345678', '/uploads/docs/gplx_tran_duc_minh.webp', '2029-06-15', 'Approved', 2),

-- Customer 4 - Approved
(8, 'ID_Card', '001234567893', '/uploads/docs/cmnd_pham_thi_thu_ha.webp', '2030-12-05', 'Approved', 3),
(8, 'Driver_License', 'B2-456789', '/uploads/docs/gplx_pham_thi_thu_ha.webp', '2028-09-20', 'Approved', 3),

-- Customer 5 - Approved
(9, 'ID_Card', '001234567894', '/uploads/docs/cmnd_hoang_van_hai.jpg', '2032-04-18', 'Approved', 2),
(9, 'Driver_License', 'B2-567890', '/uploads/docs/gplx_hoang_van_hai.jpg', '2029-01-10', 'Approved', 2),

-- Customer 6 - Approved
(10, 'ID_Card', '001234567895', '/uploads/docs/cmnd_dang_thi_ngoc.jpg', '2030-07-22', 'Approved', 3),
(10, 'Driver_License', 'B2-678901', '/uploads/docs/gplx_dang_thi_ngoc.jpg', '2028-05-30', 'Approved', 2),

-- Customer 7 - Approved
(11, 'ID_Card', '001234567896', '/uploads/docs/cmnd_bui_van_nam.jpg', '2031-09-14', 'Approved', 2),
(11, 'Driver_License', 'B2-789012', '/uploads/docs/gplx_bui_van_nam.jpg', '2029-11-25', 'Approved', 3),

-- Customer 8 - Approved
(12, 'ID_Card', '001234567897', '/uploads/docs/cmnd_vo_thi_kim_loan.jpg', '2030-03-08', 'Approved', 3),
(12, 'Driver_License', 'B2-890123', '/uploads/docs/gplx_vo_thi_kim_loan.webp', '2028-12-15', 'Approved', 2),

-- Customer 9 - Pending
(13, 'ID_Card', '001234567898', '/uploads/docs/cmnd_nguyen_thi_bich.png', '2031-06-20', 'Pending', NULL),
(13, 'Driver_License', 'B2-901234', '/uploads/docs/gplx_nguyen_thi_bich.webp', '2029-08-05', 'Pending', NULL),

-- Customer 10 - Rejected
(14, 'ID_Card', '001234567899', '/uploads/docs/cmnd_tran_van_phuc.jpg', '2028-11-30', 'Rejected', 2),
(14, 'Driver_License', 'B2-012345', '/uploads/docs/gplx_tran_van_phuc.jpg', '2027-04-15', 'Pending', NULL);

-- 3. VEHICLE BRANDS
INSERT INTO vehicle_brands (brand_name) VALUES
('Toyota'),
('Honda'),
('Mazda'),
('Hyundai'),
('Kia'),
('Ford'),
('Mitsubishi'),
('Vinfast'),
('Mercedes-Benz'),
('BMW');

-- 4. VEHICLE MODELS
INSERT INTO vehicle_models (brand_id, model_name, category, year, seats, transmission, fuel_type) VALUES
-- Toyota
(1, 'Vios', 'Economy', 2023, 5, 'Automatic', 'Gasoline'),
(1, 'Camry', 'Sedan', 2025, 5, 'Automatic', 'Gasoline'),
(1, 'Fortuner', 'SUV', 2023, 7, 'Automatic', 'Diesel'),
(1, 'Innova', 'Economy', 2022, 7, 'Manual', 'Gasoline'),

-- Honda
(2, 'City', 'Economy', 2023, 5, 'Automatic', 'Gasoline'),
(2, 'Civic', 'Sedan', 2025, 5, 'Automatic', 'Gasoline'),
(2, 'CR-V', 'SUV', 2023, 7, 'Automatic', 'Gasoline'),
(2, 'Accord', 'Sedan', 2023, 5, 'Automatic', 'Gasoline'),

-- Mazda
(3, 'Mazda 3', 'Sedan', 2023, 5, 'Automatic', 'Gasoline'),
(3, 'CX-5', 'SUV', 2025, 5, 'Automatic', 'Gasoline'),
(3, 'CX-8', 'SUV', 2023, 7, 'Automatic', 'Diesel'),

-- Hyundai
(4, 'Accent', 'Economy', 2023, 5, 'Automatic', 'Gasoline'),
(4, 'Elantra', 'Sedan', 2025, 5, 'Automatic', 'Gasoline'),
(4, 'Tucson', 'SUV', 2023, 5, 'Automatic', 'Gasoline'),
(4, 'Santa Fe', 'SUV', 2025, 7, 'Automatic', 'Diesel'),

-- Kia
(5, 'Morning', 'Economy', 2022, 4, 'Manual', 'Gasoline'),
(5, 'Cerato', 'Sedan', 2023, 5, 'Automatic', 'Gasoline'),
(5, 'Seltos', 'SUV', 2025, 5, 'Automatic', 'Gasoline'),
(5, 'Sorento', 'SUV', 2023, 7, 'Automatic', 'Diesel'),

-- Ford
(6, 'Ranger', 'SUV', 2023, 5, 'Automatic', 'Diesel'),
(6, 'Everest', 'SUV', 2025, 7, 'Automatic', 'Diesel'),

-- Mitsubishi
(7, 'Attrage', 'Economy', 2022, 5, 'Manual', 'Gasoline'),
(7, 'Xpander', 'Economy', 2023, 7, 'Automatic', 'Gasoline'),

-- Vinfast
(8, 'VF 5', 'Economy', 2025, 5, 'Automatic', 'Electric'),
(8, 'VF 8', 'SUV', 2025, 5, 'Automatic', 'Electric'),
(8, 'VF 9', 'SUV', 2025, 7, 'Automatic', 'Electric'),

-- Mercedes
(9, 'E-Class', 'Luxury', 2025, 5, 'Automatic', 'Gasoline'),
(9, 'S-Class', 'Luxury', 2025, 5, 'Automatic', 'Gasoline'),

-- BMW
(10, '5 Series', 'Luxury', 2025, 5, 'Automatic', 'Gasoline'),
(10, 'X5', 'Luxury', 2025, 7, 'Automatic', 'Diesel');

-- 5. LOCATIONS
INSERT INTO locations (location_name, address, city, phone) VALUES
('Chi nhánh Hoàn Kiếm', '123 Phố Tràng Tiền, Phường Tràng Tiền, Quận Hoàn Kiếm', 'Hà Nội', '0241234567'),
('Chi nhánh Hai Bà Trưng', '456 Phố Bà Triệu, Phường Bạch Đằng, Quận Hai Bà Trưng', 'Hà Nội', '0241234568'),
('Chi nhánh Cầu Giấy', '789 Đường Xuân Thủy, Phường Dịch Vọng Hậu, Quận Cầu Giấy', 'Hà Nội', '0241234569'),
('Chi nhánh Đống Đa', '321 Đường Láng, Phường Láng Thượng, Quận Đống Đa', 'Hà Nội', '0241234570'),
('Chi nhánh Long Biên', '135 Đường Nguyễn Văn Cừ, Phường Ngọc Lâm, Quận Long Biên', 'Hà Nội', '0241234571'),
('Chi nhánh Nội Bài', '246 Đường Võ Nguyên Giáp, Sóc Sơn', 'Hà Nội', '0241234572'),
('Chi nhánh Hà Đông', '357 Đường Quang Trung, Phường Quang Trung, Quận Hà Đông', 'Hà Nội', '0241234573');

-- 6. VEHICLES
-- Lưu ý: Tên file ảnh được đặt theo biển số xe (loại bỏ dấu gạch ngang, chữ thường)
-- Ví dụ: Biển số 30A-12345 → Tên file: 30a12345.jpg (hoặc 30a12345.png, 30a12345.jpeg, v.v.)
-- Hỗ trợ các định dạng: .jpg, .jpeg, .png, .webp, .gif, v.v.
-- Tất cả ảnh phải được lưu trong folder: src/main/resources/static/images/
INSERT INTO vehicles (model_id, location_id, license_plate, color, status, daily_rate, deposit_amount, images) VALUES
-- Available vehicles - All deposits standardized to 50,000,000 VND
(1, 1, '30A-12345', 'Trắng', 'Available', 500000, 50000000, '["30a12345.jpg"]'),
(1, 1, '30A-12346', 'Đen', 'Available', 500000, 50000000, '["30a12346.webp"]'),
(5, 1, '30B-23456', 'Bạc', 'Available', 550000, 50000000, '["30b23456.jpg"]'),
(12, 2, '30C-34567', 'Đỏ', 'Available', 600000, 50000000, '["30c34567.jpg"]'),
(16, 2, '30D-45678', 'Xanh', 'Available', 400000, 50000000, '["30d45678.jpg"]'),
(22, 3, '30E-56789', 'Trắng', 'Available', 450000, 50000000, '["30e56789.webp"]'),
(4, 3, '30F-67890', 'Bạc', 'Available', 700000, 50000000, '["30f67890.jpg"]'),
(23, 4, '30G-78901', 'Đen', 'Available', 800000, 50000000, '["30g78901.jpg"]'),

-- Sedan category
(2, 1, '30H-89012', 'Đen', 'Available', 1200000, 50000000, '["30h89012.webp"]'),
(6, 2, '30I-90123', 'Trắng', 'Available', 1100000, 50000000, '["30i90123.webp"]'),
(9, 3, '30J-01234', 'Đỏ', 'Available', 1150000, 50000000, '["30j01234.jpg"]'),
(13, 4, '30K-12347', 'Xanh', 'Available', 1100000, 50000000, '["30k12347.jpg"]'),

-- SUV category
(3, 1, '30L-23458', 'Đen', 'Available', 1800000, 50000000, '["30l23458.jpg"]'),
(7, 2, '30M-34569', 'Trắng', 'Available', 1500000, 50000000, '["30m34569.jpg"]'),
(10, 3, '30N-45670', 'Bạc', 'Available', 1600000, 50000000, '["30n45670.jpg"]'),
(14, 4, '30O-56781', 'Đỏ', 'Available', 1550000, 50000000, '["30o56781.jpeg"]'),
(18, 1, '30P-67892', 'Xanh', 'Available', 1650000, 50000000, '["30p67892.jpg"]'),

-- Luxury category
(27, 1, '30Q-78903', 'Đen', 'Available', 3500000, 50000000, '["30q78903.webp"]'),
(28, 2, '30R-89014', 'Trắng', 'Available', 5000000, 50000000, '["30r89014.jpg"]'),
(29, 3, '30S-90125', 'Bạc', 'Available', 3800000, 50000000, '["30s90125.webp"]'),

-- Rented vehicles (for active contracts)
(1, 1, '30T-01235', 'Trắng', 'Rented', 500000, 50000000, '["30t01235.webp"]'),
(5, 2, '30U-12348', 'Đen', 'Rented', 550000, 50000000, '["30u12348.jpg"]'),
(9, 3, '30V-23459', 'Đỏ', 'Rented', 1150000, 50000000, '["30v23459.webp"]'),
(10, 4, '30W-34570', 'Xanh', 'Rented', 1600000, 50000000, '["30w34570.jpg"]'),
(27, 1, '30X-45681', 'Đen', 'Rented', 3500000, 50000000, '["30x45681.webp"]'),

-- Maintenance vehicles
(3, 5, '30AB-11111', 'Đen', 'Maintenance', 1800000, 50000000, '["30ab11111.jpg"]'),
(7, 6, '30AC-22222', 'Trắng', 'Maintenance', 1500000, 50000000, '["30ac22222.webp"]'),

-- Electric vehicles
(24, 3, '30Y-56792', 'Trắng', 'Available', 800000, 50000000, '["30y56792.webp"]'),
(25, 4, '30Z-67803', 'Đen', 'Available', 1400000, 50000000, '["30z67803.jpg"]'),
(26, 1, '30AA-78914', 'Xanh', 'Available', 1900000, 50000000, '["30aa78914.webp"]');

-- 7. BOOKINGS
INSERT INTO bookings (booking_id, customer_id, vehicle_id, pickup_location_id, return_location_id, start_date, end_date, total_days, status, rejection_reason, created_at) VALUES
-- Approved bookings (will create contracts)
(1, 5, 21, 1, 1, '2025-03-12 09:00:00', '2025-03-15 18:00:00', 3, 'Approved', NULL, '2025-03-07 10:30:00'),
(2, 6, 22, 2, 2, '2025-03-14 08:00:00', '2025-03-17 17:00:00', 3, 'Approved', NULL, '2025-03-08 14:20:00'),
(3, 7, 23, 3, 4, '2025-03-17 10:00:00', '2025-03-22 16:00:00', 5, 'Approved', NULL, '2025-03-09 09:15:00'),

-- Pending bookings (waiting for review)
(4, 8, 1, 1, 1, '2025-03-22 09:00:00', '2025-03-27 18:00:00', 5, 'Pending', NULL, '2025-03-06 11:00:00'),
(5, 9, 5, 2, 2, '2025-03-24 08:30:00', '2025-03-29 17:30:00', 5, 'Pending', NULL, '2025-03-06 13:45:00'),
(6, 10, 12, 3, 3, '2025-03-27 10:00:00', '2025-04-01 16:00:00', 5, 'Pending', NULL, '2025-03-06 15:20:00'),
(7, 11, 16, 4, 4, '2025-03-30 09:00:00', '2025-04-04 18:00:00', 5, 'Pending', NULL, '2025-03-06 16:30:00'),

-- Rejected bookings
(8, 13, 8, 3, 3, '2025-03-17 09:00:00', '2025-03-20 18:00:00', 3, 'Rejected', 'Không đủ giấy tờ xác minh.', '2025-03-05 10:00:00'),
(9, 14, 11, 4, 4, '2025-03-20 08:00:00', '2025-03-24 17:00:00', 4, 'Rejected', 'Lịch thuê trùng với lịch bảo dưỡng xe.', '2025-03-05 14:30:00'),

-- Cancelled bookings
(10, 6, 7, 3, 3, '2025-02-25 09:00:00', '2025-02-28 18:00:00', 3, 'Cancelled', NULL, '2025-02-24 09:00:00'),
(11, 5, 8, 1, 2, '2025-03-22 09:00:00', '2025-03-25 18:00:00', 3, 'Cancelled', NULL, '2025-03-21 11:30:00');

-- 8. BOOKING DOCUMENTS
INSERT INTO booking_documents (booking_id, document_id) VALUES
-- Booking 1
(1, 1), (1, 2),
-- Booking 2
(2, 3), (2, 4),
-- Booking 3
(3, 5), (3, 6),
-- Booking 4
(4, 7), (4, 8),
-- Booking 5
(5, 9), (5, 10),
-- Booking 6
(6, 11), (6, 12),
-- Booking 7
(7, 13), (7, 14);

-- 9. CONTRACTS
-- Note: We need to create more bookings first for completed contracts
-- Adding more completed bookings
INSERT INTO bookings (booking_id, customer_id, vehicle_id, pickup_location_id, return_location_id, start_date, end_date, total_days, status, created_at) VALUES
-- Completed bookings for past contracts
(12, 8, 9, 1, 1, '2025-02-01 09:00:00', '2025-02-05 18:00:00', 4, 'Approved', '2025-01-30 10:00:00'),
(13, 9, 14, 2, 2, '2025-02-10 08:00:00', '2025-02-15 17:00:00', 5, 'Approved', '2025-02-08 14:00:00'),
(14, 10, 15, 3, 4, '2025-02-15 10:00:00', '2025-02-20 16:00:00', 5, 'Approved', '2025-02-13 09:00:00'),
(15, 11, 10, 4, 4, '2025-01-20 09:00:00', '2025-01-25 18:00:00', 5, 'Approved', '2025-01-18 11:00:00'),

-- TEST BOOKINGS for handover pickup testing (contracts 10-14)
-- These bookings are approved and will have Active contracts with deposit paid but NO pickup handover
(16, 5, 1, 1, 1, '2025-01-10 09:00:00', '2025-01-13 18:00:00', 3, 'Approved', '2025-01-08 10:00:00'),
(17, 6, 5, 2, 2, '2025-01-12 08:00:00', '2025-01-15 17:00:00', 3, 'Approved', '2025-01-10 14:00:00'),
(18, 7, 12, 3, 3, '2025-01-08 10:00:00', '2025-01-12 16:00:00', 4, 'Approved', '2025-01-06 09:00:00'),
(19, 8, 16, 4, 4, '2025-01-15 09:00:00', '2025-01-20 18:00:00', 5, 'Approved', '2025-01-13 11:00:00'),
(20, 9, 22, 2, 2, '2025-01-11 08:30:00', '2025-01-16 17:30:00', 5, 'Approved', '2025-01-09 15:00:00');

-- Add booking documents for test bookings
INSERT INTO booking_documents (booking_id, document_id) VALUES
-- Test Bookings 16-20 (using same documents as their customers)
(16, 1), (16, 2),
(17, 3), (17, 4),
(18, 5), (18, 6),
(19, 7), (19, 8),
(20, 9), (20, 10);

INSERT INTO contracts (contract_id, booking_id, contract_number, customer_id, vehicle_id, staff_id, start_date, end_date, total_days, daily_rate, total_rental_fee, deposit_amount, status, created_at) VALUES
-- Active contracts (currently renting)
-- All deposits are 50,000,000 VND as per business requirement
-- Contract 1: Trễ hạn (endDate đã qua)
(1, 1, 'HD-2025-0001', 5, 21, 2, '2025-03-12 09:00:00', '2025-03-15 18:00:00', 3, 500000, 1500000, 50000000, 'Active', '2025-03-11 14:00:00'),
-- Contract 2: Đúng hạn (endDate trong tương lai)
(2, 2, 'HD-2025-0002', 6, 22, 2, '2025-03-20 08:00:00', '2025-04-05 17:00:00', 16, 550000, 8800000, 50000000, 'Active', '2025-03-19 10:00:00'),
-- Contract 3: Đúng hạn (endDate trong tương lai)
(3, 3, 'HD-2025-0003', 7, 23, 3, '2025-03-25 10:00:00', '2025-04-10 16:00:00', 16, 1150000, 18400000, 50000000, 'Active', '2025-03-24 15:30:00'),

-- Completed contracts (returned vehicles)
-- Contract 4: Normal return, no issues
(4, 12, 'HD-2025-0004', 8, 9, 2, '2025-02-01 09:00:00', '2025-02-05 18:00:00', 4, 1200000, 4800000, 50000000, 'Completed', '2025-01-31 11:00:00'),

-- Contract 5: Late return + damage fee
(5, 13, 'HD-2025-0005', 9, 14, 3, '2025-02-10 08:00:00', '2025-02-15 17:00:00', 5, 1500000, 7500000, 50000000, 'Completed', '2025-02-09 09:30:00'),

-- Contract 6: Different location return
(6, 14, 'HD-2025-0006', 10, 15, 2, '2025-02-15 10:00:00', '2025-02-20 16:00:00', 5, 1600000, 8000000, 50000000, 'Completed', '2025-02-14 14:20:00'),

-- Contract 7: Normal return with traffic violation
(7, 15, 'HD-2025-0007', 11, 10, 3, '2025-01-20 09:00:00', '2025-01-25 18:00:00', 5, 1100000, 5500000, 50000000, 'Completed', '2025-01-19 10:45:00'),

-- Cancelled contract (contract exists but booking was cancelled - demonstrating the fix)
-- This shows that booking_id can reference a cancelled booking
(8, 10, 'HD-2025-0008', 6, 7, 2, '2025-02-25 09:00:00', '2025-02-28 18:00:00', 3, 800000, 2400000, 50000000, 'Cancelled', '2025-02-24 13:00:00'),

-- Contract 9: Returned, waiting customer to pay bill (new Bill_Pending status)
-- Note: booking_id is NULL here to avoid unique booking_id conflict in seed data
(9, NULL, 'HD-2025-0009', 5, 1, 2, '2025-01-10 09:00:00', '2025-01-13 18:00:00', 3, 950000, 2850000, 50000000, 'Bill_Pending', '2025-01-09 12:00:00'),

-- Pending payment contract with cancelled booking (demonstrating the nullable booking scenario)
-- Customer created booking #11 for Mitsubishi Xpander (vehicle_id=8), contract was created,
-- but then customer cancelled the booking before payment
-- Contract still exists with booking_id=11 (which is now cancelled)
(200, 11, 'HD-2025-0200', 5, 8, 2, '2025-03-22 09:00:00', '2025-03-25 18:00:00', 3, 800000, 2400000, 50000000, 'Pending_Payment', '2025-03-21 16:00:00'),

-- TEST CONTRACTS for handover pickup testing (contracts 10-14)
-- These contracts have Active status with deposit paid but NO pickup handover
-- They will appear in the handover pickup list at /staff/handover/pickup
(10, 16, 'HD-2025-0010', 5, 1, 2, '2025-01-10 09:00:00', '2025-01-13 18:00:00', 3, 500000, 1500000, 50000000, 'Active', '2025-01-09 10:00:00'),
(11, 17, 'HD-2025-0011', 6, 5, 2, '2025-01-12 08:00:00', '2025-01-15 17:00:00', 3, 550000, 1650000, 50000000, 'Active', '2025-01-11 14:00:00'),
(12, 18, 'HD-2025-0012', 7, 12, 3, '2025-01-08 10:00:00', '2025-01-12 16:00:00', 4, 600000, 2400000, 50000000, 'Active', '2025-01-07 09:00:00'),
(13, 19, 'HD-2025-0013', 8, 16, 2, '2025-01-15 09:00:00', '2025-01-20 18:00:00', 5, 400000, 2000000, 50000000, 'Active', '2025-01-14 11:00:00'),
(14, 20, 'HD-2025-0014', 9, 22, 3, '2025-01-11 08:30:00', '2025-01-16 17:30:00', 5, 550000, 2750000, 50000000, 'Active', '2025-01-10 15:00:00');

-- 10. PAYMENTS
-- Note: New fields added for online payment gateway integration and bill details:
-- Gateway fields: transaction_ref, gateway_transaction_id, gateway_response_code, gateway_transaction_status,
-- gateway_bank_code, gateway_card_type, gateway_pay_date, gateway_secure_hash, payment_url
-- Bill fields (for RENTAL payment type): bill_number, original_rental_fee, rental_adjustment, actual_rental_fee,
-- late_fee, damage_fee, one_way_fee, total_additional_fees, deposit_amount, amount_paid, amount_due, notes
INSERT INTO payments (contract_id, payment_type, amount, method, status, payment_date, created_at,
                     transaction_ref, gateway_transaction_id, gateway_response_code, gateway_transaction_status,
                     gateway_bank_code, gateway_card_type, gateway_pay_date, gateway_secure_hash, payment_url,
                     bill_number, original_rental_fee, rental_adjustment, actual_rental_fee,
                     late_fee, damage_fee, one_way_fee, total_additional_fees, deposit_amount, amount_paid, amount_due, notes) VALUES
-- Active contracts - Deposit paid (all 50,000,000 VND)
(1, 'DEPOSIT', 50000000, 'TRANSFER', 'COMPLETED', '2025-03-11 14:30:00', '2025-03-11 14:30:00',
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(2, 'DEPOSIT', 50000000, 'CARD', 'COMPLETED', '2025-03-13 10:30:00', '2025-03-13 10:30:00',
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(3, 'DEPOSIT', 50000000, 'TRANSFER', 'COMPLETED', '2025-03-16 16:00:00', '2025-03-16 16:00:00',
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),

-- Completed contracts - Full payments (deposit 50,000,000 VND)
-- Note: RENTAL payments below are old sample data (before bill integration), bill fields are NULL
(4, 'DEPOSIT', 50000000, 'TRANSFER', 'COMPLETED', '2025-01-31 11:30:00', '2025-01-31 11:30:00',
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(4, 'RENTAL', 4800000, 'CARD', 'COMPLETED', '2025-02-05 19:00:00', '2025-02-05 19:00:00',
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),

(5, 'DEPOSIT', 50000000, 'CARD', 'COMPLETED', '2025-02-09 10:00:00', '2025-02-09 10:00:00',
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(5, 'RENTAL', 7500000, 'TRANSFER', 'COMPLETED', '2025-02-16 11:00:00', '2025-02-16 11:00:00',
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),

(6, 'DEPOSIT', 50000000, 'TRANSFER', 'COMPLETED', '2025-02-14 14:45:00', '2025-02-14 14:45:00',
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(6, 'RENTAL', 8000000, 'CARD', 'COMPLETED', '2025-02-21 10:30:00', '2025-02-21 10:30:00',
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),

(7, 'DEPOSIT', 50000000, 'CARD', 'COMPLETED', '2025-01-19 11:15:00', '2025-01-19 11:15:00',
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(7, 'RENTAL', 5500000, 'TRANSFER', 'COMPLETED', '2025-01-25 19:30:00', '2025-01-25 19:30:00',
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),

-- Cancelled contract - Refund (deposit 50,000,000 VND)
(8, 'DEPOSIT', 50000000, 'TRANSFER', 'COMPLETED', '2025-02-24 13:30:00', '2025-02-24 13:30:00',
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(8, 'REFUND', 50000000, 'TRANSFER', 'COMPLETED', '2025-02-24 15:00:00', '2025-02-24 15:00:00',
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),

-- Pending payment contract - Online payment gateway (sample)
-- This demonstrates a pending online payment awaiting customer to complete
(9, 'DEPOSIT', 50000000, 'ONLINE', 'PENDING', NULL, '2025-03-11 16:05:00',
 'DEPOSIT9_12345678', NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 'https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=5000000000&vnp_Command=pay&...',
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),

-- TEST CONTRACTS deposit payments (contracts 10-14)
-- All deposits are 50,000,000 VND and status is COMPLETED
(10, 'DEPOSIT', 50000000, 'TRANSFER', 'COMPLETED', '2025-01-09 10:30:00', '2025-01-09 10:30:00',
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(11, 'DEPOSIT', 50000000, 'CARD', 'COMPLETED', '2025-01-11 14:30:00', '2025-01-11 14:30:00',
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(12, 'DEPOSIT', 50000000, 'TRANSFER', 'COMPLETED', '2025-01-07 09:30:00', '2025-01-07 09:30:00',
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(13, 'DEPOSIT', 50000000, 'CARD', 'COMPLETED', '2025-01-14 11:30:00', '2025-01-14 11:30:00',
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(14, 'DEPOSIT', 50000000, 'TRANSFER', 'COMPLETED', '2025-01-10 15:30:00', '2025-01-10 15:30:00',
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);

-- 11. HANDOVERS
INSERT INTO handovers (contract_id, handover_type, staff_id, handover_time, odometer, fuel_level, condition_notes, images, created_at) VALUES
-- Active contracts - Pickup only (for testing return car functionality - contracts 1-3 will appear in return list)
-- Contract 1: Trễ hạn (pickup đã lâu)
(1, 'PICKUP', 2, '2025-03-12 09:30:00', 45000, 100, 'Xe trong tình trạng tốt, đầy xăng, không trầy xước', '["https://example.com/pickup/h1-1.jpg","https://example.com/pickup/h1-2.jpg"]', '2025-03-12 09:30:00'),
-- Contract 2: Đúng hạn (pickup gần đây, endDate trong tương lai)
(2, 'PICKUP', 2, '2025-03-20 08:30:00', 38000, 100, 'Xe sạch sẽ, không vấn đề gì', '["https://example.com/pickup/h2-1.jpg"]', '2025-03-20 08:30:00'),
-- Contract 3: Đúng hạn (pickup gần đây, endDate trong tương lai)
(3, 'PICKUP', 3, '2025-03-25 10:30:00', 52000, 100, 'Xe trong tình trạng hoàn hảo', '["https://example.com/pickup/h3-1.jpg","https://example.com/pickup/h3-2.jpg"]', '2025-03-25 10:30:00'),

-- Completed contracts - Pickup and Return
-- Contract 4: Normal return
(4, 'PICKUP', 2, '2025-02-01 09:30:00', 62000, 100, 'Xe tốt, không vấn đề', '["https://example.com/pickup/h4-1.jpg"]', '2025-02-01 09:30:00'),
(4, 'RETURN', 2, '2025-02-05 18:20:00', 62800, 95, 'Xe trả đúng hạn, tình trạng tốt, xăng còn 95%', '["https://example.com/return/h4-1.jpg","https://example.com/return/h4-2.jpg"]', '2025-02-05 18:20:00'),

-- Contract 5: Late return + damage
(5, 'PICKUP', 3, '2025-02-10 08:30:00', 78000, 100, 'Xe trong tình trạng tốt', '["https://example.com/pickup/h5-1.jpg"]', '2025-02-10 08:30:00'),
(5, 'RETURN', 3, '2025-02-16 10:30:00', 79200, 70, 'Trả trễ 17.5 giờ, có trầy xước ở cửa phụ, xăng còn 70%', '["https://example.com/return/h5-1.jpg","https://example.com/return/h5-2.jpg","https://example.com/return/h5-3.jpg"]', '2025-02-16 10:30:00'),

-- Contract 6: Different location return
(6, 'PICKUP', 2, '2025-02-15 10:30:00', 55000, 100, 'Xe hoàn hảo', '["https://example.com/pickup/h6-1.jpg"]', '2025-02-15 10:30:00'),
(6, 'RETURN', 4, '2025-02-21 10:00:00', 55850, 90, 'Trả xe tại địa điểm khác, tình trạng tốt', '["https://example.com/return/h6-1.jpg","https://example.com/return/h6-2.jpg"]', '2025-02-21 10:00:00'),

-- Contract 7: Normal return (will have traffic violation later)
(7, 'PICKUP', 3, '2025-01-20 09:30:00', 41000, 100, 'Xe tốt', '["https://example.com/pickup/h7-1.jpg"]', '2025-01-20 09:30:00'),
(7, 'RETURN', 3, '2025-01-25 18:30:00', 41650, 100, 'Trả đúng hạn, xe trong tình trạng tốt', '["https://example.com/return/h7-1.jpg"]', '2025-01-25 18:30:00');

-- 12. RETURN FEES
INSERT INTO return_fees (contract_id, handover_id, is_late, hours_late, late_fee, has_damage, damage_description, damage_fee, is_different_location, one_way_fee, total_fees, created_at) VALUES
-- Contract 4: No fees
(4, 2, FALSE, 0, 0, FALSE, NULL, 0, FALSE, 0, 0, '2025-02-05 18:30:00'),

-- Contract 5: Late + Damage
(5, 4, TRUE, 17.5, 875000, TRUE, 'Trầy xước cửa xe phụ bên phải, kích thước 15cm', 2000000, FALSE, 0, 2875000, '2025-02-16 10:45:00'),

-- Contract 6: One-way fee
(6, 6, FALSE, 0, 0, FALSE, NULL, 0, TRUE, 1200000, 1200000, '2025-02-21 10:15:00'),

-- Contract 7: No fees (but will have traffic violation)
(7, 8, FALSE, 0, 0, FALSE, NULL, 0, FALSE, 0, 0, '2025-01-25 18:45:00');

-- 13. DEPOSIT HOLDS
INSERT INTO deposit_holds (contract_id, deposit_amount, deducted_at_return, hold_start_date, hold_end_date, status, created_at) VALUES
-- Contract 4: Ready for refund (no violations) - Deposit 50,000,000 VND
(4, 50000000, 0, '2025-02-05 18:30:00', '2025-02-19 18:30:00', 'READY', '2025-02-05 18:30:00'),

-- Contract 5: Ready for refund (fees deducted) - Deposit 50,000,000 VND
(5, 50000000, 2875000, '2025-02-16 10:45:00', '2025-03-02 10:45:00', 'READY', '2025-02-16 10:45:00'),

-- Contract 6: Ready for refund (one-way fee deducted) - Deposit 50,000,000 VND
(6, 50000000, 1200000, '2025-02-21 10:15:00', '2025-03-07 10:15:00', 'READY', '2025-02-21 10:15:00'),

-- Contract 7: Holding (waiting for traffic violation check) - Deposit 50,000,000 VND
(7, 50000000, 0, '2025-01-25 18:45:00', '2025-02-08 18:45:00', 'HOLDING', '2025-01-25 18:45:00');

-- 14. TRAFFIC VIOLATIONS
INSERT INTO traffic_violations (hold_id, contract_id, violation_type, violation_date, fine_amount, description, evidence_url, status, created_at) VALUES
-- Contract 7: Traffic violations during rental period
(4, 7, 'Vượt đèn đỏ', '2025-01-22 15:30:00', 1000000, 'Vượt đèn đỏ tại ngã tư Lê Lợi - Nguyễn Huệ', 'https://example.com/violations/v1.jpg', 'Confirmed', '2025-02-02 09:00:00'),
(4, 7, 'Đi quá tốc độ', '2025-01-24 08:45:00', 800000, 'Vượt tốc độ cho phép 20km/h trên đường Võ Văn Kiệt', 'https://example.com/violations/v2.jpg', 'Confirmed', '2025-02-03 10:30:00'),

-- Pending violation
(4, 7, 'Đậu xe sai quy định', '2025-01-23 14:20:00', 500000, 'Đậu xe trên vỉa hè đường Hai Bà Trưng', 'https://example.com/violations/v3.jpg', 'Pending', '2025-02-05 11:00:00');

-- 15. REFUNDS
INSERT INTO refunds (hold_id, contract_id, customer_id, original_deposit, deducted_at_return, traffic_fines, refund_amount, refund_method, status, processed_at, created_at) VALUES
-- Contract 4: Full refund completed - Deposit 50,000,000 VND
(1, 4, 8, 50000000, 0, 0, 50000000, 'Transfer', 'Completed', '2025-02-20 10:00:00', '2025-02-19 18:30:00'),

-- Contract 5: Partial refund completed (fees deducted) - Deposit 50,000,000 VND
(2, 5, 9, 50000000, 2875000, 0, 47125000, 'Transfer', 'Completed', '2025-03-03 14:30:00', '2025-03-02 10:45:00'),

-- Contract 6: Partial refund pending - Deposit 50,000,000 VND
(3, 6, 10, 50000000, 1200000, 0, 48800000, 'Transfer', 'Pending', NULL, '2025-03-06 10:15:00');

-- 16. SUPPORT TICKETS
INSERT INTO support_tickets (ticket_number, customer_id, category, subject, description, status, assigned_to, created_at) VALUES
-- Open tickets
('TK-2025-0001', 5, 'BOOKING', 'Thay đổi thời gian thuê xe', 'Em muốn thay đổi thời gian nhận xe từ 9h sáng sang 14h chiều được không ạ?', 'OPEN', NULL, '2025-03-06 08:30:00'),
('TK-2025-0002', 6, 'VEHICLE', 'Hỏi về tình trạng xe', 'Xe Toyota Camry còn mới không ạ? Em muốn thuê xe mới nhất có thể.', 'OPEN', NULL, '2025-03-06 09:15:00'),
('TK-2025-0003', NULL, 'GENERAL', 'Hỏi về giấy tờ cần thiết', 'Cho em hỏi thuê xe cần giấy tờ gì ạ? Em chưa có GPLX lâu năm.', 'OPEN', NULL, '2025-03-06 10:00:00'),

-- In Progress tickets
('TK-2025-0004', 7, 'PAYMENT', 'Vấn đề thanh toán tiền cọc', 'Em chuyển khoản tiền cọc rồi nhưng chưa thấy cập nhật trạng thái thanh toán.', 'IN_PROGRESS', 2, '2025-03-05 14:20:00'),
('TK-2025-0005', 8, 'BOOKING', 'Yêu cầu hủy booking', 'Em có việc đột xuất không thể thuê xe được, muốn hủy booking và hoàn tiền.', 'IN_PROGRESS', 3, '2025-03-05 16:45:00'),

-- Resolved tickets
('TK-2025-0006', 9, 'VEHICLE', 'Xe bị hỏng giữa đường', 'Em đang thuê xe mà xe bị chết máy giữa đường, cần hỗ trợ gấp!', 'RESOLVED', 2, '2025-03-04 11:30:00'),
('TK-2025-0007', 10, 'PAYMENT', 'Hỏi về phí trả xe khác địa điểm', 'Nếu em nhận xe ở Q1 mà trả ở Tân Bình thì tính phí thế nào ạ?', 'RESOLVED', 3, '2025-03-03 09:00:00'),
('TK-2025-0008', 11, 'GENERAL', 'Hỏi về chính sách hoàn tiền cọc', 'Cho em hỏi tiền cọc hoàn sau bao lâu và có trường hợp nào bị giữ cọc không ạ?', 'RESOLVED', 2, '2025-03-02 15:20:00'),

-- Closed tickets (with ratings)
('TK-2025-0009', 5, 'BOOKING', 'Hỏi về thời gian thuê tối thiểu', 'Em chỉ muốn thuê 1 ngày được không ạ?', 'CLOSED', 2, '2025-02-28 10:00:00'),
('TK-2025-0010', 6, 'VEHICLE', 'Xe có bluetooth không?', 'Em muốn thuê xe có kết nối bluetooth để nghe nhạc.', 'CLOSED', 3, '2025-02-27 14:30:00');

-- 17. SUPPORT MESSAGES
INSERT INTO support_messages (ticket_id, sender_id, message_text, rating, created_at) VALUES
-- Ticket 1: Open (no response yet)
(1, 5, 'Em muốn thay đổi thời gian nhận xe từ 9h sáng sang 14h chiều được không ạ?', NULL, '2025-03-06 08:30:00'),

-- Ticket 2: Open (no response yet)
(2, 6, 'Xe Toyota Camry còn mới không ạ? Em muốn thuê xe mới nhất có thể.', NULL, '2025-03-06 09:15:00'),

-- Ticket 3: Open (guest inquiry)
(3, 1, 'Cho em hỏi thuê xe cần giấy tờ gì ạ? Em chưa có GPLX lâu năm.', NULL, '2025-03-06 10:00:00'),

-- Ticket 4: In Progress (conversation ongoing)
(4, 7, 'Em chuyển khoản tiền cọc rồi nhưng chưa thấy cập nhật trạng thái thanh toán.', NULL, '2025-03-05 14:20:00'),
(4, 2, 'Chào anh/chị, cho em xin mã giao dịch để kiểm tra ạ.', NULL, '2025-03-05 14:35:00'),
(4, 7, 'Mã GD là: 1234567890. Em chuyển lúc 10h sáng nay.', NULL, '2025-03-05 14:50:00'),
(4, 2, 'Dạ, em đã tìm thấy. Hệ thống sẽ cập nhật trong 15 phút nữa ạ.', NULL, '2025-03-05 15:00:00'),

-- Ticket 5: In Progress
(5, 8, 'Em có việc đột xuất không thể thuê xe được, muốn hủy booking và hoàn tiền.', NULL, '2025-03-05 16:45:00'),
(5, 3, 'Chào anh/chị, booking của anh/chị đã được duyệt chưa ạ?', NULL, '2025-03-05 17:00:00'),
(5, 8, 'Chưa ạ, vẫn đang chờ duyệt.', NULL, '2025-03-05 17:15:00'),

-- Ticket 6: Resolved (emergency handled)
(6, 9, 'Em đang thuê xe mà xe bị chết máy giữa đường, cần hỗ trợ gấp!', NULL, '2025-03-04 11:30:00'),
(6, 2, 'Em ở đâu ạ? Bên mình sẽ cử nhân viên đến hỗ trợ ngay!', NULL, '2025-03-04 11:32:00'),
(6, 9, 'Em đang ở đường Võ Văn Tần gần ngã tư với Hai Bà Trưng ạ.', NULL, '2025-03-04 11:35:00'),
(6, 2, 'Nhân viên đang trên đường đến, khoảng 15 phút nữa sẽ đến nơi ạ.', NULL, '2025-03-04 11:40:00'),
(6, 9, 'Cảm ơn anh/chị nhiều ạ!', NULL, '2025-03-04 12:00:00'),
(6, 2, 'Vấn đề đã được xử lý. Xe đã hoạt động trở lại bình thường ạ.', NULL, '2025-03-04 12:30:00'),

-- Ticket 7: Resolved
(7, 10, 'Nếu em nhận xe ở Q1 mà trả ở Tân Bình thì tính phí thế nào ạ?', NULL, '2025-03-03 09:00:00'),
(7, 3, 'Chào anh/chị, phí trả xe khác địa điểm là 15% trên tổng tiền thuê ạ. Ví dụ tổng tiền thuê 10 triệu thì phí thêm là 1.5 triệu.', NULL, '2025-03-03 09:15:00'),
(7, 10, 'Em hiểu rồi, cảm ơn anh/chị.', NULL, '2025-03-03 09:30:00'),

-- Ticket 8: Resolved
(8, 11, 'Cho em hỏi tiền cọc hoàn sau bao lâu và có trường hợp nào bị giữ cọc không ạ?', NULL, '2025-03-02 15:20:00'),
(8, 2, 'Chào anh/chị, tiền cọc sẽ được giữ 14 ngày để kiểm tra vi phạm giao thông. Sau 14 ngày nếu không có vi phạm sẽ hoàn đầy đủ. Nếu có vi phạm sẽ trừ tiền phạt từ cọc và hoàn lại phần còn lại ạ.', NULL, '2025-03-02 15:35:00'),
(8, 11, 'Vậy nếu có hư hỏng xe thì sao ạ?', NULL, '2025-03-02 15:50:00'),
(8, 2, 'Nếu có hư hỏng sẽ tính phí ngay khi trả xe và trừ vào cọc luôn ạ. Còn vi phạm giao thông thì phải đợi 14 ngày mới biết có vi phạm hay không.', NULL, '2025-03-02 16:00:00'),
(8, 11, 'Em hiểu rồi, cảm ơn nhiều ạ!', NULL, '2025-03-02 16:15:00'),

-- Ticket 9: Closed with rating
(9, 5, 'Em chỉ muốn thuê 1 ngày được không ạ?', NULL, '2025-02-28 10:00:00'),
(9, 2, 'Dạ được ạ, bên mình cho thuê tối thiểu 1 ngày. Anh/chị có thể đặt xe ngay trên website ạ.', NULL, '2025-02-28 10:15:00'),
(9, 5, 'Cảm ơn anh/chị!', NULL, '2025-02-28 10:30:00'),
(9, 5, 'Nhân viên tư vấn nhiệt tình, giải đáp nhanh chóng!', 5, '2025-02-28 11:00:00'),

-- Ticket 10: Closed with rating
(10, 6, 'Em muốn thuê xe có kết nối bluetooth để nghe nhạc.', NULL, '2025-02-27 14:30:00'),
(10, 3, 'Chào anh/chị, hầu hết các xe đời mới của bên mình đều có bluetooth ạ. Anh/chị có thể xem thông tin chi tiết từng xe trên trang danh sách xe.', NULL, '2025-02-27 14:45:00'),
(10, 6, 'Vậy xe Toyota Vios 2023 có không ạ?', NULL, '2025-02-27 15:00:00'),
(10, 3, 'Dạ có ạ, xe Vios 2023 đều có kết nối bluetooth và Apple CarPlay nữa ạ.', NULL, '2025-02-27 15:15:00'),
(10, 6, 'Tuyệt vời, cảm ơn nhiều!', NULL, '2025-02-27 15:30:00'),
(10, 6, 'Hỗ trợ tốt, thông tin đầy đủ!', 4, '2025-02-27 16:00:00');

-- 18. NOTIFICATIONS
INSERT INTO notifications (user_id, title, message, is_read, created_at) VALUES
-- Customer notifications
(5, 'Booking được phê duyệt', 'Booking HD-2025-0001 của bạn đã được phê duyệt. Vui lòng thanh toán tiền cọc để hoàn tất.', TRUE, '2025-03-11 10:00:00'),
(5, 'Thanh toán thành công', 'Thanh toán tiền cọc 3.000.000 VNĐ cho hợp đồng HD-2025-0001 thành công.', TRUE, '2025-03-11 14:30:00'),
(5, 'Nhắc nhở nhận xe', 'Bạn có lịch nhận xe vào ngày 12/03/2025 lúc 09:00. Vui lòng đến đúng giờ.', TRUE, '2025-03-11 18:00:00'),
(5, 'Ticket hỗ trợ mới', 'Yêu cầu hỗ trợ TK-2025-0001 của bạn đã được tiếp nhận.', FALSE, '2025-03-06 08:30:00'),

(6, 'Booking được phê duyệt', 'Booking HD-2025-0002 của bạn đã được phê duyệt.', TRUE, '2025-03-13 09:00:00'),
(6, 'Thanh toán thành công', 'Thanh toán tiền cọc 3.500.000 VNĐ cho hợp đồng HD-2025-0002 thành công.', TRUE, '2025-03-13 10:30:00'),
(6, 'Nhắc nhở trả xe', 'Hợp đồng HD-2025-0002 sẽ hết hạn vào ngày 17/03/2025 lúc 17:00. Vui lòng trả xe đúng hạn.', FALSE, '2025-03-16 08:00:00'),

(7, 'Booking được phê duyệt', 'Booking HD-2025-0003 của bạn đã được phê duyệt.', TRUE, '2025-03-16 14:00:00'),
(7, 'Thanh toán thành công', 'Thanh toán tiền cọc 7.800.000 VNĐ cho hợp đồng HD-2025-0003 thành công.', TRUE, '2025-03-16 16:00:00'),

(8, 'Hoàn tiền cọc', 'Tiền cọc 8.000.000 VNĐ cho hợp đồng HD-2025-0004 đã được hoàn vào tài khoản của bạn.', TRUE, '2025-02-20 10:00:00'),
(8, 'Ticket hỗ trợ đang xử lý', 'Yêu cầu hỗ trợ TK-2025-0005 của bạn đang được xử lý.', FALSE, '2025-03-05 17:00:00'),

(9, 'Phí phát sinh khi trả xe', 'Hợp đồng HD-2025-0005 có phí phát sinh 2.875.000 VNĐ (trễ hạn + hư hỏng). Số tiền đã được trừ vào cọc.', TRUE, '2025-02-16 11:00:00'),
(9, 'Hoàn tiền cọc', 'Tiền cọc còn lại 7.125.000 VNĐ cho hợp đồng HD-2025-0005 đã được hoàn.', TRUE, '2025-03-03 14:30:00'),

(10, 'Phí trả xe khác địa điểm', 'Hợp đồng HD-2025-0006 có phí trả xe khác địa điểm 1.200.000 VNĐ.', TRUE, '2025-02-21 10:30:00'),
(10, 'Tiền cọc chờ hoàn', 'Tiền cọc 9.800.000 VNĐ của bạn đang chờ xử lý hoàn tiền.', FALSE, '2025-03-06 10:15:00'),

(11, 'Vi phạm giao thông', 'Phát hiện vi phạm giao thông trong thời gian thuê xe HD-2025-0007. Tổng phạt: 2.300.000 VNĐ.', TRUE, '2025-02-05 09:00:00'),
(11, 'Đang kiểm tra vi phạm', 'Tiền cọc của bạn đang được giữ để kiểm tra vi phạm giao thông (14 ngày).', TRUE, '2025-01-25 19:00:00'),

-- Staff notifications
(2, 'Booking mới cần duyệt', 'Có 4 booking mới đang chờ phê duyệt.', FALSE, '2025-03-06 16:30:00'),
(2, 'Nhắc nhở bàn giao xe', 'Hợp đồng HD-2025-0001 có lịch bàn giao xe vào 12/03/2025 lúc 09:00.', TRUE, '2025-03-11 18:00:00'),
(2, 'Ticket hỗ trợ mới', 'Có 3 ticket hỗ trợ mới cần xử lý.', FALSE, '2025-03-06 10:00:00'),

(3, 'Xe cần bảo trì', 'Xe 30AB-11111 đang ở trạng thái Maintenance cần kiểm tra.', FALSE, '2025-03-05 09:00:00'),
(3, 'Nhắc nhở nhận xe trả', 'Hợp đồng HD-2025-0002 có lịch nhận xe trả vào 17/03/2025 lúc 17:00.', FALSE, '2025-03-16 08:00:00'),

-- Admin notifications
(1, 'Báo cáo doanh thu tuần', 'Doanh thu tuần này: 125.000.000 VNĐ. Tăng 15% so với tuần trước.', FALSE, '2025-03-04 09:00:00'),
(1, 'Xe cần kiểm tra', '2 xe đang trong trạng thái Maintenance cần xử lý.', FALSE, '2025-03-05 09:00:00'),
(1, 'Giấy tờ chờ duyệt', 'Có 2 bộ giấy tờ khách hàng đang chờ phê duyệt.', FALSE, '2025-03-06 08:00:00');

-- ===================================================================
-- ADDITIONAL SAMPLE DATA FOR TESTING REPORTS (August 2025 - March 2025)
-- ===================================================================

-- Additional completed bookings for past 8 months (for diverse reporting data)
INSERT INTO bookings (booking_id, customer_id, vehicle_id, pickup_location_id, return_location_id, start_date, end_date, total_days, status, created_at) VALUES
-- August 2025
(21, 5, 2, 1, 1, '2025-08-05 09:00:00', '2025-08-10 18:00:00', 5, 'Approved', '2025-08-03 10:00:00'),
(22, 6, 3, 2, 2, '2025-08-12 08:00:00', '2025-08-15 17:00:00', 3, 'Approved', '2025-08-10 14:00:00'),
(23, 7, 6, 3, 3, '2025-08-18 10:00:00', '2025-08-23 16:00:00', 5, 'Approved', '2025-08-16 09:00:00'),
(24, 8, 11, 4, 4, '2025-08-25 09:00:00', '2025-08-28 18:00:00', 3, 'Approved', '2025-08-23 11:00:00'),

-- September 2025
(25, 9, 4, 1, 1, '2025-09-02 09:00:00', '2025-09-07 18:00:00', 5, 'Approved', '2025-08-31 10:00:00'),
(26, 10, 7, 2, 2, '2025-09-10 08:00:00', '2025-09-13 17:00:00', 3, 'Approved', '2025-09-08 14:00:00'),
(27, 11, 13, 3, 4, '2025-09-15 10:00:00', '2025-09-20 16:00:00', 5, 'Approved', '2025-09-13 09:00:00'),
(28, 5, 17, 4, 4, '2025-09-22 09:00:00', '2025-09-26 18:00:00', 4, 'Approved', '2025-09-20 11:00:00'),
(29, 6, 19, 1, 1, '2025-09-28 08:00:00', '2025-10-01 17:00:00', 3, 'Approved', '2025-09-26 15:00:00'),

-- October 2025
(30, 7, 8, 2, 2, '2025-10-03 09:00:00', '2025-10-08 18:00:00', 5, 'Approved', '2025-10-01 10:00:00'),
(31, 8, 14, 3, 3, '2025-10-10 08:00:00', '2025-10-14 17:00:00', 4, 'Approved', '2025-10-08 14:00:00'),
(32, 9, 20, 4, 4, '2025-10-15 10:00:00', '2025-10-20 16:00:00', 5, 'Approved', '2025-10-13 09:00:00'),
(33, 10, 1, 1, 1, '2025-10-22 09:00:00', '2025-10-25 18:00:00', 3, 'Approved', '2025-10-20 11:00:00'),
(34, 11, 5, 2, 3, '2025-10-27 08:00:00', '2025-10-31 17:00:00', 4, 'Approved', '2025-10-25 15:00:00'),

-- November 2025
(35, 5, 12, 3, 3, '2025-11-02 09:00:00', '2025-11-07 18:00:00', 5, 'Approved', '2025-10-31 10:00:00'),
(36, 6, 18, 4, 4, '2025-11-09 08:00:00', '2025-11-12 17:00:00', 3, 'Approved', '2025-11-07 14:00:00'),
(37, 7, 21, 1, 1, '2025-11-14 10:00:00', '2025-11-19 16:00:00', 5, 'Approved', '2025-11-12 09:00:00'),
(38, 8, 3, 2, 2, '2025-11-21 09:00:00', '2025-11-25 18:00:00', 4, 'Approved', '2025-11-19 11:00:00'),
(39, 9, 9, 3, 3, '2025-11-26 08:00:00', '2025-11-29 17:00:00', 3, 'Approved', '2025-11-24 15:00:00'),

-- December 2025
(40, 10, 15, 4, 4, '2025-12-01 09:00:00', '2025-12-06 18:00:00', 5, 'Approved', '2025-11-29 10:00:00'),
(41, 11, 22, 1, 1, '2025-12-08 08:00:00', '2025-12-12 17:00:00', 4, 'Approved', '2025-12-06 14:00:00'),
(42, 5, 10, 2, 2, '2025-12-14 10:00:00', '2025-12-19 16:00:00', 5, 'Approved', '2025-12-12 09:00:00'),
(43, 6, 16, 3, 4, '2025-12-20 09:00:00', '2025-12-23 18:00:00', 3, 'Approved', '2025-12-18 11:00:00'),
(44, 7, 2, 4, 4, '2025-12-26 08:00:00', '2025-12-30 17:00:00', 4, 'Approved', '2025-12-24 15:00:00'),

-- January 2025 (additional)
(45, 8, 6, 1, 1, '2025-01-05 09:00:00', '2025-01-10 18:00:00', 5, 'Approved', '2025-01-03 10:00:00'),
(46, 9, 11, 2, 2, '2025-01-12 08:00:00', '2025-01-16 17:00:00', 4, 'Approved', '2025-01-10 14:00:00'),
(47, 10, 17, 3, 3, '2025-01-18 10:00:00', '2025-01-22 16:00:00', 4, 'Approved', '2025-01-16 09:00:00'),
(48, 11, 23, 4, 4, '2025-01-24 09:00:00', '2025-01-28 18:00:00', 4, 'Approved', '2025-01-22 11:00:00'),

-- February 2025 (additional)
(49, 5, 4, 1, 1, '2025-02-02 09:00:00', '2025-02-07 18:00:00', 5, 'Approved', '2025-01-31 10:00:00'),
(50, 6, 8, 2, 2, '2025-02-09 08:00:00', '2025-02-13 17:00:00', 4, 'Approved', '2025-02-07 14:00:00'),
(51, 7, 13, 3, 4, '2025-02-16 10:00:00', '2025-02-21 16:00:00', 5, 'Approved', '2025-02-14 09:00:00'),
(52, 8, 19, 4, 4, '2025-02-23 09:00:00', '2025-02-27 18:00:00', 4, 'Approved', '2025-02-21 11:00:00');

-- Additional bookings (Aug 2025 - Feb 2026)
INSERT INTO bookings (booking_id, customer_id, vehicle_id, pickup_location_id, return_location_id, start_date, end_date, total_days, status, created_at) VALUES
-- August 2025
(53, 5, 2, 1, 1, '2025-08-05 09:00:00', '2025-08-10 18:00:00', 5, 'Approved', '2025-08-03 10:00:00'),
(54, 6, 3, 2, 2, '2025-08-12 08:00:00', '2025-08-15 17:00:00', 3, 'Approved', '2025-08-10 14:00:00'),
(55, 7, 6, 3, 3, '2025-08-18 10:00:00', '2025-08-23 16:00:00', 5, 'Approved', '2025-08-16 09:00:00'),
(56, 8, 11, 4, 4, '2025-08-25 09:00:00', '2025-08-28 18:00:00', 3, 'Approved', '2025-08-23 11:00:00'),
-- September 2025
(57, 9, 4, 1, 1, '2025-09-02 09:00:00', '2025-09-07 18:00:00', 5, 'Approved', '2025-08-31 10:00:00'),
(58, 10, 7, 2, 2, '2025-09-10 08:00:00', '2025-09-13 17:00:00', 3, 'Approved', '2025-09-08 14:00:00'),
(59, 11, 13, 3, 3, '2025-09-15 10:00:00', '2025-09-20 16:00:00', 5, 'Approved', '2025-09-13 09:00:00'),
(60, 5, 17, 4, 4, '2025-09-22 09:00:00', '2025-09-26 18:00:00', 4, 'Approved', '2025-09-20 11:00:00'),
(61, 6, 19, 1, 1, '2025-09-28 08:00:00', '2025-10-01 17:00:00', 3, 'Approved', '2025-09-26 15:00:00'),
-- October 2025
(62, 7, 8, 2, 2, '2025-10-03 09:00:00', '2025-10-08 18:00:00', 5, 'Approved', '2025-10-01 10:00:00'),
(63, 8, 14, 3, 3, '2025-10-10 08:00:00', '2025-10-14 17:00:00', 4, 'Approved', '2025-10-08 14:00:00'),
(64, 9, 20, 4, 4, '2025-10-15 10:00:00', '2025-10-20 16:00:00', 5, 'Approved', '2025-10-13 09:00:00'),
(65, 10, 1, 1, 1, '2025-10-22 09:00:00', '2025-10-25 18:00:00', 3, 'Approved', '2025-10-20 11:00:00'),
(66, 11, 5, 2, 3, '2025-10-27 08:00:00', '2025-10-31 17:00:00', 4, 'Approved', '2025-10-25 15:00:00'),
-- November 2025
(67, 5, 12, 3, 3, '2025-11-02 09:00:00', '2025-11-07 18:00:00', 5, 'Approved', '2025-10-31 10:00:00'),
(68, 6, 18, 4, 4, '2025-11-09 08:00:00', '2025-11-12 17:00:00', 3, 'Approved', '2025-11-07 14:00:00'),
(69, 7, 21, 1, 1, '2025-11-14 10:00:00', '2025-11-19 16:00:00', 5, 'Approved', '2025-11-12 09:00:00'),
(70, 8, 3, 2, 2, '2025-11-21 09:00:00', '2025-11-25 18:00:00', 4, 'Approved', '2025-11-19 11:00:00'),
(71, 9, 9, 3, 3, '2025-11-26 08:00:00', '2025-11-29 17:00:00', 3, 'Approved', '2025-11-24 15:00:00'),
-- December 2025
(72, 10, 15, 4, 4, '2025-12-01 09:00:00', '2025-12-06 18:00:00', 5, 'Approved', '2025-11-29 10:00:00'),
(73, 11, 22, 1, 1, '2025-12-08 08:00:00', '2025-12-12 17:00:00', 4, 'Approved', '2025-12-06 14:00:00'),
(74, 5, 10, 2, 2, '2025-12-14 10:00:00', '2025-12-19 16:00:00', 5, 'Approved', '2025-12-12 09:00:00'),
(75, 6, 16, 3, 4, '2025-12-20 09:00:00', '2025-12-23 18:00:00', 3, 'Approved', '2025-12-18 11:00:00'),
(76, 7, 2, 4, 4, '2025-12-26 08:00:00', '2025-12-30 17:00:00', 4, 'Approved', '2025-12-24 15:00:00'),
-- January 2026 (additional)
(77, 8, 6, 1, 1, '2026-01-05 09:00:00', '2026-01-10 18:00:00', 5, 'Approved', '2026-01-03 10:00:00'),
(78, 9, 11, 2, 2, '2026-01-12 08:00:00', '2026-01-16 17:00:00', 4, 'Approved', '2026-01-10 14:00:00'),
(79, 10, 17, 3, 3, '2026-01-18 10:00:00', '2026-01-22 16:00:00', 4, 'Approved', '2026-01-16 09:00:00'),
(80, 11, 23, 4, 4, '2026-01-24 09:00:00', '2026-01-28 18:00:00', 4, 'Approved', '2026-01-22 11:00:00'),
-- February 2026 (additional)
(81, 5, 4, 1, 1, '2026-02-02 09:00:00', '2026-02-07 18:00:00', 5, 'Approved', '2026-01-31 10:00:00'),
(82, 6, 8, 2, 2, '2026-02-09 08:00:00', '2026-02-13 17:00:00', 4, 'Approved', '2026-02-07 14:00:00'),
(83, 7, 13, 3, 4, '2026-02-16 10:00:00', '2026-02-21 16:00:00', 5, 'Approved', '2026-02-14 09:00:00'),
(84, 8, 19, 4, 4, '2026-02-23 09:00:00', '2026-02-27 18:00:00', 4, 'Approved', '2026-02-21 11:00:00');

-- Add booking documents for new bookings
INSERT INTO booking_documents (booking_id, document_id) VALUES
(21, 1), (21, 2), (22, 3), (22, 4), (23, 5), (23, 6), (24, 7), (24, 8),
(25, 9), (25, 10), (26, 11), (26, 12), (27, 13), (27, 14), (28, 1), (28, 2),
(29, 3), (29, 4), (30, 5), (30, 6), (31, 7), (31, 8), (32, 9), (32, 10),
(33, 11), (33, 12), (34, 13), (34, 14), (35, 1), (35, 2), (36, 3), (36, 4),
(37, 5), (37, 6), (38, 7), (38, 8), (39, 9), (39, 10), (40, 11), (40, 12),
(41, 13), (41, 14), (42, 1), (42, 2), (43, 3), (43, 4), (44, 5), (44, 6),
(45, 7), (45, 8), (46, 9), (46, 10), (47, 11), (47, 12), (48, 13), (48, 14),
(49, 1), (49, 2), (50, 3), (50, 4), (51, 5), (51, 6), (52, 7), (52, 8);

-- Booking documents for additional bookings (53-84)
INSERT INTO booking_documents (booking_id, document_id) VALUES
(53, 1), (53, 2), (54, 3), (54, 4), (55, 5), (55, 6), (56, 7), (56, 8),
(57, 9), (57, 10), (58, 11), (58, 12), (59, 13), (59, 14), (60, 1), (60, 2),
(61, 3), (61, 4), (62, 5), (62, 6), (63, 7), (63, 8), (64, 9), (64, 10),
(65, 11), (65, 12), (66, 13), (66, 14), (67, 1), (67, 2), (68, 3), (68, 4),
(69, 5), (69, 6), (70, 7), (70, 8), (71, 9), (71, 10), (72, 11), (72, 12),
(73, 13), (73, 14), (74, 1), (74, 2), (75, 3), (75, 4), (76, 5), (76, 6),
(77, 7), (77, 8), (78, 9), (78, 10), (79, 11), (79, 12), (80, 13), (80, 14),
(81, 1), (81, 2), (82, 3), (82, 4), (83, 5), (83, 6), (84, 7), (84, 8);

-- Additional contracts (all completed, spanning Aug 2025 - Feb 2025)
INSERT INTO contracts (contract_id, booking_id, contract_number, customer_id, vehicle_id, staff_id, start_date, end_date, total_days, daily_rate, total_rental_fee, deposit_amount, status, created_at) VALUES
-- August 2025
(15, 21, 'HD-2025-0055', 5, 2, 2, '2025-08-05 09:00:00', '2025-08-10 18:00:00', 5, 550000, 2750000, 50000000, 'Completed', '2025-08-04 10:00:00'),
(16, 22, 'HD-2025-0056', 6, 3, 3, '2025-08-12 08:00:00', '2025-08-15 17:00:00', 3, 500000, 1500000, 50000000, 'Completed', '2025-08-11 14:00:00'),
(17, 23, 'HD-2025-0057', 7, 6, 2, '2025-08-18 10:00:00', '2025-08-23 16:00:00', 5, 750000, 3750000, 50000000, 'Completed', '2025-08-17 09:00:00'),
(18, 24, 'HD-2025-0058', 8, 11, 3, '2025-08-25 09:00:00', '2025-08-28 18:00:00', 3, 800000, 2400000, 50000000, 'Completed', '2025-08-24 11:00:00'),

-- September 2025
(19, 25, 'HD-2025-0059', 9, 4, 2, '2025-09-02 09:00:00', '2025-09-07 18:00:00', 5, 600000, 3000000, 50000000, 'Completed', '2025-09-01 10:00:00'),
(20, 26, 'HD-2025-0060', 10, 7, 3, '2025-09-10 08:00:00', '2025-09-13 17:00:00', 3, 700000, 2100000, 50000000, 'Completed', '2025-09-09 14:00:00'),
(21, 27, 'HD-2025-0061', 11, 13, 2, '2025-09-15 10:00:00', '2025-09-20 16:00:00', 5, 1300000, 6500000, 50000000, 'Completed', '2025-09-14 09:00:00'),
(22, 28, 'HD-2025-0062', 5, 17, 3, '2025-09-22 09:00:00', '2025-09-26 18:00:00', 4, 450000, 1800000, 50000000, 'Completed', '2025-09-21 11:00:00'),
(23, 29, 'HD-2025-0063', 6, 19, 2, '2025-09-28 08:00:00', '2025-10-01 17:00:00', 3, 650000, 1950000, 50000000, 'Completed', '2025-09-27 15:00:00'),

-- October 2025
(24, 30, 'HD-2025-0064', 7, 8, 3, '2025-10-03 09:00:00', '2025-10-08 18:00:00', 5, 800000, 4000000, 50000000, 'Completed', '2025-10-02 10:00:00'),
(25, 31, 'HD-2025-0065', 8, 14, 2, '2025-10-10 08:00:00', '2025-10-14 17:00:00', 4, 1500000, 6000000, 50000000, 'Completed', '2025-10-09 14:00:00'),
(26, 32, 'HD-2025-0066', 9, 20, 3, '2025-10-15 10:00:00', '2025-10-20 16:00:00', 5, 700000, 3500000, 50000000, 'Completed', '2025-10-14 09:00:00'),
(27, 33, 'HD-2025-0067', 10, 1, 2, '2025-10-22 09:00:00', '2025-10-25 18:00:00', 3, 500000, 1500000, 50000000, 'Completed', '2025-10-21 11:00:00'),
(28, 34, 'HD-2025-0068', 11, 5, 3, '2025-10-27 08:00:00', '2025-10-31 17:00:00', 4, 550000, 2200000, 50000000, 'Completed', '2025-10-26 15:00:00'),

-- November 2025
(29, 35, 'HD-2025-0069', 5, 12, 2, '2025-11-02 09:00:00', '2025-11-07 18:00:00', 5, 600000, 3000000, 50000000, 'Completed', '2025-11-01 10:00:00'),
(30, 36, 'HD-2025-0070', 6, 18, 3, '2025-11-09 08:00:00', '2025-11-12 17:00:00', 3, 500000, 1500000, 50000000, 'Completed', '2025-11-08 14:00:00'),
(31, 37, 'HD-2025-0071', 7, 21, 2, '2025-11-14 10:00:00', '2025-11-19 16:00:00', 5, 500000, 2500000, 50000000, 'Completed', '2025-11-13 09:00:00'),
(32, 38, 'HD-2025-0072', 8, 3, 3, '2025-11-21 09:00:00', '2025-11-25 18:00:00', 4, 500000, 2000000, 50000000, 'Completed', '2025-11-20 11:00:00'),
(33, 39, 'HD-2025-0073', 9, 9, 2, '2025-11-26 08:00:00', '2025-11-29 17:00:00', 3, 1200000, 3600000, 50000000, 'Completed', '2025-11-25 15:00:00'),

-- December 2025
(34, 40, 'HD-2025-0074', 10, 15, 3, '2025-12-01 09:00:00', '2025-12-06 18:00:00', 5, 1600000, 8000000, 50000000, 'Completed', '2025-11-30 10:00:00'),
(35, 41, 'HD-2025-0075', 11, 22, 2, '2025-12-08 08:00:00', '2025-12-12 17:00:00', 4, 550000, 2200000, 50000000, 'Completed', '2025-12-07 14:00:00'),
(36, 42, 'HD-2025-0076', 5, 10, 3, '2025-12-14 10:00:00', '2025-12-19 16:00:00', 5, 1100000, 5500000, 50000000, 'Completed', '2025-12-13 09:00:00'),
(37, 43, 'HD-2025-0077', 6, 16, 2, '2025-12-20 09:00:00', '2025-12-23 18:00:00', 3, 400000, 1200000, 50000000, 'Completed', '2025-12-19 11:00:00'),
(38, 44, 'HD-2025-0078', 7, 2, 3, '2025-12-26 08:00:00', '2025-12-30 17:00:00', 4, 550000, 2200000, 50000000, 'Completed', '2025-12-25 15:00:00'),

-- January 2025 (additional)
(39, 45, 'HD-2025-0015', 8, 6, 2, '2025-01-05 09:00:00', '2025-01-10 18:00:00', 5, 750000, 3750000, 50000000, 'Completed', '2025-01-04 10:00:00'),
(40, 46, 'HD-2025-0016', 9, 11, 3, '2025-01-12 08:00:00', '2025-01-16 17:00:00', 4, 800000, 3200000, 50000000, 'Completed', '2025-01-11 14:00:00'),
(41, 47, 'HD-2025-0017', 10, 17, 2, '2025-01-18 10:00:00', '2025-01-22 16:00:00', 4, 450000, 1800000, 50000000, 'Completed', '2025-01-17 09:00:00'),
(42, 48, 'HD-2025-0018', 11, 23, 3, '2025-01-24 09:00:00', '2025-01-28 18:00:00', 4, 1150000, 4600000, 50000000, 'Completed', '2025-01-23 11:00:00'),

-- February 2025 (additional)
(43, 49, 'HD-2025-0019', 5, 4, 2, '2025-02-02 09:00:00', '2025-02-07 18:00:00', 5, 600000, 3000000, 50000000, 'Completed', '2025-02-01 10:00:00'),
(44, 50, 'HD-2025-0020', 6, 8, 3, '2025-02-09 08:00:00', '2025-02-13 17:00:00', 4, 800000, 3200000, 50000000, 'Completed', '2025-02-08 14:00:00'),
(45, 51, 'HD-2025-0021', 7, 13, 2, '2025-02-16 10:00:00', '2025-02-21 16:00:00', 5, 1300000, 6500000, 50000000, 'Completed', '2025-02-15 09:00:00'),
(46, 52, 'HD-2025-0022', 8, 19, 3, '2025-02-23 09:00:00', '2025-02-27 18:00:00', 4, 650000, 2600000, 50000000, 'Completed', '2025-02-22 11:00:00');

-- Additional contracts (completed, spanning Aug 2025 - Feb 2026)
INSERT INTO contracts (contract_id, booking_id, contract_number, customer_id, vehicle_id, staff_id, start_date, end_date, total_days, daily_rate, total_rental_fee, deposit_amount, status, created_at) VALUES
-- August 2025
(47, 53, 'HD-2025-0023', 5, 2, 2, '2025-08-05 09:00:00', '2025-08-10 18:00:00', 5, 550000, 2750000, 50000000, 'Completed', '2025-08-04 10:00:00'),
(48, 54, 'HD-2025-0024', 6, 3, 3, '2025-08-12 08:00:00', '2025-08-15 17:00:00', 3, 500000, 1500000, 50000000, 'Completed', '2025-08-11 14:00:00'),
(49, 55, 'HD-2025-0025', 7, 6, 2, '2025-08-18 10:00:00', '2025-08-23 16:00:00', 5, 750000, 3750000, 50000000, 'Completed', '2025-08-17 09:00:00'),
(50, 56, 'HD-2025-0026', 8, 11, 3, '2025-08-25 09:00:00', '2025-08-28 18:00:00', 3, 800000, 2400000, 50000000, 'Completed', '2025-08-24 11:00:00'),
-- September 2025
(51, 57, 'HD-2025-0027', 9, 4, 2, '2025-09-02 09:00:00', '2025-09-07 18:00:00', 5, 600000, 3000000, 50000000, 'Completed', '2025-09-01 10:00:00'),
(52, 58, 'HD-2025-0028', 10, 7, 3, '2025-09-10 08:00:00', '2025-09-13 17:00:00', 3, 700000, 2100000, 50000000, 'Completed', '2025-09-09 14:00:00'),
(53, 59, 'HD-2025-0029', 11, 13, 2, '2025-09-15 10:00:00', '2025-09-20 16:00:00', 5, 1300000, 6500000, 50000000, 'Completed', '2025-09-14 09:00:00'),
(54, 60, 'HD-2025-0030', 5, 17, 3, '2025-09-22 09:00:00', '2025-09-26 18:00:00', 4, 450000, 1800000, 50000000, 'Completed', '2025-09-21 11:00:00'),
(55, 61, 'HD-2025-0031', 6, 19, 2, '2025-09-28 08:00:00', '2025-10-01 17:00:00', 3, 650000, 1950000, 50000000, 'Completed', '2025-09-27 15:00:00'),
-- October 2025
(56, 62, 'HD-2025-0032', 7, 8, 3, '2025-10-03 09:00:00', '2025-10-08 18:00:00', 5, 800000, 4000000, 50000000, 'Completed', '2025-10-02 10:00:00'),
(57, 63, 'HD-2025-0033', 8, 14, 2, '2025-10-10 08:00:00', '2025-10-14 17:00:00', 4, 1500000, 6000000, 50000000, 'Completed', '2025-10-09 14:00:00'),
(58, 64, 'HD-2025-0034', 9, 20, 3, '2025-10-15 10:00:00', '2025-10-20 16:00:00', 5, 700000, 3500000, 50000000, 'Completed', '2025-10-14 09:00:00'),
(59, 65, 'HD-2025-0035', 10, 1, 2, '2025-10-22 09:00:00', '2025-10-25 18:00:00', 3, 500000, 1500000, 50000000, 'Completed', '2025-10-21 11:00:00'),
(60, 66, 'HD-2025-0036', 11, 5, 3, '2025-10-27 08:00:00', '2025-10-31 17:00:00', 4, 550000, 2200000, 50000000, 'Completed', '2025-10-26 15:00:00'),
-- November 2025
(61, 67, 'HD-2025-0037', 5, 12, 2, '2025-11-02 09:00:00', '2025-11-07 18:00:00', 5, 600000, 3000000, 50000000, 'Completed', '2025-11-01 10:00:00'),
(62, 68, 'HD-2025-0038', 6, 18, 3, '2025-11-09 08:00:00', '2025-11-12 17:00:00', 3, 500000, 1500000, 50000000, 'Completed', '2025-11-08 14:00:00'),
(63, 69, 'HD-2025-0039', 7, 21, 2, '2025-11-14 10:00:00', '2025-11-19 16:00:00', 5, 500000, 2500000, 50000000, 'Completed', '2025-11-13 09:00:00'),
(64, 70, 'HD-2025-0040', 8, 3, 3, '2025-11-21 09:00:00', '2025-11-25 18:00:00', 4, 500000, 2000000, 50000000, 'Completed', '2025-11-20 11:00:00'),
(65, 71, 'HD-2025-0041', 9, 9, 2, '2025-11-26 08:00:00', '2025-11-29 17:00:00', 3, 1200000, 3600000, 50000000, 'Completed', '2025-11-25 15:00:00'),
-- December 2025
(66, 72, 'HD-2025-0042', 10, 15, 3, '2025-12-01 09:00:00', '2025-12-06 18:00:00', 5, 1600000, 8000000, 50000000, 'Completed', '2025-11-30 10:00:00'),
(67, 73, 'HD-2025-0043', 11, 22, 2, '2025-12-08 08:00:00', '2025-12-12 17:00:00', 4, 550000, 2200000, 50000000, 'Completed', '2025-12-07 14:00:00'),
(68, 74, 'HD-2025-0044', 5, 10, 3, '2025-12-14 10:00:00', '2025-12-19 16:00:00', 5, 1100000, 5500000, 50000000, 'Completed', '2025-12-13 09:00:00'),
(69, 75, 'HD-2025-0045', 6, 16, 2, '2025-12-20 09:00:00', '2025-12-23 18:00:00', 3, 400000, 1200000, 50000000, 'Completed', '2025-12-19 11:00:00'),
(70, 76, 'HD-2025-0046', 7, 2, 3, '2025-12-26 08:00:00', '2025-12-30 17:00:00', 4, 550000, 2200000, 50000000, 'Completed', '2025-12-25 15:00:00'),
-- January 2026 (additional)
(71, 77, 'HD-2025-0047', 8, 6, 2, '2026-01-05 09:00:00', '2026-01-10 18:00:00', 5, 750000, 3750000, 50000000, 'Completed', '2026-01-04 10:00:00'),
(72, 78, 'HD-2025-0048', 9, 11, 3, '2026-01-12 08:00:00', '2026-01-16 17:00:00', 4, 800000, 3200000, 50000000, 'Completed', '2026-01-11 14:00:00'),
(73, 79, 'HD-2025-0049', 10, 17, 2, '2026-01-18 10:00:00', '2026-01-22 16:00:00', 4, 450000, 1800000, 50000000, 'Completed', '2026-01-17 09:00:00'),
(74, 80, 'HD-2025-0050', 11, 23, 3, '2026-01-24 09:00:00', '2026-01-28 18:00:00', 4, 1150000, 4600000, 50000000, 'Completed', '2026-01-23 11:00:00'),
-- February 2026 (additional)
(75, 81, 'HD-2025-0051', 5, 4, 2, '2026-02-02 09:00:00', '2026-02-07 18:00:00', 5, 600000, 3000000, 50000000, 'Completed', '2026-02-01 10:00:00'),
(76, 82, 'HD-2025-0052', 6, 8, 3, '2026-02-09 08:00:00', '2026-02-13 17:00:00', 4, 800000, 3200000, 50000000, 'Completed', '2026-02-08 14:00:00'),
(77, 83, 'HD-2025-0053', 7, 13, 2, '2026-02-16 10:00:00', '2026-02-21 16:00:00', 5, 1300000, 6500000, 50000000, 'Completed', '2026-02-15 09:00:00'),
(78, 84, 'HD-2025-0054', 8, 19, 3, '2026-02-23 09:00:00', '2026-02-27 18:00:00', 4, 650000, 2600000, 50000000, 'Completed', '2026-02-22 11:00:00');
-- Additional payments with diverse methods (CASH, CARD, TRANSFER, ONLINE)
-- Note: Bill fields are NULL for these old sample RENTAL payments
INSERT INTO payments (contract_id, payment_type, amount, method, status, payment_date, created_at,
                     transaction_ref, gateway_transaction_id, gateway_response_code, gateway_transaction_status,
                     gateway_bank_code, gateway_card_type, gateway_pay_date, gateway_secure_hash, payment_url,
                     bill_number, original_rental_fee, rental_adjustment, actual_rental_fee,
                     late_fee, damage_fee, one_way_fee, total_additional_fees, deposit_amount, amount_paid, amount_due, notes) VALUES
-- August 2025 - Mix of payment methods
(15, 'DEPOSIT', 50000000, 'TRANSFER', 'COMPLETED', '2025-08-04 10:30:00', '2025-08-04 10:30:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(15, 'RENTAL', 2750000, 'CASH', 'COMPLETED', '2025-08-10 19:00:00', '2025-08-10 19:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(16, 'DEPOSIT', 50000000, 'CARD', 'COMPLETED', '2025-08-11 14:30:00', '2025-08-11 14:30:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(16, 'RENTAL', 1500000, 'CARD', 'COMPLETED', '2025-08-15 18:00:00', '2025-08-15 18:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(17, 'DEPOSIT', 50000000, 'ONLINE', 'COMPLETED', '2025-08-17 09:30:00', '2025-08-17 09:30:00', 'DEP17_AUG2025', 'TXN_AUG_001', '00', 'SUCCESS', 'TECHCOMBANK', 'VISA', '20250817093000', 'hash_aug_001', NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(17, 'RENTAL', 3750000, 'TRANSFER', 'COMPLETED', '2025-08-23 17:00:00', '2025-08-23 17:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(18, 'DEPOSIT', 50000000, 'TRANSFER', 'COMPLETED', '2025-08-24 11:30:00', '2025-08-24 11:30:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(18, 'RENTAL', 2400000, 'CASH', 'COMPLETED', '2025-08-28 19:00:00', '2025-08-28 19:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),

-- September 2025
(19, 'DEPOSIT', 50000000, 'CARD', 'COMPLETED', '2025-09-01 10:30:00', '2025-09-01 10:30:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(19, 'RENTAL', 3000000, 'CARD', 'COMPLETED', '2025-09-07 19:00:00', '2025-09-07 19:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(20, 'DEPOSIT', 50000000, 'TRANSFER', 'COMPLETED', '2025-09-09 14:30:00', '2025-09-09 14:30:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(20, 'RENTAL', 2100000, 'TRANSFER', 'COMPLETED', '2025-09-13 18:00:00', '2025-09-13 18:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(21, 'DEPOSIT', 50000000, 'ONLINE', 'COMPLETED', '2025-09-14 09:30:00', '2025-09-14 09:30:00', 'DEP21_SEP2025', 'TXN_SEP_001', '00', 'SUCCESS', 'VIETCOMBANK', 'MASTERCARD', '20250914093000', 'hash_sep_001', NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(21, 'RENTAL', 6500000, 'CASH', 'COMPLETED', '2025-09-20 17:00:00', '2025-09-20 17:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(22, 'DEPOSIT', 50000000, 'CASH', 'COMPLETED', '2025-09-21 11:30:00', '2025-09-21 11:30:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(22, 'RENTAL', 1800000, 'CASH', 'COMPLETED', '2025-09-26 19:00:00', '2025-09-26 19:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(23, 'DEPOSIT', 50000000, 'TRANSFER', 'COMPLETED', '2025-09-27 15:30:00', '2025-09-27 15:30:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(23, 'RENTAL', 1950000, 'TRANSFER', 'COMPLETED', '2025-10-01 18:00:00', '2025-10-01 18:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),

-- October 2025
(24, 'DEPOSIT', 50000000, 'CARD', 'COMPLETED', '2025-10-02 10:30:00', '2025-10-02 10:30:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(24, 'RENTAL', 4000000, 'CARD', 'COMPLETED', '2025-10-08 19:00:00', '2025-10-08 19:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(25, 'DEPOSIT', 50000000, 'ONLINE', 'COMPLETED', '2025-10-09 14:30:00', '2025-10-09 14:30:00', 'DEP25_OCT2025', 'TXN_OCT_001', '00', 'SUCCESS', 'BIDV', 'VISA', '20251009143000', 'hash_oct_001', NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(25, 'RENTAL', 6000000, 'TRANSFER', 'COMPLETED', '2025-10-14 18:00:00', '2025-10-14 18:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(26, 'DEPOSIT', 50000000, 'TRANSFER', 'COMPLETED', '2025-10-14 09:30:00', '2025-10-14 09:30:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(26, 'RENTAL', 3500000, 'CASH', 'COMPLETED', '2025-10-20 17:00:00', '2025-10-20 17:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(27, 'DEPOSIT', 50000000, 'CASH', 'COMPLETED', '2025-10-21 11:30:00', '2025-10-21 11:30:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(27, 'RENTAL', 1500000, 'CASH', 'COMPLETED', '2025-10-25 19:00:00', '2025-10-25 19:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(28, 'DEPOSIT', 50000000, 'CARD', 'COMPLETED', '2025-10-26 15:30:00', '2025-10-26 15:30:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(28, 'RENTAL', 2200000, 'CARD', 'COMPLETED', '2025-10-31 18:00:00', '2025-10-31 18:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),

-- November 2025
(29, 'DEPOSIT', 50000000, 'TRANSFER', 'COMPLETED', '2025-11-01 10:30:00', '2025-11-01 10:30:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(29, 'RENTAL', 3000000, 'TRANSFER', 'COMPLETED', '2025-11-07 19:00:00', '2025-11-07 19:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(30, 'DEPOSIT', 50000000, 'ONLINE', 'COMPLETED', '2025-11-08 14:30:00', '2025-11-08 14:30:00', 'DEP30_NOV2025', 'TXN_NOV_001', '00', 'SUCCESS', 'ACB', 'MASTERCARD', '20251108143000', 'hash_nov_001', NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(30, 'RENTAL', 1500000, 'CARD', 'COMPLETED', '2025-11-12 18:00:00', '2025-11-12 18:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(31, 'DEPOSIT', 50000000, 'CARD', 'COMPLETED', '2025-11-13 09:30:00', '2025-11-13 09:30:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(31, 'RENTAL', 2500000, 'CASH', 'COMPLETED', '2025-11-19 17:00:00', '2025-11-19 17:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(32, 'DEPOSIT', 50000000, 'TRANSFER', 'COMPLETED', '2025-11-20 11:30:00', '2025-11-20 11:30:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(32, 'RENTAL', 2000000, 'TRANSFER', 'COMPLETED', '2025-11-25 19:00:00', '2025-11-25 19:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(33, 'DEPOSIT', 50000000, 'CASH', 'COMPLETED', '2025-11-25 15:30:00', '2025-11-25 15:30:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(33, 'RENTAL', 3600000, 'CASH', 'COMPLETED', '2025-11-29 18:00:00', '2025-11-29 18:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),

-- December 2025
(34, 'DEPOSIT', 50000000, 'ONLINE', 'COMPLETED', '2025-11-30 10:30:00', '2025-11-30 10:30:00', 'DEP34_DEC2025', 'TXN_DEC_001', '00', 'SUCCESS', 'MBBANK', 'VISA', '20251130103000', 'hash_dec_001', NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(34, 'RENTAL', 8000000, 'TRANSFER', 'COMPLETED', '2025-12-06 19:00:00', '2025-12-06 19:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(35, 'DEPOSIT', 50000000, 'CARD', 'COMPLETED', '2025-12-07 14:30:00', '2025-12-07 14:30:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(35, 'RENTAL', 2200000, 'CARD', 'COMPLETED', '2025-12-12 18:00:00', '2025-12-12 18:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(36, 'DEPOSIT', 50000000, 'TRANSFER', 'COMPLETED', '2025-12-13 09:30:00', '2025-12-13 09:30:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(36, 'RENTAL', 5500000, 'CASH', 'COMPLETED', '2025-12-19 17:00:00', '2025-12-19 17:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(37, 'DEPOSIT', 50000000, 'CASH', 'COMPLETED', '2025-12-19 11:30:00', '2025-12-19 11:30:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(37, 'RENTAL', 1200000, 'CASH', 'COMPLETED', '2025-12-23 19:00:00', '2025-12-23 19:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(38, 'DEPOSIT', 50000000, 'ONLINE', 'COMPLETED', '2025-12-25 15:30:00', '2025-12-25 15:30:00', 'DEP38_DEC2025', 'TXN_DEC_002', '00', 'SUCCESS', 'VIETINBANK', 'MASTERCARD', '20251225153000', 'hash_dec_002', NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(38, 'RENTAL', 2200000, 'CARD', 'COMPLETED', '2025-12-30 18:00:00', '2025-12-30 18:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),

-- January 2025 (additional)
(39, 'DEPOSIT', 50000000, 'TRANSFER', 'COMPLETED', '2025-01-04 10:30:00', '2025-01-04 10:30:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(39, 'RENTAL', 3750000, 'TRANSFER', 'COMPLETED', '2025-01-10 19:00:00', '2025-01-10 19:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(40, 'DEPOSIT', 50000000, 'CARD', 'COMPLETED', '2025-01-11 14:30:00', '2025-01-11 14:30:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(40, 'RENTAL', 3200000, 'CARD', 'COMPLETED', '2025-01-16 18:00:00', '2025-01-16 18:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(41, 'DEPOSIT', 50000000, 'ONLINE', 'COMPLETED', '2025-01-17 09:30:00', '2025-01-17 09:30:00', 'DEP41_JAN2025', 'TXN_JAN_001', '00', 'SUCCESS', 'SACOMBANK', 'VISA', '20250117093000', 'hash_jan_001', NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(41, 'RENTAL', 1800000, 'CASH', 'COMPLETED', '2025-01-22 17:00:00', '2025-01-22 17:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(42, 'DEPOSIT', 50000000, 'CASH', 'COMPLETED', '2025-01-23 11:30:00', '2025-01-23 11:30:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(42, 'RENTAL', 4600000, 'CASH', 'COMPLETED', '2025-01-28 19:00:00', '2025-01-28 19:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),

-- February 2025 (additional)
(43, 'DEPOSIT', 50000000, 'TRANSFER', 'COMPLETED', '2025-02-01 10:30:00', '2025-02-01 10:30:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(43, 'RENTAL', 3000000, 'TRANSFER', 'COMPLETED', '2025-02-07 19:00:00', '2025-02-07 19:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(44, 'DEPOSIT', 50000000, 'ONLINE', 'COMPLETED', '2025-02-08 14:30:00', '2025-02-08 14:30:00', 'DEP44_FEB2025', 'TXN_FEB_001', '00', 'SUCCESS', 'DONGABANK', 'MASTERCARD', '20250208143000', 'hash_feb_001', NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(44, 'RENTAL', 3200000, 'CARD', 'COMPLETED', '2025-02-13 18:00:00', '2025-02-13 18:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(45, 'DEPOSIT', 50000000, 'CARD', 'COMPLETED', '2025-02-15 09:30:00', '2025-02-15 09:30:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(45, 'RENTAL', 6500000, 'CASH', 'COMPLETED', '2025-02-21 17:00:00', '2025-02-21 17:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(46, 'DEPOSIT', 50000000, 'TRANSFER', 'COMPLETED', '2025-02-22 11:30:00', '2025-02-22 11:30:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
(46, 'RENTAL', 2600000, 'TRANSFER', 'COMPLETED', '2025-02-27 19:00:00', '2025-02-27 19:00:00', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
 NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);

-- Additional handovers (pickup and return) for completed contracts
INSERT INTO handovers (contract_id, handover_type, staff_id, handover_time, odometer, fuel_level, condition_notes, images, created_at) VALUES
-- August 2025
(15, 'PICKUP', 2, '2025-08-05 09:30:00', 35000, 100, 'Xe tốt, đầy xăng', '["https://example.com/pickup/h15-1.jpg"]', '2025-08-05 09:30:00'),
(15, 'RETURN', 2, '2025-08-10 18:30:00', 36100, 85, 'Xe trả đúng hạn, tình trạng tốt', '["https://example.com/return/h15-1.jpg"]', '2025-08-10 18:30:00'),
(16, 'PICKUP', 3, '2025-08-12 08:30:00', 28000, 100, 'Xe sạch sẽ', '["https://example.com/pickup/h16-1.jpg"]', '2025-08-12 08:30:00'),
(16, 'RETURN', 3, '2025-08-15 17:30:00', 28600, 90, 'Trả đúng hạn', '["https://example.com/return/h16-1.jpg"]', '2025-08-15 17:30:00'),
(17, 'PICKUP', 2, '2025-08-18 10:30:00', 42000, 100, 'Xe hoàn hảo', '["https://example.com/pickup/h17-1.jpg"]', '2025-08-18 10:30:00'),
(17, 'RETURN', 2, '2025-08-23 16:30:00', 43150, 95, 'Tình trạng tốt', '["https://example.com/return/h17-1.jpg"]', '2025-08-23 16:30:00'),
(18, 'PICKUP', 3, '2025-08-25 09:30:00', 51000, 100, 'Xe tốt', '["https://example.com/pickup/h18-1.jpg"]', '2025-08-25 09:30:00'),
(18, 'RETURN', 3, '2025-08-28 18:30:00', 51650, 80, 'Đúng hạn', '["https://example.com/return/h18-1.jpg"]', '2025-08-28 18:30:00'),

-- September 2025
(19, 'PICKUP', 2, '2025-09-02 09:30:00', 39000, 100, 'Xe sạch', '["https://example.com/pickup/h19-1.jpg"]', '2025-09-02 09:30:00'),
(19, 'RETURN', 2, '2025-09-07 18:30:00', 40200, 90, 'Tốt', '["https://example.com/return/h19-1.jpg"]', '2025-09-07 18:30:00'),
(20, 'PICKUP', 3, '2025-09-10 08:30:00', 47000, 100, 'Hoàn hảo', '["https://example.com/pickup/h20-1.jpg"]', '2025-09-10 08:30:00'),
(20, 'RETURN', 3, '2025-09-13 17:30:00', 47700, 85, 'OK', '["https://example.com/return/h20-1.jpg"]', '2025-09-13 17:30:00'),
(21, 'PICKUP', 2, '2025-09-15 10:30:00', 66000, 100, 'Tốt', '["https://example.com/pickup/h21-1.jpg"]', '2025-09-15 10:30:00'),
(21, 'RETURN', 2, '2025-09-20 16:30:00', 67300, 75, 'Trả đúng', '["https://example.com/return/h21-1.jpg"]', '2025-09-20 16:30:00'),
(22, 'PICKUP', 3, '2025-09-22 09:30:00', 23000, 100, 'Xe mới', '["https://example.com/pickup/h22-1.jpg"]', '2025-09-22 09:30:00'),
(22, 'RETURN', 3, '2025-09-26 18:30:00', 23850, 95, 'Tốt', '["https://example.com/return/h22-1.jpg"]', '2025-09-26 18:30:00'),
(23, 'PICKUP', 2, '2025-09-28 08:30:00', 44000, 100, 'Sạch', '["https://example.com/pickup/h23-1.jpg"]', '2025-09-28 08:30:00'),
(23, 'RETURN', 2, '2025-10-01 17:30:00', 44650, 88, 'OK', '["https://example.com/return/h23-1.jpg"]', '2025-10-01 17:30:00'),

-- October 2025
(24, 'PICKUP', 3, '2025-10-03 09:30:00', 52000, 100, 'Tốt', '["https://example.com/pickup/h24-1.jpg"]', '2025-10-03 09:30:00'),
(24, 'RETURN', 3, '2025-10-08 18:30:00', 53100, 82, 'Hoàn hảo', '["https://example.com/return/h24-1.jpg"]', '2025-10-08 18:30:00'),
(25, 'PICKUP', 2, '2025-10-10 08:30:00', 79000, 100, 'Xe đẹp', '["https://example.com/pickup/h25-1.jpg"]', '2025-10-10 08:30:00'),
(25, 'RETURN', 2, '2025-10-14 17:30:00', 79950, 90, 'Tốt', '["https://example.com/return/h25-1.jpg"]', '2025-10-14 17:30:00'),
(26, 'PICKUP', 3, '2025-10-15 10:30:00', 46000, 100, 'OK', '["https://example.com/pickup/h26-1.jpg"]', '2025-10-15 10:30:00'),
(26, 'RETURN', 3, '2025-10-20 16:30:00', 47250, 85, 'Tốt', '["https://example.com/return/h26-1.jpg"]', '2025-10-20 16:30:00'),
(27, 'PICKUP', 2, '2025-10-22 09:30:00', 46000, 100, 'Sạch', '["https://example.com/pickup/h27-1.jpg"]', '2025-10-22 09:30:00'),
(27, 'RETURN', 2, '2025-10-25 18:30:00', 46750, 92, 'OK', '["https://example.com/return/h27-1.jpg"]', '2025-10-25 18:30:00'),
(28, 'PICKUP', 3, '2025-10-27 08:30:00', 39000, 100, 'Tốt', '["https://example.com/pickup/h28-1.jpg"]', '2025-10-27 08:30:00'),
(28, 'RETURN', 3, '2025-10-31 17:30:00', 39950, 88, 'Hoàn hảo', '["https://example.com/return/h28-1.jpg"]', '2025-10-31 17:30:00'),

-- November 2025
(29, 'PICKUP', 2, '2025-11-02 09:30:00', 32000, 100, 'Xe tốt', '["https://example.com/pickup/h29-1.jpg"]', '2025-11-02 09:30:00'),
(29, 'RETURN', 2, '2025-11-07 18:30:00', 33200, 87, 'OK', '["https://example.com/return/h29-1.jpg"]', '2025-11-07 18:30:00'),
(30, 'PICKUP', 3, '2025-11-09 08:30:00', 29000, 100, 'Sạch', '["https://example.com/pickup/h30-1.jpg"]', '2025-11-09 08:30:00'),
(30, 'RETURN', 3, '2025-11-12 17:30:00', 29700, 90, 'Tốt', '["https://example.com/return/h30-1.jpg"]', '2025-11-12 17:30:00'),
(31, 'PICKUP', 2, '2025-11-14 10:30:00', 45000, 100, 'Đẹp', '["https://example.com/pickup/h31-1.jpg"]', '2025-11-14 10:30:00'),
(31, 'RETURN', 2, '2025-11-19 16:30:00', 46200, 85, 'OK', '["https://example.com/return/h31-1.jpg"]', '2025-11-19 16:30:00'),
(32, 'PICKUP', 3, '2025-11-21 09:30:00', 29000, 100, 'Tốt', '["https://example.com/pickup/h32-1.jpg"]', '2025-11-21 09:30:00'),
(32, 'RETURN', 3, '2025-11-25 18:30:00', 29950, 88, 'Hoàn hảo', '["https://example.com/return/h32-1.jpg"]', '2025-11-25 18:30:00'),
(33, 'PICKUP', 2, '2025-11-26 08:30:00', 63000, 100, 'Sạch', '["https://example.com/pickup/h33-1.jpg"]', '2025-11-26 08:30:00'),
(33, 'RETURN', 2, '2025-11-29 17:30:00', 63750, 92, 'Tốt', '["https://example.com/return/h33-1.jpg"]', '2025-11-29 17:30:00'),

-- December 2025
(34, 'PICKUP', 3, '2025-12-01 09:30:00', 56000, 100, 'Xe đẹp', '["https://example.com/pickup/h34-1.jpg"]', '2025-12-01 09:30:00'),
(34, 'RETURN', 3, '2025-12-06 18:30:00', 57200, 80, 'Tốt', '["https://example.com/return/h34-1.jpg"]', '2025-12-06 18:30:00'),
(35, 'PICKUP', 2, '2025-12-08 08:30:00', 39000, 100, 'OK', '["https://example.com/pickup/h35-1.jpg"]', '2025-12-08 08:30:00'),
(35, 'RETURN', 2, '2025-12-12 17:30:00', 39950, 90, 'Tốt', '["https://example.com/return/h35-1.jpg"]', '2025-12-12 17:30:00'),
(36, 'PICKUP', 3, '2025-12-14 10:30:00', 42000, 100, 'Sạch', '["https://example.com/pickup/h36-1.jpg"]', '2025-12-14 10:30:00'),
(36, 'RETURN', 3, '2025-12-19 16:30:00', 43200, 85, 'Hoàn hảo', '["https://example.com/return/h36-1.jpg"]', '2025-12-19 16:30:00'),
(37, 'PICKUP', 2, '2025-12-20 09:30:00', 24000, 100, 'Tốt', '["https://example.com/pickup/h37-1.jpg"]', '2025-12-20 09:30:00'),
(37, 'RETURN', 2, '2025-12-23 18:30:00', 24750, 92, 'OK', '["https://example.com/return/h37-1.jpg"]', '2025-12-23 18:30:00'),
(38, 'PICKUP', 3, '2025-12-26 08:30:00', 36000, 100, 'Sạch', '["https://example.com/pickup/h38-1.jpg"]', '2025-12-26 08:30:00'),
(38, 'RETURN', 3, '2025-12-30 17:30:00', 36950, 88, 'Tốt', '["https://example.com/return/h38-1.jpg"]', '2025-12-30 17:30:00'),

-- January 2025 (additional)
(39, 'PICKUP', 2, '2025-01-05 09:30:00', 43000, 100, 'Hoàn hảo', '["https://example.com/pickup/h39-1.jpg"]', '2025-01-05 09:30:00'),
(39, 'RETURN', 2, '2025-01-10 18:30:00', 44200, 82, 'Tốt', '["https://example.com/return/h39-1.jpg"]', '2025-01-10 18:30:00'),
(40, 'PICKUP', 3, '2025-01-12 08:30:00', 52000, 100, 'Xe đẹp', '["https://example.com/pickup/h40-1.jpg"]', '2025-01-12 08:30:00'),
(40, 'RETURN', 3, '2025-01-16 17:30:00', 52950, 90, 'OK', '["https://example.com/return/h40-1.jpg"]', '2025-01-16 17:30:00'),
(41, 'PICKUP', 2, '2025-01-18 10:30:00', 24000, 100, 'Sạch', '["https://example.com/pickup/h41-1.jpg"]', '2025-01-18 10:30:00'),
(41, 'RETURN', 2, '2025-01-22 16:30:00', 24850, 85, 'Tốt', '["https://example.com/return/h41-1.jpg"]', '2025-01-22 16:30:00'),
(42, 'PICKUP', 3, '2025-01-24 09:30:00', 53000, 100, 'OK', '["https://example.com/pickup/h42-1.jpg"]', '2025-01-24 09:30:00'),
(42, 'RETURN', 3, '2025-01-28 18:30:00', 53950, 88, 'Hoàn hảo', '["https://example.com/return/h42-1.jpg"]', '2025-01-28 18:30:00'),

-- February 2025 (additional)
(43, 'PICKUP', 2, '2025-02-02 09:30:00', 40000, 100, 'Tốt', '["https://example.com/pickup/h43-1.jpg"]', '2025-02-02 09:30:00'),
(43, 'RETURN', 2, '2025-02-07 18:30:00', 41200, 87, 'OK', '["https://example.com/return/h43-1.jpg"]', '2025-02-07 18:30:00'),
(44, 'PICKUP', 3, '2025-02-09 08:30:00', 53000, 100, 'Sạch', '["https://example.com/pickup/h44-1.jpg"]', '2025-02-09 08:30:00'),
(44, 'RETURN', 3, '2025-02-13 17:30:00', 53950, 90, 'Tốt', '["https://example.com/return/h44-1.jpg"]', '2025-02-13 17:30:00'),
(45, 'PICKUP', 2, '2025-02-16 10:30:00', 67000, 100, 'Hoàn hảo', '["https://example.com/pickup/h45-1.jpg"]', '2025-02-16 10:30:00'),
(45, 'RETURN', 2, '2025-02-21 16:30:00', 68300, 75, 'OK', '["https://example.com/return/h45-1.jpg"]', '2025-02-21 16:30:00'),
(46, 'PICKUP', 3, '2025-02-23 09:30:00', 45000, 100, 'Xe đẹp', '["https://example.com/pickup/h46-1.jpg"]', '2025-02-23 09:30:00'),
(46, 'RETURN', 3, '2025-02-27 18:30:00', 45950, 85, 'Tốt', '["https://example.com/return/h46-1.jpg"]', '2025-02-27 18:30:00');

-- Additional return fees (mix of normal returns and fees)
INSERT INTO return_fees (contract_id, handover_id, is_late, hours_late, late_fee, has_damage, damage_description, damage_fee, is_different_location, one_way_fee, total_fees, created_at) VALUES
-- Most returns are normal (no fees)
(15, 2, FALSE, 0, 0, FALSE, NULL, 0, FALSE, 0, 0, '2025-08-10 18:45:00'),
(16, 4, FALSE, 0, 0, FALSE, NULL, 0, FALSE, 0, 0, '2025-08-15 17:45:00'),
(17, 6, FALSE, 0, 0, FALSE, NULL, 0, FALSE, 0, 0, '2025-08-23 16:45:00'),
(18, 8, FALSE, 0, 0, FALSE, NULL, 0, FALSE, 0, 0, '2025-08-28 18:45:00'),
(19, 10, FALSE, 0, 0, FALSE, NULL, 0, FALSE, 0, 0, '2025-09-07 18:45:00'),
(20, 12, FALSE, 0, 0, FALSE, NULL, 0, FALSE, 0, 0, '2025-09-13 17:45:00'),
(21, 14, FALSE, 0, 0, FALSE, NULL, 0, FALSE, 0, 0, '2025-09-20 16:45:00'),
(22, 16, FALSE, 0, 0, FALSE, NULL, 0, FALSE, 0, 0, '2025-09-26 18:45:00'),
(23, 18, FALSE, 0, 0, FALSE, NULL, 0, FALSE, 0, 0, '2025-10-01 17:45:00'),
(24, 20, FALSE, 0, 0, FALSE, NULL, 0, FALSE, 0, 0, '2025-10-08 18:45:00'),
-- October contract 25: Late return (8 hours)
(25, 22, TRUE, 8, 400000, FALSE, NULL, 0, FALSE, 0, 400000, '2025-10-14 17:45:00'),
(26, 24, FALSE, 0, 0, FALSE, NULL, 0, FALSE, 0, 0, '2025-10-20 16:45:00'),
(27, 26, FALSE, 0, 0, FALSE, NULL, 0, FALSE, 0, 0, '2025-10-25 18:45:00'),
(28, 28, FALSE, 0, 0, FALSE, NULL, 0, FALSE, 0, 0, '2025-10-31 17:45:00'),
(29, 30, FALSE, 0, 0, FALSE, NULL, 0, FALSE, 0, 0, '2025-11-07 18:45:00'),
(30, 32, FALSE, 0, 0, FALSE, NULL, 0, FALSE, 0, 0, '2025-11-12 17:45:00'),
(31, 34, FALSE, 0, 0, FALSE, NULL, 0, FALSE, 0, 0, '2025-11-19 16:45:00'),
-- November contract 32: Minor damage
(32, 36, FALSE, 0, 0, TRUE, 'Trầy nhỏ cản sau', 500000, FALSE, 0, 500000, '2025-11-25 18:45:00'),
(33, 38, FALSE, 0, 0, FALSE, NULL, 0, FALSE, 0, 0, '2025-11-29 17:45:00'),
(34, 40, FALSE, 0, 0, FALSE, NULL, 0, FALSE, 0, 0, '2025-12-06 18:45:00'),
(35, 42, FALSE, 0, 0, FALSE, NULL, 0, FALSE, 0, 0, '2025-12-12 17:45:00'),
(36, 44, FALSE, 0, 0, FALSE, NULL, 0, FALSE, 0, 0, '2025-12-19 16:45:00'),
(37, 46, FALSE, 0, 0, FALSE, NULL, 0, FALSE, 0, 0, '2025-12-23 18:45:00'),
-- December contract 38: Different location return
(38, 48, FALSE, 0, 0, FALSE, NULL, 0, TRUE, 1000000, 1000000, '2025-12-30 17:45:00'),
(39, 50, FALSE, 0, 0, FALSE, NULL, 0, FALSE, 0, 0, '2025-01-10 18:45:00'),
(40, 52, FALSE, 0, 0, FALSE, NULL, 0, FALSE, 0, 0, '2025-01-16 17:45:00'),
(41, 54, FALSE, 0, 0, FALSE, NULL, 0, FALSE, 0, 0, '2025-01-22 16:45:00'),
(42, 56, FALSE, 0, 0, FALSE, NULL, 0, FALSE, 0, 0, '2025-01-28 18:45:00'),
(43, 58, FALSE, 0, 0, FALSE, NULL, 0, FALSE, 0, 0, '2025-02-07 18:45:00'),
-- February contract 44: Late + different location
(44, 60, TRUE, 12, 600000, FALSE, NULL, 0, TRUE, 950000, 1550000, '2025-02-13 17:45:00'),
(45, 62, FALSE, 0, 0, FALSE, NULL, 0, FALSE, 0, 0, '2025-02-21 16:45:00'),
(46, 64, FALSE, 0, 0, FALSE, NULL, 0, FALSE, 0, 0, '2025-02-27 18:45:00');

-- Additional deposit holds and refunds
INSERT INTO deposit_holds (contract_id, deposit_amount, deducted_at_return, hold_start_date, hold_end_date, status, created_at) VALUES
(15, 50000000, 0, '2025-08-10 18:45:00', '2025-08-24 18:45:00', 'READY', '2025-08-10 18:45:00'),
(16, 50000000, 0, '2025-08-15 17:45:00', '2025-08-29 17:45:00', 'READY', '2025-08-15 17:45:00'),
(17, 50000000, 0, '2025-08-23 16:45:00', '2025-09-06 16:45:00', 'READY', '2025-08-23 16:45:00'),
(18, 50000000, 0, '2025-08-28 18:45:00', '2025-09-11 18:45:00', 'READY', '2025-08-28 18:45:00'),
(19, 50000000, 0, '2025-09-07 18:45:00', '2025-09-21 18:45:00', 'READY', '2025-09-07 18:45:00'),
(20, 50000000, 0, '2025-09-13 17:45:00', '2025-09-27 17:45:00', 'READY', '2025-09-13 17:45:00'),
(21, 50000000, 0, '2025-09-20 16:45:00', '2025-10-04 16:45:00', 'READY', '2025-09-20 16:45:00'),
(22, 50000000, 0, '2025-09-26 18:45:00', '2025-10-10 18:45:00', 'READY', '2025-09-26 18:45:00'),
(23, 50000000, 0, '2025-10-01 17:45:00', '2025-10-15 17:45:00', 'READY', '2025-10-01 17:45:00'),
(24, 50000000, 0, '2025-10-08 18:45:00', '2025-10-22 18:45:00', 'READY', '2025-10-08 18:45:00'),
(25, 50000000, 400000, '2025-10-14 17:45:00', '2025-10-28 17:45:00', 'READY', '2025-10-14 17:45:00'),
(26, 50000000, 0, '2025-10-20 16:45:00', '2025-11-03 16:45:00', 'READY', '2025-10-20 16:45:00'),
(27, 50000000, 0, '2025-10-25 18:45:00', '2025-11-08 18:45:00', 'READY', '2025-10-25 18:45:00'),
(28, 50000000, 0, '2025-10-31 17:45:00', '2025-11-14 17:45:00', 'READY', '2025-10-31 17:45:00'),
(29, 50000000, 0, '2025-11-07 18:45:00', '2025-11-21 18:45:00', 'READY', '2025-11-07 18:45:00'),
(30, 50000000, 0, '2025-11-12 17:45:00', '2025-11-26 17:45:00', 'READY', '2025-11-12 17:45:00'),
(31, 50000000, 0, '2025-11-19 16:45:00', '2025-12-03 16:45:00', 'READY', '2025-11-19 16:45:00'),
(32, 50000000, 500000, '2025-11-25 18:45:00', '2025-12-09 18:45:00', 'READY', '2025-11-25 18:45:00'),
(33, 50000000, 0, '2025-11-29 17:45:00', '2025-12-13 17:45:00', 'READY', '2025-11-29 17:45:00'),
(34, 50000000, 0, '2025-12-06 18:45:00', '2025-12-20 18:45:00', 'READY', '2025-12-06 18:45:00'),
(35, 50000000, 0, '2025-12-12 17:45:00', '2025-12-26 17:45:00', 'READY', '2025-12-12 17:45:00'),
(36, 50000000, 0, '2025-12-19 16:45:00', '2025-01-02 16:45:00', 'READY', '2025-12-19 16:45:00'),
(37, 50000000, 0, '2025-12-23 18:45:00', '2025-01-06 18:45:00', 'READY', '2025-12-23 18:45:00'),
(38, 50000000, 1000000, '2025-12-30 17:45:00', '2025-01-13 17:45:00', 'READY', '2025-12-30 17:45:00'),
(39, 50000000, 0, '2025-01-10 18:45:00', '2025-01-24 18:45:00', 'READY', '2025-01-10 18:45:00'),
(40, 50000000, 0, '2025-01-16 17:45:00', '2025-01-30 17:45:00', 'READY', '2025-01-16 17:45:00'),
(41, 50000000, 0, '2025-01-22 16:45:00', '2025-02-05 16:45:00', 'READY', '2025-01-22 16:45:00'),
(42, 50000000, 0, '2025-01-28 18:45:00', '2025-02-11 18:45:00', 'READY', '2025-01-28 18:45:00'),
(43, 50000000, 0, '2025-02-07 18:45:00', '2025-02-21 18:45:00', 'READY', '2025-02-07 18:45:00'),
(44, 50000000, 1550000, '2025-02-13 17:45:00', '2025-02-27 17:45:00', 'READY', '2025-02-13 17:45:00'),
(45, 50000000, 0, '2025-02-21 16:45:00', '2025-03-07 16:45:00', 'READY', '2025-02-21 16:45:00'),
(46, 50000000, 0, '2025-02-27 18:45:00', '2025-03-13 18:45:00', 'READY', '2025-02-27 18:45:00');

-- Additional refunds (most are completed)
INSERT INTO refunds (hold_id, contract_id, customer_id, original_deposit, deducted_at_return, traffic_fines, refund_amount, refund_method, status, processed_at, created_at) VALUES
(5, 15, 5, 50000000, 0, 0, 50000000, 'Transfer', 'Completed', '2025-08-25 10:00:00', '2025-08-24 18:45:00'),
(6, 16, 6, 50000000, 0, 0, 50000000, 'Transfer', 'Completed', '2025-08-30 10:00:00', '2025-08-29 17:45:00'),
(7, 17, 7, 50000000, 0, 0, 50000000, 'Transfer', 'Completed', '2025-09-07 10:00:00', '2025-09-06 16:45:00'),
(8, 18, 8, 50000000, 0, 0, 50000000, 'Transfer', 'Completed', '2025-09-12 10:00:00', '2025-09-11 18:45:00'),
(9, 19, 9, 50000000, 0, 0, 50000000, 'Transfer', 'Completed', '2025-09-22 10:00:00', '2025-09-21 18:45:00'),
(10, 20, 10, 50000000, 0, 0, 50000000, 'Transfer', 'Completed', '2025-09-28 10:00:00', '2025-09-27 17:45:00'),
(11, 21, 11, 50000000, 0, 0, 50000000, 'Transfer', 'Completed', '2025-10-05 10:00:00', '2025-10-04 16:45:00'),
(12, 22, 5, 50000000, 0, 0, 50000000, 'Transfer', 'Completed', '2025-10-11 10:00:00', '2025-10-10 18:45:00'),
(13, 23, 6, 50000000, 0, 0, 50000000, 'Transfer', 'Completed', '2025-10-16 10:00:00', '2025-10-15 17:45:00'),
(14, 24, 7, 50000000, 0, 0, 50000000, 'Transfer', 'Completed', '2025-10-23 10:00:00', '2025-10-22 18:45:00'),
(15, 25, 8, 50000000, 400000, 0, 49600000, 'Transfer', 'Completed', '2025-10-29 10:00:00', '2025-10-28 17:45:00'),
(16, 26, 9, 50000000, 0, 0, 50000000, 'Transfer', 'Completed', '2025-11-04 10:00:00', '2025-11-03 16:45:00'),
(17, 27, 10, 50000000, 0, 0, 50000000, 'Transfer', 'Completed', '2025-11-09 10:00:00', '2025-11-08 18:45:00'),
(18, 28, 11, 50000000, 0, 0, 50000000, 'Transfer', 'Completed', '2025-11-15 10:00:00', '2025-11-14 17:45:00'),
(19, 29, 5, 50000000, 0, 0, 50000000, 'Transfer', 'Completed', '2025-11-22 10:00:00', '2025-11-21 18:45:00'),
(20, 30, 6, 50000000, 0, 0, 50000000, 'Transfer', 'Completed', '2025-11-27 10:00:00', '2025-11-26 17:45:00'),
(21, 31, 7, 50000000, 0, 0, 50000000, 'Transfer', 'Completed', '2025-12-04 10:00:00', '2025-12-03 16:45:00'),
(22, 32, 8, 50000000, 500000, 0, 49500000, 'Transfer', 'Completed', '2025-12-10 10:00:00', '2025-12-09 18:45:00'),
(23, 33, 9, 50000000, 0, 0, 50000000, 'Transfer', 'Completed', '2025-12-14 10:00:00', '2025-12-13 17:45:00'),
(24, 34, 10, 50000000, 0, 0, 50000000, 'Transfer', 'Completed', '2025-12-21 10:00:00', '2025-12-20 18:45:00'),
(25, 35, 11, 50000000, 0, 0, 50000000, 'Transfer', 'Completed', '2025-12-27 10:00:00', '2025-12-26 17:45:00'),
(26, 36, 5, 50000000, 0, 0, 50000000, 'Transfer', 'Completed', '2025-01-03 10:00:00', '2025-01-02 16:45:00'),
(27, 37, 6, 50000000, 0, 0, 50000000, 'Transfer', 'Completed', '2025-01-07 10:00:00', '2025-01-06 18:45:00'),
(28, 38, 7, 50000000, 1000000, 0, 49000000, 'Transfer', 'Completed', '2025-01-14 10:00:00', '2025-01-13 17:45:00'),
(29, 39, 8, 50000000, 0, 0, 50000000, 'Transfer', 'Completed', '2025-01-25 10:00:00', '2025-01-24 18:45:00'),
(30, 40, 9, 50000000, 0, 0, 50000000, 'Transfer', 'Completed', '2025-01-31 10:00:00', '2025-01-30 17:45:00'),
(31, 41, 10, 50000000, 0, 0, 50000000, 'Transfer', 'Completed', '2025-02-06 10:00:00', '2025-02-05 16:45:00'),
(32, 42, 11, 50000000, 0, 0, 50000000, 'Transfer', 'Completed', '2025-02-12 10:00:00', '2025-02-11 18:45:00'),
(33, 43, 5, 50000000, 0, 0, 50000000, 'Transfer', 'Completed', '2025-02-22 10:00:00', '2025-02-21 18:45:00'),
(34, 44, 6, 50000000, 1550000, 0, 48450000, 'Transfer', 'Completed', '2025-02-28 10:00:00', '2025-02-27 17:45:00'),
(35, 45, 7, 50000000, 0, 0, 50000000, 'Transfer', 'Completed', '2025-03-08 10:00:00', '2025-03-07 16:45:00'),
(36, 46, 8, 50000000, 0, 0, 50000000, 'Transfer', 'Pending', NULL, '2025-03-13 18:45:00');

-- ===================================================================
-- STORED PROCEDURES
-- ===================================================================

DELIMITER //

-- Tính tổng tiền thuê
CREATE PROCEDURE sp_calculate_rental(
    IN p_vehicle_id INT,
    IN p_days INT,
    OUT p_daily_rate DECIMAL(10,2),
    OUT p_total_fee DECIMAL(10,2),
    OUT p_deposit DECIMAL(10,2)
)
BEGIN
    SELECT daily_rate, deposit_amount
    INTO p_daily_rate, p_deposit
    FROM vehicles
    WHERE vehicle_id = p_vehicle_id;
    
    SET p_total_fee = p_daily_rate * p_days;
END//

-- Kiểm tra xe available
CREATE PROCEDURE sp_check_availability(
    IN p_vehicle_id INT,
    IN p_start_date DATETIME,
    IN p_end_date DATETIME,
    OUT p_available BOOLEAN
)
BEGIN
    DECLARE v_count INT;
    
    SELECT COUNT(*) INTO v_count
    FROM contracts
    WHERE vehicle_id = p_vehicle_id
    AND status = 'Active'
    AND (
        (p_start_date BETWEEN start_date AND end_date)
        OR (p_end_date BETWEEN start_date AND end_date)
        OR (start_date BETWEEN p_start_date AND p_end_date)
    );
    
    SET p_available = (v_count = 0);
END//

DELIMITER ;

-- ===================================================================
-- TRIGGERS
-- ===================================================================

DELIMITER //

-- Update vehicle status when contract becomes ACTIVE (after deposit payment)
-- Vehicle should NOT be marked as Rented when contract is created with Pending_Payment status
CREATE TRIGGER trg_contract_created
AFTER INSERT ON contracts
FOR EACH ROW
BEGIN
    -- Only mark vehicle as Rented if contract is created with Active status
    IF NEW.status = 'Active' THEN
        UPDATE vehicles
        SET status = 'Rented'
        WHERE vehicle_id = NEW.vehicle_id;
    END IF;
END//

-- Update vehicle status when contract status changes to ACTIVE
CREATE TRIGGER trg_contract_activated
AFTER UPDATE ON contracts
FOR EACH ROW
BEGIN
    -- When contract changes from Pending_Payment to Active, mark vehicle as Rented
    IF OLD.status = 'Pending_Payment' AND NEW.status = 'Active' THEN
        UPDATE vehicles
        SET status = 'Rented'
        WHERE vehicle_id = NEW.vehicle_id;
    END IF;
END//

-- Update vehicle status khi có return fees (tức là đã trả xe)
CREATE TRIGGER trg_vehicle_returned
AFTER INSERT ON return_fees
FOR EACH ROW
BEGIN
    UPDATE vehicles v
    JOIN contracts c ON c.vehicle_id = v.vehicle_id
    SET v.status = 'Available',
        c.status = 'Completed'
    WHERE c.contract_id = NEW.contract_id;
END//

-- Tự động tính tổng phí
CREATE TRIGGER trg_calculate_total_fees
BEFORE INSERT ON return_fees
FOR EACH ROW
BEGIN
    SET NEW.total_fees = NEW.late_fee + NEW.damage_fee + NEW.one_way_fee;
END//

DELIMITER ;

-- ===================================================================
-- EVENTS
-- ===================================================================

SET GLOBAL event_scheduler = ON;

DELIMITER //

-- Tự động chuyển status deposit hold sang Ready sau 14 ngày
CREATE EVENT evt_auto_ready_refund
ON SCHEDULE EVERY 1 DAY
DO
BEGIN
    UPDATE deposit_holds
    SET status = 'READY'
    WHERE status = 'HOLDING'
    AND hold_end_date <= NOW();
END//

DELIMITER ;

-- ===================================================================
-- VIEWS
-- ===================================================================

-- View: Xe available
CREATE VIEW v_available_vehicles AS
SELECT 
    v.vehicle_id,
    v.license_plate,
    CONCAT(b.brand_name, ' ', m.model_name, ' ', m.year) AS vehicle_name,
    m.category,
    m.seats,
    m.transmission,
    v.daily_rate,
    v.deposit_amount,
    l.location_name,
    l.city
FROM vehicles v
JOIN vehicle_models m ON v.model_id = m.model_id
JOIN vehicle_brands b ON m.brand_id = b.brand_id
JOIN locations l ON v.location_id = l.location_id
WHERE v.status = 'Available';

-- View: Hợp đồng đang active
CREATE VIEW v_active_contracts AS
SELECT 
    c.contract_id,
    c.contract_number,
    u.full_name,
    u.phone,
    v.license_plate,
    c.start_date,
    c.end_date,
    DATEDIFF(c.end_date, NOW()) AS days_left
FROM contracts c
JOIN users u ON c.customer_id = u.user_id
JOIN vehicles v ON c.vehicle_id = v.vehicle_id
WHERE c.status = 'Active';

-- View: Tiền cọc chờ hoàn
CREATE VIEW v_pending_refunds AS
SELECT 
    h.hold_id,
    c.contract_number,
    u.full_name,
    u.phone,
    h.deposit_amount - h.deducted_at_return AS remaining,
    h.hold_end_date,
    h.status
FROM deposit_holds h
JOIN contracts c ON h.contract_id = c.contract_id
JOIN users u ON c.customer_id = u.user_id
WHERE h.status IN ('HOLDING', 'READY');

-- View: Vi phạm giao thông chưa xử lý
CREATE VIEW v_pending_violations AS
SELECT 
    tv.violation_id,
    c.contract_number,
    u.full_name,
    tv.violation_type,
    tv.violation_date,
    tv.fine_amount,
    tv.status
FROM traffic_violations tv
JOIN contracts c ON tv.contract_id = c.contract_id
JOIN users u ON c.customer_id = u.user_id
WHERE tv.status = 'Pending';

-- ===================================================================
-- INDEXES
-- ===================================================================

CREATE INDEX idx_contracts_active ON contracts(status, start_date);
CREATE INDEX idx_vehicles_location ON vehicles(status, location_id);
CREATE INDEX idx_bookings_date ON bookings(start_date, end_date);

