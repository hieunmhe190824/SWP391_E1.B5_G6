// Form Validation for Car Rental System

// Email validation
function validateEmail(email) {
    const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return re.test(email);
}

// Check email exists (real-time duplicate check)
function checkEmailExists(email, emailElement) {
    // Chỉ check nếu email hợp lệ
    if (!validateEmail(email)) {
        return;
    }
    
    // Gọi API để check email đã tồn tại chưa
    fetch('/auth/check-email?email=' + encodeURIComponent(email))
        .then(response => response.json())
        .then(data => {
            if (data.exists) {
                showError(emailElement, 'Email này đã được sử dụng');
            } else {
                clearError(emailElement);
            }
        })
        .catch(error => {
            console.error('Error checking email:', error);
            // Không hiển thị lỗi nếu API call thất bại để không làm gián đoạn UX
        });
}

// Phone number validation
// Kiểm tra số điện thoại phải là số có 10-11 chữ số, không được chữ cái và ký tự đặc biệt
function validatePhone(phone) {
    if (!phone) return false;
    
    // Loại bỏ khoảng trắng và dấu gạch ngang để kiểm tra
    const phoneDigits = phone.replace(/[\s\-]/g, '');
    
    // Kiểm tra chỉ chứa số và độ dài 10-11
    const re = /^[0-9]{10,11}$/;
    if (!re.test(phoneDigits)) {
        return false;
    }
    
    // Kiểm tra input gốc không chứa chữ cái (a-z, A-Z)
    if (/[a-zA-Z]/.test(phone)) {
        return false;
    }
    
    return true;
}

// Password validation
function validatePassword(password) {
    // At least 8 characters, 1 uppercase, 1 lowercase, 1 number
    // Cho phép ký tự đặc biệt
    const re = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/;
    return re.test(password);
}

// License plate validation
function validateLicensePlate(plate) {
    // Vietnamese license plate format
    const re = /^[0-9]{2}[A-Z]{1,2}-[0-9]{4,5}$/;
    return re.test(plate);
}

// Date validation (future dates only)
function validateFutureDate(date) {
    const selectedDate = new Date(date);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    return selectedDate >= today;
}

// Registration form validation
// Chỉ validate các field NOT NULL trong database: full_name, email, phone, password_hash
// address có thể NULL nên không cần validate null
function validateRegistrationForm() {
    let isValid = true;

    // Full Name - NOT NULL trong DB (full_name)
    const fullName = document.getElementById('fullName');
    if (fullName && (!fullName.value || fullName.value.trim().length === 0)) {
        showError(fullName, 'Vui lòng nhập họ và tên');
        isValid = false;
    } else if (fullName) {
        clearError(fullName);
    }

    // Email - NOT NULL trong DB
    const email = document.getElementById('email');
    if (email && (!email.value || email.value.trim().length === 0)) {
        showError(email, 'Vui lòng nhập email');
        isValid = false;
    } else if (email && !validateEmail(email.value)) {
        showError(email, 'Email không hợp lệ');
        isValid = false;
    } else if (email) {
        clearError(email);
    }

    // Phone - NOT NULL trong DB
    const phone = document.getElementById('phone');
    if (phone && (!phone.value || phone.value.trim().length === 0)) {
        showError(phone, 'Vui lòng nhập số điện thoại');
        isValid = false;
    } else if (phone && !validatePhone(phone.value)) {
        showError(phone, 'Số điện thoại phải có 10-11 chữ số');
        isValid = false;
    } else if (phone) {
        clearError(phone);
    }

    // Password - NOT NULL trong DB (password_hash)
    const password = document.getElementById('password');
    if (password && (!password.value || password.value.trim().length === 0)) {
        showError(password, 'Vui lòng nhập mật khẩu');
        isValid = false;
    } else if (password && !validatePassword(password.value)) {
        showError(password, 'Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường và số');
        isValid = false;
    } else if (password) {
        clearError(password);
    }

    // Confirm Password - Không có trong DB nhưng cần validate để đảm bảo password đúng
    const confirmPassword = document.getElementById('confirmPassword');
    if (confirmPassword && (!confirmPassword.value || confirmPassword.value.trim().length === 0)) {
        showError(confirmPassword, 'Vui lòng xác nhận mật khẩu');
        isValid = false;
    } else if (confirmPassword && password && confirmPassword.value !== password.value) {
        showError(confirmPassword, 'Mật khẩu xác nhận không khớp');
        isValid = false;
    } else if (confirmPassword) {
        clearError(confirmPassword);
    }

    // Address - Có thể NULL trong DB, không cần validate null

    return isValid;
}

// Booking form validation
function validateBookingForm() {
    let isValid = true;

    // Start date
    const startDate = document.getElementById('startDate');
    if (startDate && !validateFutureDate(startDate.value)) {
        showError(startDate, 'Start date must be today or in the future');
        isValid = false;
    } else if (startDate) {
        clearError(startDate);
    }

    // End date
    const endDate = document.getElementById('endDate');
    if (endDate && startDate) {
        const start = new Date(startDate.value);
        const end = new Date(endDate.value);

        if (end <= start) {
            showError(endDate, 'End date must be after start date');
            isValid = false;
        } else {
            clearError(endDate);
        }
    }

    return isValid;
}

// Show error message
function showError(element, message) {
    // Remove existing error class
    element.classList.remove('error');
    element.classList.add('error');

    // Remove existing error message
    const existingError = element.parentElement.querySelector('.error-message');
    if (existingError) {
        existingError.remove();
    }

    // Add new error message
    const errorDiv = document.createElement('div');
    errorDiv.className = 'error-message';
    errorDiv.textContent = message;
    element.parentElement.appendChild(errorDiv);
}

// Clear error message
function clearError(element) {
    element.classList.remove('error');

    const errorMessage = element.parentElement.querySelector('.error-message');
    if (errorMessage) {
        errorMessage.remove();
    }
}

// Real-time validation for individual fields
// Chỉ validate các field NOT NULL trong database
function setupRealTimeValidation() {
    const registrationForm = document.getElementById('registrationForm');
    if (!registrationForm) return;

    // Full Name validation - NOT NULL trong DB (full_name)
    const fullName = document.getElementById('fullName');
    if (fullName) {
        fullName.addEventListener('blur', function() {
            if (!this.value || this.value.trim().length === 0) {
                showError(this, 'Vui lòng nhập họ và tên');
            } else {
                clearError(this);
            }
        });
    }

    // Email validation - NOT NULL trong DB
    const email = document.getElementById('email');
    if (email) {
        let emailCheckTimeout;
        
        // Real-time validation khi người dùng đang gõ
        email.addEventListener('input', function() {
            const emailValue = this.value.trim();
            
            // Clear timeout trước đó để debounce
            clearTimeout(emailCheckTimeout);
            
            // Clear error trước
            clearError(this);
            
            // Nếu email rỗng hoặc không hợp lệ, không check duplicate
            if (!emailValue || !validateEmail(emailValue)) {
                return;
            }
            
            // Debounce: Đợi 500ms sau khi người dùng ngừng gõ mới check
            emailCheckTimeout = setTimeout(function() {
                checkEmailExists(emailValue, email);
            }, 500);
        });
        
        // Validation khi blur (rời khỏi field)
        email.addEventListener('blur', function() {
            clearTimeout(emailCheckTimeout);
            
            if (!this.value || this.value.trim().length === 0) {
                showError(this, 'Vui lòng nhập email');
            } else if (!validateEmail(this.value)) {
                showError(this, 'Email không hợp lệ');
            } else {
                // Check duplicate khi blur
                checkEmailExists(this.value.trim(), this);
            }
        });
    }

    // Phone validation - NOT NULL trong DB
    const phone = document.getElementById('phone');
    if (phone) {
        // Ngăn chặn nhập chữ cái và ký tự đặc biệt (chỉ cho phép số, khoảng trắng và dấu gạch ngang)
        phone.addEventListener('input', function(e) {
            // Cho phép số, khoảng trắng và dấu gạch ngang (sẽ được loại bỏ khi validate)
            this.value = this.value.replace(/[^0-9\s\-]/g, '');
        });
        
        // Ngăn chặn paste chữ cái và ký tự đặc biệt
        phone.addEventListener('paste', function(e) {
            e.preventDefault();
            const paste = (e.clipboardData || window.clipboardData).getData('text');
            const cleaned = paste.replace(/[^0-9\s\-]/g, '');
            this.value = cleaned;
        });
        
        phone.addEventListener('blur', function() {
            if (!this.value || this.value.trim().length === 0) {
                showError(this, 'Vui lòng nhập số điện thoại');
            } else if (!validatePhone(this.value)) {
                showError(this, 'Số điện thoại phải có 10-11 chữ số, không được chứa chữ cái và ký tự đặc biệt');
            } else {
                clearError(this);
            }
        });
    }

    // Password validation - NOT NULL trong DB (password_hash)
    const password = document.getElementById('password');
    if (password) {
        password.addEventListener('blur', function() {
            if (!this.value || this.value.trim().length === 0) {
                showError(this, 'Vui lòng nhập mật khẩu');
            } else if (!validatePassword(this.value)) {
                showError(this, 'Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường và số');
            } else {
                clearError(this);
            }
        });
    }

    // Confirm Password validation - Không có trong DB nhưng cần validate
    const confirmPassword = document.getElementById('confirmPassword');
    if (confirmPassword && password) {
        confirmPassword.addEventListener('blur', function() {
            if (!this.value || this.value.trim().length === 0) {
                showError(this, 'Vui lòng xác nhận mật khẩu');
            } else if (this.value !== password.value) {
                showError(this, 'Mật khẩu xác nhận không khớp');
            } else {
                clearError(this);
            }
        });

        // Also validate when password changes
        password.addEventListener('input', function() {
            if (confirmPassword.value && confirmPassword.value !== this.value) {
                showError(confirmPassword, 'Mật khẩu xác nhận không khớp');
            } else if (confirmPassword.value) {
                clearError(confirmPassword);
            }
        });
    }

    // Address - Có thể NULL trong DB, không cần validate null
}

// Real-time validation
document.addEventListener('DOMContentLoaded', function() {
    // Setup real-time validation for registration form
    setupRealTimeValidation();

    // Add submit validation to forms
    const registrationForm = document.getElementById('registrationForm');
    if (registrationForm) {
        registrationForm.addEventListener('submit', function(e) {
            if (!validateRegistrationForm()) {
                e.preventDefault();
            }
        });
    }

    const bookingForm = document.getElementById('bookingForm');
    if (bookingForm) {
        bookingForm.addEventListener('submit', function(e) {
            if (!validateBookingForm()) {
                e.preventDefault();
            }
        });
    }

    // Tự động ẩn các alert message sau 3 giây
    autoHideAlerts();
});

// Function để tự động ẩn các alert message sau 3 giây
function autoHideAlerts() {
    const alerts = document.querySelectorAll('.alert:not(.fade-out)');
    
    alerts.forEach(function(alert) {
        // Bỏ qua inline error messages (chỉ ẩn các alert chính)
        if (alert.classList.contains('error-message')) {
            return;
        }

        // Set timeout để ẩn sau 3 giây
        setTimeout(function() {
            // Thêm class fade-out để trigger animation
            alert.classList.add('fade-out');
            
            // Chỉ ẩn alert, không xóa parent để tránh mất các phần tử khác
            setTimeout(function() {
                alert.style.display = 'none';
            }, 500); // Đợi animation fade-out hoàn thành (0.5s)
        }, 3000); // 3 giây
    });
}
