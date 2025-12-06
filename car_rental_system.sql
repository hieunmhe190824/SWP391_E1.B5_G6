-- ===================================================================
-- HỆ THỐNG THUÊ XE - DATABASE SCHEMA (MySQL 8.0+)
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
    daily_rate DECIMAL(10, 2) NOT NULL,                  -- Giá/ngày
    deposit_amount DECIMAL(10, 2) NOT NULL,              -- Tiền cọc
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
CREATE TABLE bookings (
    booking_id INT PRIMARY KEY AUTO_INCREMENT,
    customer_id INT NOT NULL,
    vehicle_id INT NOT NULL,
    pickup_location_id INT NOT NULL,
    return_location_id INT NOT NULL,
    start_date DATETIME NOT NULL,
    end_date DATETIME NOT NULL,
    total_days INT NOT NULL,
    status ENUM('Pending', 'Approved', 'Rejected', 'Cancelled') DEFAULT 'Pending',
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
CREATE TABLE contracts (
    contract_id INT PRIMARY KEY AUTO_INCREMENT,
    booking_id INT NOT NULL UNIQUE,                      -- 1-1 với booking
    contract_number VARCHAR(50) NOT NULL UNIQUE,
    customer_id INT NOT NULL,
    vehicle_id INT NOT NULL,
    staff_id INT NOT NULL,                               -- Staff tạo
    start_date DATETIME NOT NULL,
    end_date DATETIME NOT NULL,
    total_days INT NOT NULL,
    daily_rate DECIMAL(10, 2) NOT NULL,
    total_rental_fee DECIMAL(10, 2) NOT NULL,
    deposit_amount DECIMAL(10, 2) NOT NULL,
    status ENUM('Active', 'Completed', 'Cancelled') DEFAULT 'Active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE,
    FOREIGN KEY (customer_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id) ON DELETE CASCADE,
    FOREIGN KEY (staff_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_customer (customer_id),
    INDEX idx_status (status),
    CONSTRAINT chk_deposit_fee CHECK (deposit_amount > total_rental_fee)
) ENGINE=InnoDB;

-- Bảng: payments - Thanh toán
CREATE TABLE payments (
    payment_id INT PRIMARY KEY AUTO_INCREMENT,
    contract_id INT NOT NULL,
    payment_type ENUM('Deposit', 'Rental', 'Refund') NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    method ENUM('Cash', 'Card', 'Transfer') NOT NULL,
    status ENUM('Pending', 'Completed', 'Failed') DEFAULT 'Pending',
    payment_date DATETIME,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (contract_id) REFERENCES contracts(contract_id) ON DELETE CASCADE,
    INDEX idx_contract (contract_id),
    INDEX idx_type (payment_type)
) ENGINE=InnoDB;

-- ===================================================================
-- 4. RENTAL PROCESS (2 bảng)
-- ===================================================================

-- Bảng: handovers - Bàn giao xe (pickup + return)
CREATE TABLE handovers (
    handover_id INT PRIMARY KEY AUTO_INCREMENT,
    contract_id INT NOT NULL,
    handover_type ENUM('Pickup', 'Return') NOT NULL,     -- Nhận hoặc trả
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
    hours_late DECIMAL(10, 2) DEFAULT 0,
    late_fee DECIMAL(10, 2) DEFAULT 0,
    
    has_damage BOOLEAN DEFAULT FALSE,
    damage_description TEXT,
    damage_fee DECIMAL(10, 2) DEFAULT 0,
    
    is_different_location BOOLEAN DEFAULT FALSE,
    one_way_fee DECIMAL(10, 2) DEFAULT 0,
    
    total_fees DECIMAL(10, 2) NOT NULL,                  -- Tổng phí
    
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
    deposit_amount DECIMAL(10, 2) NOT NULL,
    deducted_at_return DECIMAL(10, 2) DEFAULT 0,        -- Đã trừ khi trả xe
    hold_start_date DATETIME NOT NULL,
    hold_end_date DATETIME NOT NULL,
    status ENUM('Holding', 'Ready', 'Refunded') DEFAULT 'Holding',
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
    fine_amount DECIMAL(10, 2) NOT NULL,
    description TEXT,
    evidence_url VARCHAR(255),                           -- Link ảnh chứng cứ
    status ENUM('Pending', 'Confirmed') DEFAULT 'Pending',
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
    
    original_deposit DECIMAL(10, 2) NOT NULL,
    deducted_at_return DECIMAL(10, 2) DEFAULT 0,
    traffic_fines DECIMAL(10, 2) DEFAULT 0,             -- Tổng phạt (tính từ traffic_violations)
    
    refund_amount DECIMAL(10, 2) NOT NULL,
    refund_method ENUM('Transfer', 'Cash') NOT NULL,
    status ENUM('Pending', 'Completed') DEFAULT 'Pending',
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
    category ENUM('Booking', 'Payment', 'Vehicle', 'General') NOT NULL,
    subject VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    status ENUM('Open', 'In_Progress', 'Resolved', 'Closed') DEFAULT 'Open',
    assigned_to INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
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
('one_way_fee_percent', '15', 'Phí trả khác địa điểm (%)'),
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
(5, 'ID_Card', '001234567890', '/uploads/docs/cmnd_nguyen_minh_tuan.jpg', '2030-05-15', 'Approved', 2),
(5, 'Driver_License', 'B2-123456', '/uploads/docs/gplx_nguyen_minh_tuan.jpg', '2028-03-20', 'Approved', 2),

-- Customer 2 - Approved
(6, 'ID_Card', '001234567891', '/uploads/docs/cmnd_le_thi_lan_anh.jpg', '2029-08-10', 'Approved', 2),
(6, 'Driver_License', 'B2-234567', '/uploads/docs/gplx_le_thi_lan_anh.jpg', '2027-11-30', 'Approved', 3),

-- Customer 3 - Approved
(7, 'ID_Card', '001234567892', '/uploads/docs/cmnd_tran_duc_minh.jpg', '2031-02-25', 'Approved', 2),
(7, 'Driver_License', 'B2-345678', '/uploads/docs/gplx_tran_duc_minh.jpg', '2029-06-15', 'Approved', 2),

-- Customer 4 - Approved
(8, 'ID_Card', '001234567893', '/uploads/docs/cmnd_pham_thi_thu_ha.jpg', '2030-12-05', 'Approved', 3),
(8, 'Driver_License', 'B2-456789', '/uploads/docs/gplx_pham_thi_thu_ha.jpg', '2028-09-20', 'Approved', 3),

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
(12, 'Driver_License', 'B2-890123', '/uploads/docs/gplx_vo_thi_kim_loan.jpg', '2028-12-15', 'Approved', 2),

-- Customer 9 - Pending
(13, 'ID_Card', '001234567898', '/uploads/docs/cmnd_nguyen_thi_bich.jpg', '2031-06-20', 'Pending', NULL),
(13, 'Driver_License', 'B2-901234', '/uploads/docs/gplx_nguyen_thi_bich.jpg', '2029-08-05', 'Pending', NULL),

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
(1, 'Camry', 'Sedan', 2024, 5, 'Automatic', 'Gasoline'),
(1, 'Fortuner', 'SUV', 2023, 7, 'Automatic', 'Diesel'),
(1, 'Innova', 'Economy', 2022, 7, 'Manual', 'Gasoline'),

-- Honda
(2, 'City', 'Economy', 2023, 5, 'Automatic', 'Gasoline'),
(2, 'Civic', 'Sedan', 2024, 5, 'Automatic', 'Gasoline'),
(2, 'CR-V', 'SUV', 2023, 7, 'Automatic', 'Gasoline'),
(2, 'Accord', 'Sedan', 2023, 5, 'Automatic', 'Gasoline'),

-- Mazda
(3, 'Mazda 3', 'Sedan', 2023, 5, 'Automatic', 'Gasoline'),
(3, 'CX-5', 'SUV', 2024, 5, 'Automatic', 'Gasoline'),
(3, 'CX-8', 'SUV', 2023, 7, 'Automatic', 'Diesel'),

-- Hyundai
(4, 'Accent', 'Economy', 2023, 5, 'Automatic', 'Gasoline'),
(4, 'Elantra', 'Sedan', 2024, 5, 'Automatic', 'Gasoline'),
(4, 'Tucson', 'SUV', 2023, 5, 'Automatic', 'Gasoline'),
(4, 'Santa Fe', 'SUV', 2024, 7, 'Automatic', 'Diesel'),

-- Kia
(5, 'Morning', 'Economy', 2022, 4, 'Manual', 'Gasoline'),
(5, 'Cerato', 'Sedan', 2023, 5, 'Automatic', 'Gasoline'),
(5, 'Seltos', 'SUV', 2024, 5, 'Automatic', 'Gasoline'),
(5, 'Sorento', 'SUV', 2023, 7, 'Automatic', 'Diesel'),

-- Ford
(6, 'Ranger', 'SUV', 2023, 5, 'Automatic', 'Diesel'),
(6, 'Everest', 'SUV', 2024, 7, 'Automatic', 'Diesel'),

-- Mitsubishi
(7, 'Attrage', 'Economy', 2022, 5, 'Manual', 'Gasoline'),
(7, 'Xpander', 'Economy', 2023, 7, 'Automatic', 'Gasoline'),

-- Vinfast
(8, 'VF 5', 'Economy', 2024, 5, 'Automatic', 'Electric'),
(8, 'VF 8', 'SUV', 2024, 5, 'Automatic', 'Electric'),
(8, 'VF 9', 'SUV', 2024, 7, 'Automatic', 'Electric'),

-- Mercedes
(9, 'E-Class', 'Luxury', 2024, 5, 'Automatic', 'Gasoline'),
(9, 'S-Class', 'Luxury', 2024, 5, 'Automatic', 'Gasoline'),

-- BMW
(10, '5 Series', 'Luxury', 2024, 5, 'Automatic', 'Gasoline'),
(10, 'X5', 'Luxury', 2024, 7, 'Automatic', 'Diesel');

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
-- Available vehicles
(1, 1, '30A-12345', 'Trắng', 'Available', 500000, 3000000, '["30a12345.jpg"]'),
(1, 1, '30A-12346', 'Đen', 'Available', 500000, 3000000, '["30a12346.webp"]'),
(5, 1, '30B-23456', 'Bạc', 'Available', 550000, 3500000, '["30b23456.jpg"]'),
(12, 2, '30C-34567', 'Đỏ', 'Available', 600000, 4000000, '["30c34567.jpg"]'),
(16, 2, '30D-45678', 'Xanh', 'Available', 400000, 2500000, '["30d45678.jpg"]'),
(22, 3, '30E-56789', 'Trắng', 'Available', 450000, 2800000, '["30e56789.webp"]'),
(4, 3, '30F-67890', 'Bạc', 'Available', 700000, 4500000, '["30f67890.jpg"]'),
(23, 4, '30G-78901', 'Đen', 'Available', 800000, 5000000, '["30g78901.jpg"]'),

-- Sedan category
(2, 1, '30H-89012', 'Đen', 'Available', 1200000, 8000000, '["30h89012.webp"]'),
(6, 2, '30I-90123', 'Trắng', 'Available', 1100000, 7500000, '["30i90123.webp"]'),
(9, 3, '30J-01234', 'Đỏ', 'Available', 1150000, 7800000, '["30j01234.jpg"]'),
(13, 4, '30K-12347', 'Xanh', 'Available', 1100000, 7500000, '["30k12347.jpg"]'),

-- SUV category
(3, 1, '30L-23458', 'Đen', 'Available', 1800000, 12000000, '["30l23458.jpg"]'),
(7, 2, '30M-34569', 'Trắng', 'Available', 1500000, 10000000, '["30m34569.jpg"]'),
(10, 3, '30N-45670', 'Bạc', 'Available', 1600000, 11000000, '["30n45670.jpg"]'),
(14, 4, '30O-56781', 'Đỏ', 'Available', 1550000, 10500000, '["30o56781.jpeg"]'),
(18, 1, '30P-67892', 'Xanh', 'Available', 1650000, 11500000, '["30p67892.jpg"]'),

-- Luxury category
(27, 1, '30Q-78903', 'Đen', 'Available', 3500000, 25000000, '["30q78903.webp"]'),
(28, 2, '30R-89014', 'Trắng', 'Available', 5000000, 35000000, '["30r89014.jpg"]'),
(29, 3, '30S-90125', 'Bạc', 'Available', 3800000, 27000000, '["30s90125.webp"]'),

-- Rented vehicles (for active contracts)
(1, 1, '30T-01235', 'Trắng', 'Rented', 500000, 3000000, '["30t01235.webp"]'),
(5, 2, '30U-12348', 'Đen', 'Rented', 550000, 3500000, '["30u12348.jpg"]'),
(9, 3, '30V-23459', 'Đỏ', 'Rented', 1150000, 7800000, '["30v23459.webp"]'),
(10, 4, '30W-34570', 'Xanh', 'Rented', 1600000, 11000000, '["30w34570.jpg"]'),
(27, 1, '30X-45681', 'Đen', 'Rented', 3500000, 25000000, '["30x45681.webp"]'),

-- Maintenance vehicles
(3, 5, '30AB-11111', 'Đen', 'Maintenance', 1800000, 12000000, '["30ab11111.jpg"]'),
(7, 6, '30AC-22222', 'Trắng', 'Maintenance', 1500000, 10000000, '["30ac22222.webp"]'),

-- Electric vehicles
(24, 3, '30Y-56792', 'Trắng', 'Available', 800000, 5500000, '["30y56792.webp"]'),
(25, 4, '30Z-67803', 'Đen', 'Available', 1400000, 9500000, '["30z67803.jpg"]'),
(26, 1, '30AA-78914', 'Xanh', 'Available', 1900000, 13000000, '["30aa78914.webp"]');

-- 7. BOOKINGS
INSERT INTO bookings (customer_id, vehicle_id, pickup_location_id, return_location_id, start_date, end_date, total_days, status, created_at) VALUES
-- Approved bookings (will create contracts)
(5, 21, 1, 1, '2024-12-10 09:00:00', '2024-12-13 18:00:00', 3, 'Approved', '2024-12-05 10:30:00'),
(6, 22, 2, 2, '2024-12-12 08:00:00', '2024-12-15 17:00:00', 3, 'Approved', '2024-12-06 14:20:00'),
(7, 23, 3, 4, '2024-12-15 10:00:00', '2024-12-20 16:00:00', 5, 'Approved', '2024-12-07 09:15:00'),

-- Pending bookings (waiting for review)
(8, 1, 1, 1, '2024-12-20 09:00:00', '2024-12-25 18:00:00', 5, 'Pending', '2024-12-04 11:00:00'),
(9, 5, 2, 2, '2024-12-22 08:30:00', '2024-12-27 17:30:00', 5, 'Pending', '2024-12-04 13:45:00'),
(10, 12, 3, 3, '2024-12-25 10:00:00', '2024-12-30 16:00:00', 5, 'Pending', '2024-12-04 15:20:00'),
(11, 16, 4, 4, '2024-12-28 09:00:00', '2025-01-02 18:00:00', 5, 'Pending', '2024-12-04 16:30:00'),

-- Rejected bookings
(13, 8, 3, 3, '2024-12-15 09:00:00', '2024-12-18 18:00:00', 3, 'Rejected', '2024-12-03 10:00:00'),
(14, 11, 4, 4, '2024-12-18 08:00:00', '2024-12-22 17:00:00', 4, 'Rejected', '2024-12-03 14:30:00'),

-- Cancelled bookings
(5, 7, 3, 3, '2024-12-08 09:00:00', '2024-12-12 18:00:00', 4, 'Cancelled', '2024-12-01 09:00:00'),
(6, 9, 1, 2, '2024-12-10 10:00:00', '2024-12-14 16:00:00', 4, 'Cancelled', '2024-12-02 11:30:00');

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
INSERT INTO bookings (customer_id, vehicle_id, pickup_location_id, return_location_id, start_date, end_date, total_days, status, created_at) VALUES
-- Completed bookings for past contracts
(8, 9, 1, 1, '2024-11-01 09:00:00', '2024-11-05 18:00:00', 4, 'Approved', '2024-10-30 10:00:00'),
(9, 14, 2, 2, '2024-11-10 08:00:00', '2024-11-15 17:00:00', 5, 'Approved', '2024-11-08 14:00:00'),
(10, 15, 3, 4, '2024-11-15 10:00:00', '2024-11-20 16:00:00', 5, 'Approved', '2024-11-13 09:00:00'),
(11, 10, 4, 4, '2024-10-20 09:00:00', '2024-10-25 18:00:00', 5, 'Approved', '2024-10-18 11:00:00');

INSERT INTO contracts (booking_id, contract_number, customer_id, vehicle_id, staff_id, start_date, end_date, total_days, daily_rate, total_rental_fee, deposit_amount, status, created_at) VALUES
-- Active contracts (currently renting)
(1, 'HD-2024-0001', 5, 21, 2, '2024-12-10 09:00:00', '2024-12-13 18:00:00', 3, 500000, 1500000, 3000000, 'Active', '2024-12-09 14:00:00'),
(2, 'HD-2024-0002', 6, 22, 2, '2024-12-12 08:00:00', '2024-12-15 17:00:00', 3, 550000, 1650000, 3500000, 'Active', '2024-12-11 10:00:00'),
(3, 'HD-2024-0003', 7, 23, 3, '2024-12-15 10:00:00', '2024-12-20 16:00:00', 5, 1150000, 5750000, 7800000, 'Active', '2024-12-14 15:30:00'),

-- Completed contracts (returned vehicles)
-- Contract 4: Normal return, no issues
(12, 'HD-2024-0004', 8, 9, 2, '2024-11-01 09:00:00', '2024-11-05 18:00:00', 4, 1200000, 4800000, 8000000, 'Completed', '2024-10-31 11:00:00'),

-- Contract 5: Late return + damage fee
(13, 'HD-2024-0005', 9, 14, 3, '2024-11-10 08:00:00', '2024-11-15 17:00:00', 5, 1500000, 7500000, 10000000, 'Completed', '2024-11-09 09:30:00'),

-- Contract 6: Different location return
(14, 'HD-2024-0006', 10, 15, 2, '2024-11-15 10:00:00', '2024-11-20 16:00:00', 5, 1600000, 8000000, 11000000, 'Completed', '2024-11-14 14:20:00'),

-- Contract 7: Normal return with traffic violation
(15, 'HD-2024-0007', 11, 10, 3, '2024-10-20 09:00:00', '2024-10-25 18:00:00', 5, 1100000, 5500000, 7500000, 'Completed', '2024-10-19 10:45:00'),

-- Cancelled contract
(10, 'HD-2024-0008', 6, 7, 2, '2024-11-25 09:00:00', '2024-11-28 18:00:00', 3, 800000, 2400000, 5000000, 'Cancelled', '2024-11-24 13:00:00');

-- 10. PAYMENTS
INSERT INTO payments (contract_id, payment_type, amount, method, status, payment_date, created_at) VALUES
-- Active contracts - Deposit paid
(1, 'Deposit', 3000000, 'Transfer', 'Completed', '2024-12-09 14:30:00', '2024-12-09 14:30:00'),
(2, 'Deposit', 3500000, 'Card', 'Completed', '2024-12-11 10:30:00', '2024-12-11 10:30:00'),
(3, 'Deposit', 7800000, 'Transfer', 'Completed', '2024-12-14 16:00:00', '2024-12-14 16:00:00'),

-- Completed contracts - Full payments
(4, 'Deposit', 8000000, 'Transfer', 'Completed', '2024-10-31 11:30:00', '2024-10-31 11:30:00'),
(4, 'Rental', 4800000, 'Card', 'Completed', '2024-11-05 19:00:00', '2024-11-05 19:00:00'),

(5, 'Deposit', 10000000, 'Card', 'Completed', '2024-11-09 10:00:00', '2024-11-09 10:00:00'),
(5, 'Rental', 7500000, 'Transfer', 'Completed', '2024-11-16 11:00:00', '2024-11-16 11:00:00'),

(6, 'Deposit', 11000000, 'Transfer', 'Completed', '2024-11-14 14:45:00', '2024-11-14 14:45:00'),
(6, 'Rental', 8000000, 'Card', 'Completed', '2024-11-21 10:30:00', '2024-11-21 10:30:00'),

(7, 'Deposit', 7500000, 'Card', 'Completed', '2024-10-19 11:15:00', '2024-10-19 11:15:00'),
(7, 'Rental', 5500000, 'Transfer', 'Completed', '2024-10-25 19:30:00', '2024-10-25 19:30:00'),

-- Cancelled contract - Refund
(8, 'Deposit', 4500000, 'Transfer', 'Completed', '2024-11-24 13:30:00', '2024-11-24 13:30:00'),
(8, 'Refund', 4500000, 'Transfer', 'Completed', '2024-11-24 15:00:00', '2024-11-24 15:00:00');

-- 11. HANDOVERS
INSERT INTO handovers (contract_id, handover_type, staff_id, handover_time, odometer, fuel_level, condition_notes, images, created_at) VALUES
-- Active contracts - Pickup only
(1, 'Pickup', 2, '2024-12-10 09:30:00', 45000, 100, 'Xe trong tình trạng tốt, đầy xăng, không trầy xước', '["https://example.com/pickup/h1-1.jpg","https://example.com/pickup/h1-2.jpg"]', '2024-12-10 09:30:00'),
(2, 'Pickup', 2, '2024-12-12 08:30:00', 38000, 100, 'Xe sạch sẽ, không vấn đề gì', '["https://example.com/pickup/h2-1.jpg"]', '2024-12-12 08:30:00'),
(3, 'Pickup', 3, '2024-12-15 10:30:00', 52000, 100, 'Xe trong tình trạng hoàn hảo', '["https://example.com/pickup/h3-1.jpg","https://example.com/pickup/h3-2.jpg"]', '2024-12-15 10:30:00'),

-- Completed contracts - Pickup and Return
-- Contract 4: Normal return
(4, 'Pickup', 2, '2024-11-01 09:30:00', 62000, 100, 'Xe tốt, không vấn đề', '["https://example.com/pickup/h4-1.jpg"]', '2024-11-01 09:30:00'),
(4, 'Return', 2, '2024-11-05 18:20:00', 62800, 95, 'Xe trả đúng hạn, tình trạng tốt, xăng còn 95%', '["https://example.com/return/h4-1.jpg","https://example.com/return/h4-2.jpg"]', '2024-11-05 18:20:00'),

-- Contract 5: Late return + damage
(5, 'Pickup', 3, '2024-11-10 08:30:00', 78000, 100, 'Xe trong tình trạng tốt', '["https://example.com/pickup/h5-1.jpg"]', '2024-11-10 08:30:00'),
(5, 'Return', 3, '2024-11-16 10:30:00', 79200, 70, 'Trả trễ 17.5 giờ, có trầy xước ở cửa phụ, xăng còn 70%', '["https://example.com/return/h5-1.jpg","https://example.com/return/h5-2.jpg","https://example.com/return/h5-3.jpg"]', '2024-11-16 10:30:00'),

-- Contract 6: Different location return
(6, 'Pickup', 2, '2024-11-15 10:30:00', 55000, 100, 'Xe hoàn hảo', '["https://example.com/pickup/h6-1.jpg"]', '2024-11-15 10:30:00'),
(6, 'Return', 4, '2024-11-21 10:00:00', 55850, 90, 'Trả xe tại địa điểm khác, tình trạng tốt', '["https://example.com/return/h6-1.jpg","https://example.com/return/h6-2.jpg"]', '2024-11-21 10:00:00'),

-- Contract 7: Normal return (will have traffic violation later)
(7, 'Pickup', 3, '2024-10-20 09:30:00', 41000, 100, 'Xe tốt', '["https://example.com/pickup/h7-1.jpg"]', '2024-10-20 09:30:00'),
(7, 'Return', 3, '2024-10-25 18:30:00', 41650, 100, 'Trả đúng hạn, xe trong tình trạng tốt', '["https://example.com/return/h7-1.jpg"]', '2024-10-25 18:30:00');

-- 12. RETURN FEES
INSERT INTO return_fees (contract_id, handover_id, is_late, hours_late, late_fee, has_damage, damage_description, damage_fee, is_different_location, one_way_fee, total_fees, created_at) VALUES
-- Contract 4: No fees
(4, 5, FALSE, 0, 0, FALSE, NULL, 0, FALSE, 0, 0, '2024-11-05 18:30:00'),

-- Contract 5: Late + Damage
(5, 7, TRUE, 17.5, 875000, TRUE, 'Trầy xước cửa xe phụ bên phải, kích thước 15cm', 2000000, FALSE, 0, 2875000, '2024-11-16 10:45:00'),

-- Contract 6: One-way fee
(6, 9, FALSE, 0, 0, FALSE, NULL, 0, TRUE, 1200000, 1200000, '2024-11-21 10:15:00'),

-- Contract 7: No fees (but will have traffic violation)
(7, 11, FALSE, 0, 0, FALSE, NULL, 0, FALSE, 0, 0, '2024-10-25 18:45:00');

-- 13. DEPOSIT HOLDS
INSERT INTO deposit_holds (contract_id, deposit_amount, deducted_at_return, hold_start_date, hold_end_date, status, created_at) VALUES
-- Contract 4: Ready for refund (no violations)
(4, 8000000, 0, '2024-11-05 18:30:00', '2024-11-19 18:30:00', 'Ready', '2024-11-05 18:30:00'),

-- Contract 5: Ready for refund (fees deducted)
(5, 10000000, 2875000, '2024-11-16 10:45:00', '2024-11-30 10:45:00', 'Ready', '2024-11-16 10:45:00'),

-- Contract 6: Ready for refund (one-way fee deducted)
(6, 11000000, 1200000, '2024-11-21 10:15:00', '2024-12-05 10:15:00', 'Ready', '2024-11-21 10:15:00'),

-- Contract 7: Holding (waiting for traffic violation check)
(7, 7500000, 0, '2024-10-25 18:45:00', '2024-11-08 18:45:00', 'Holding', '2024-10-25 18:45:00');

-- 14. TRAFFIC VIOLATIONS
INSERT INTO traffic_violations (hold_id, contract_id, violation_type, violation_date, fine_amount, description, evidence_url, status, created_at) VALUES
-- Contract 7: Traffic violations during rental period
(4, 7, 'Vượt đèn đỏ', '2024-10-22 15:30:00', 1000000, 'Vượt đèn đỏ tại ngã tư Lê Lợi - Nguyễn Huệ', 'https://example.com/violations/v1.jpg', 'Confirmed', '2024-11-02 09:00:00'),
(4, 7, 'Đi quá tốc độ', '2024-10-24 08:45:00', 800000, 'Vượt tốc độ cho phép 20km/h trên đường Võ Văn Kiệt', 'https://example.com/violations/v2.jpg', 'Confirmed', '2024-11-03 10:30:00'),

-- Pending violation
(4, 7, 'Đậu xe sai quy định', '2024-10-23 14:20:00', 500000, 'Đậu xe trên vỉa hè đường Hai Bà Trưng', 'https://example.com/violations/v3.jpg', 'Pending', '2024-11-05 11:00:00');

-- 15. REFUNDS
INSERT INTO refunds (hold_id, contract_id, customer_id, original_deposit, deducted_at_return, traffic_fines, refund_amount, refund_method, status, processed_at, created_at) VALUES
-- Contract 4: Full refund completed
(1, 4, 8, 8000000, 0, 0, 8000000, 'Transfer', 'Completed', '2024-11-20 10:00:00', '2024-11-19 18:30:00'),

-- Contract 5: Partial refund completed (fees deducted)
(2, 5, 9, 10000000, 2875000, 0, 7125000, 'Transfer', 'Completed', '2024-12-01 14:30:00', '2024-11-30 10:45:00'),

-- Contract 6: Partial refund pending
(3, 6, 10, 11000000, 1200000, 0, 9800000, 'Transfer', 'Pending', NULL, '2024-12-04 10:15:00');

-- 16. SUPPORT TICKETS
INSERT INTO support_tickets (ticket_number, customer_id, category, subject, description, status, assigned_to, created_at) VALUES
-- Open tickets
('TK-2024-0001', 5, 'Booking', 'Thay đổi thời gian thuê xe', 'Em muốn thay đổi thời gian nhận xe từ 9h sáng sang 14h chiều được không ạ?', 'Open', NULL, '2024-12-04 08:30:00'),
('TK-2024-0002', 6, 'Vehicle', 'Hỏi về tình trạng xe', 'Xe Toyota Camry còn mới không ạ? Em muốn thuê xe mới nhất có thể.', 'Open', NULL, '2024-12-04 09:15:00'),
('TK-2024-0003', NULL, 'General', 'Hỏi về giấy tờ cần thiết', 'Cho em hỏi thuê xe cần giấy tờ gì ạ? Em chưa có GPLX lâu năm.', 'Open', NULL, '2024-12-04 10:00:00'),

-- In Progress tickets
('TK-2024-0004', 7, 'Payment', 'Vấn đề thanh toán tiền cọc', 'Em chuyển khoản tiền cọc rồi nhưng chưa thấy cập nhật trạng thái thanh toán.', 'In_Progress', 2, '2024-12-03 14:20:00'),
('TK-2024-0005', 8, 'Booking', 'Yêu cầu hủy booking', 'Em có việc đột xuất không thể thuê xe được, muốn hủy booking và hoàn tiền.', 'In_Progress', 3, '2024-12-03 16:45:00'),

-- Resolved tickets
('TK-2024-0006', 9, 'Vehicle', 'Xe bị hỏng giữa đường', 'Em đang thuê xe mà xe bị chết máy giữa đường, cần hỗ trợ gấp!', 'Resolved', 2, '2024-12-02 11:30:00'),
('TK-2024-0007', 10, 'Payment', 'Hỏi về phí trả xe khác địa điểm', 'Nếu em nhận xe ở Q1 mà trả ở Tân Bình thì tính phí thế nào ạ?', 'Resolved', 3, '2024-12-01 09:00:00'),
('TK-2024-0008', 11, 'General', 'Hỏi về chính sách hoàn tiền cọc', 'Cho em hỏi tiền cọc hoàn sau bao lâu và có trường hợp nào bị giữ cọc không ạ?', 'Resolved', 2, '2024-11-30 15:20:00'),

-- Closed tickets (with ratings)
('TK-2024-0009', 5, 'Booking', 'Hỏi về thời gian thuê tối thiểu', 'Em chỉ muốn thuê 1 ngày được không ạ?', 'Closed', 2, '2024-11-28 10:00:00'),
('TK-2024-0010', 6, 'Vehicle', 'Xe có bluetooth không?', 'Em muốn thuê xe có kết nối bluetooth để nghe nhạc.', 'Closed', 3, '2024-11-27 14:30:00');

-- 17. SUPPORT MESSAGES
INSERT INTO support_messages (ticket_id, sender_id, message_text, rating, created_at) VALUES
-- Ticket 1: Open (no response yet)
(1, 5, 'Em muốn thay đổi thời gian nhận xe từ 9h sáng sang 14h chiều được không ạ?', NULL, '2024-12-04 08:30:00'),

-- Ticket 2: Open (no response yet)
(2, 6, 'Xe Toyota Camry còn mới không ạ? Em muốn thuê xe mới nhất có thể.', NULL, '2024-12-04 09:15:00'),

-- Ticket 3: Open (guest inquiry)
(3, 1, 'Cho em hỏi thuê xe cần giấy tờ gì ạ? Em chưa có GPLX lâu năm.', NULL, '2024-12-04 10:00:00'),

-- Ticket 4: In Progress (conversation ongoing)
(4, 7, 'Em chuyển khoản tiền cọc rồi nhưng chưa thấy cập nhật trạng thái thanh toán.', NULL, '2024-12-03 14:20:00'),
(4, 2, 'Chào anh/chị, cho em xin mã giao dịch để kiểm tra ạ.', NULL, '2024-12-03 14:35:00'),
(4, 7, 'Mã GD là: 1234567890. Em chuyển lúc 10h sáng nay.', NULL, '2024-12-03 14:50:00'),
(4, 2, 'Dạ, em đã tìm thấy. Hệ thống sẽ cập nhật trong 15 phút nữa ạ.', NULL, '2024-12-03 15:00:00'),

-- Ticket 5: In Progress
(5, 8, 'Em có việc đột xuất không thể thuê xe được, muốn hủy booking và hoàn tiền.', NULL, '2024-12-03 16:45:00'),
(5, 3, 'Chào anh/chị, booking của anh/chị đã được duyệt chưa ạ?', NULL, '2024-12-03 17:00:00'),
(5, 8, 'Chưa ạ, vẫn đang chờ duyệt.', NULL, '2024-12-03 17:15:00'),

-- Ticket 6: Resolved (emergency handled)
(6, 9, 'Em đang thuê xe mà xe bị chết máy giữa đường, cần hỗ trợ gấp!', NULL, '2024-12-02 11:30:00'),
(6, 2, 'Em ở đâu ạ? Bên mình sẽ cử nhân viên đến hỗ trợ ngay!', NULL, '2024-12-02 11:32:00'),
(6, 9, 'Em đang ở đường Võ Văn Tần gần ngã tư với Hai Bà Trưng ạ.', NULL, '2024-12-02 11:35:00'),
(6, 2, 'Nhân viên đang trên đường đến, khoảng 15 phút nữa sẽ đến nơi ạ.', NULL, '2024-12-02 11:40:00'),
(6, 9, 'Cảm ơn anh/chị nhiều ạ!', NULL, '2024-12-02 12:00:00'),
(6, 2, 'Vấn đề đã được xử lý. Xe đã hoạt động trở lại bình thường ạ.', NULL, '2024-12-02 12:30:00'),

-- Ticket 7: Resolved
(7, 10, 'Nếu em nhận xe ở Q1 mà trả ở Tân Bình thì tính phí thế nào ạ?', NULL, '2024-12-01 09:00:00'),
(7, 3, 'Chào anh/chị, phí trả xe khác địa điểm là 15% trên tổng tiền thuê ạ. Ví dụ tổng tiền thuê 10 triệu thì phí thêm là 1.5 triệu.', NULL, '2024-12-01 09:15:00'),
(7, 10, 'Em hiểu rồi, cảm ơn anh/chị.', NULL, '2024-12-01 09:30:00'),

-- Ticket 8: Resolved
(8, 11, 'Cho em hỏi tiền cọc hoàn sau bao lâu và có trường hợp nào bị giữ cọc không ạ?', NULL, '2024-11-30 15:20:00'),
(8, 2, 'Chào anh/chị, tiền cọc sẽ được giữ 14 ngày để kiểm tra vi phạm giao thông. Sau 14 ngày nếu không có vi phạm sẽ hoàn đầy đủ. Nếu có vi phạm sẽ trừ tiền phạt từ cọc và hoàn lại phần còn lại ạ.', NULL, '2024-11-30 15:35:00'),
(8, 11, 'Vậy nếu có hư hỏng xe thì sao ạ?', NULL, '2024-11-30 15:50:00'),
(8, 2, 'Nếu có hư hỏng sẽ tính phí ngay khi trả xe và trừ vào cọc luôn ạ. Còn vi phạm giao thông thì phải đợi 14 ngày mới biết có vi phạm hay không.', NULL, '2024-11-30 16:00:00'),
(8, 11, 'Em hiểu rồi, cảm ơn nhiều ạ!', NULL, '2024-11-30 16:15:00'),

-- Ticket 9: Closed with rating
(9, 5, 'Em chỉ muốn thuê 1 ngày được không ạ?', NULL, '2024-11-28 10:00:00'),
(9, 2, 'Dạ được ạ, bên mình cho thuê tối thiểu 1 ngày. Anh/chị có thể đặt xe ngay trên website ạ.', NULL, '2024-11-28 10:15:00'),
(9, 5, 'Cảm ơn anh/chị!', NULL, '2024-11-28 10:30:00'),
(9, 5, 'Nhân viên tư vấn nhiệt tình, giải đáp nhanh chóng!', 5, '2024-11-28 11:00:00'),

-- Ticket 10: Closed with rating
(10, 6, 'Em muốn thuê xe có kết nối bluetooth để nghe nhạc.', NULL, '2024-11-27 14:30:00'),
(10, 3, 'Chào anh/chị, hầu hết các xe đời mới của bên mình đều có bluetooth ạ. Anh/chị có thể xem thông tin chi tiết từng xe trên trang danh sách xe.', NULL, '2024-11-27 14:45:00'),
(10, 6, 'Vậy xe Toyota Vios 2023 có không ạ?', NULL, '2024-11-27 15:00:00'),
(10, 3, 'Dạ có ạ, xe Vios 2023 đều có kết nối bluetooth và Apple CarPlay nữa ạ.', NULL, '2024-11-27 15:15:00'),
(10, 6, 'Tuyệt vời, cảm ơn nhiều!', NULL, '2024-11-27 15:30:00'),
(10, 6, 'Hỗ trợ tốt, thông tin đầy đủ!', 4, '2024-11-27 16:00:00');

-- 18. NOTIFICATIONS
INSERT INTO notifications (user_id, title, message, is_read, created_at) VALUES
-- Customer notifications
(5, 'Booking được phê duyệt', 'Booking HD-2024-0001 của bạn đã được phê duyệt. Vui lòng thanh toán tiền cọc để hoàn tất.', TRUE, '2024-12-09 10:00:00'),
(5, 'Thanh toán thành công', 'Thanh toán tiền cọc 3.000.000 VNĐ cho hợp đồng HD-2024-0001 thành công.', TRUE, '2024-12-09 14:30:00'),
(5, 'Nhắc nhở nhận xe', 'Bạn có lịch nhận xe vào ngày 10/12/2024 lúc 09:00. Vui lòng đến đúng giờ.', TRUE, '2024-12-09 18:00:00'),
(5, 'Ticket hỗ trợ mới', 'Yêu cầu hỗ trợ TK-2024-0001 của bạn đã được tiếp nhận.', FALSE, '2024-12-04 08:30:00'),

(6, 'Booking được phê duyệt', 'Booking HD-2024-0002 của bạn đã được phê duyệt.', TRUE, '2024-12-11 09:00:00'),
(6, 'Thanh toán thành công', 'Thanh toán tiền cọc 3.500.000 VNĐ cho hợp đồng HD-2024-0002 thành công.', TRUE, '2024-12-11 10:30:00'),
(6, 'Nhắc nhở trả xe', 'Hợp đồng HD-2024-0002 sẽ hết hạn vào ngày 15/12/2024 lúc 17:00. Vui lòng trả xe đúng hạn.', FALSE, '2024-12-14 08:00:00'),

(7, 'Booking được phê duyệt', 'Booking HD-2024-0003 của bạn đã được phê duyệt.', TRUE, '2024-12-14 14:00:00'),
(7, 'Thanh toán thành công', 'Thanh toán tiền cọc 7.800.000 VNĐ cho hợp đồng HD-2024-0003 thành công.', TRUE, '2024-12-14 16:00:00'),

(8, 'Hoàn tiền cọc', 'Tiền cọc 8.000.000 VNĐ cho hợp đồng HD-2024-0004 đã được hoàn vào tài khoản của bạn.', TRUE, '2024-11-20 10:00:00'),
(8, 'Ticket hỗ trợ đang xử lý', 'Yêu cầu hỗ trợ TK-2024-0005 của bạn đang được xử lý.', FALSE, '2024-12-03 17:00:00'),

(9, 'Phí phát sinh khi trả xe', 'Hợp đồng HD-2024-0005 có phí phát sinh 2.875.000 VNĐ (trễ hạn + hư hỏng). Số tiền đã được trừ vào cọc.', TRUE, '2024-11-16 11:00:00'),
(9, 'Hoàn tiền cọc', 'Tiền cọc còn lại 7.125.000 VNĐ cho hợp đồng HD-2024-0005 đã được hoàn.', TRUE, '2024-12-01 14:30:00'),

(10, 'Phí trả xe khác địa điểm', 'Hợp đồng HD-2024-0006 có phí trả xe khác địa điểm 1.200.000 VNĐ.', TRUE, '2024-11-21 10:30:00'),
(10, 'Tiền cọc chờ hoàn', 'Tiền cọc 9.800.000 VNĐ của bạn đang chờ xử lý hoàn tiền.', FALSE, '2024-12-04 10:15:00'),

(11, 'Vi phạm giao thông', 'Phát hiện vi phạm giao thông trong thời gian thuê xe HD-2024-0007. Tổng phạt: 2.300.000 VNĐ.', TRUE, '2024-11-05 09:00:00'),
(11, 'Đang kiểm tra vi phạm', 'Tiền cọc của bạn đang được giữ để kiểm tra vi phạm giao thông (14 ngày).', TRUE, '2024-10-25 19:00:00'),

-- Staff notifications
(2, 'Booking mới cần duyệt', 'Có 4 booking mới đang chờ phê duyệt.', FALSE, '2024-12-04 16:30:00'),
(2, 'Nhắc nhở bàn giao xe', 'Hợp đồng HD-2024-0001 có lịch bàn giao xe vào 10/12/2024 lúc 09:00.', TRUE, '2024-12-09 18:00:00'),
(2, 'Ticket hỗ trợ mới', 'Có 3 ticket hỗ trợ mới cần xử lý.', FALSE, '2024-12-04 10:00:00'),

(3, 'Xe cần bảo trì', 'Xe 30AB-11111 đang ở trạng thái Maintenance cần kiểm tra.', FALSE, '2024-12-03 09:00:00'),
(3, 'Nhắc nhở nhận xe trả', 'Hợp đồng HD-2024-0002 có lịch nhận xe trả vào 15/12/2024 lúc 17:00.', FALSE, '2024-12-14 08:00:00'),

-- Admin notifications
(1, 'Báo cáo doanh thu tuần', 'Doanh thu tuần này: 125.000.000 VNĐ. Tăng 15% so với tuần trước.', FALSE, '2024-12-02 09:00:00'),
(1, 'Xe cần kiểm tra', '2 xe đang trong trạng thái Maintenance cần xử lý.', FALSE, '2024-12-03 09:00:00'),
(1, 'Giấy tờ chờ duyệt', 'Có 2 bộ giấy tờ khách hàng đang chờ phê duyệt.', FALSE, '2024-12-04 08:00:00');

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

-- Update vehicle status khi tạo contract
CREATE TRIGGER trg_contract_created
AFTER INSERT ON contracts
FOR EACH ROW
BEGIN
    UPDATE vehicles
    SET status = 'Rented'
    WHERE vehicle_id = NEW.vehicle_id;
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
    SET status = 'Ready'
    WHERE status = 'Holding'
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
WHERE h.status IN ('Holding', 'Ready');

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

