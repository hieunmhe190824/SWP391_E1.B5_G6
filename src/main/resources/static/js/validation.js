// Form Validation for Car Rental System

// Email validation
function validateEmail(email) {
    const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return re.test(email);
}

// Phone number validation
function validatePhone(phone) {
    const re = /^[0-9]{10,11}$/;
    return re.test(phone.replace(/[\s\-]/g, ''));
}

// Password validation
function validatePassword(password) {
    // At least 8 characters, 1 uppercase, 1 lowercase, 1 number
    const re = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d]{8,}$/;
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
function validateRegistrationForm() {
    let isValid = true;

    // Email
    const email = document.getElementById('email');
    if (email && !validateEmail(email.value)) {
        showError(email, 'Please enter a valid email address');
        isValid = false;
    } else if (email) {
        clearError(email);
    }

    // Password
    const password = document.getElementById('password');
    if (password && !validatePassword(password.value)) {
        showError(password, 'Password must be at least 8 characters with uppercase, lowercase and number');
        isValid = false;
    } else if (password) {
        clearError(password);
    }

    // Confirm Password
    const confirmPassword = document.getElementById('confirmPassword');
    if (confirmPassword && confirmPassword.value !== password.value) {
        showError(confirmPassword, 'Passwords do not match');
        isValid = false;
    } else if (confirmPassword) {
        clearError(confirmPassword);
    }

    // Phone
    const phone = document.getElementById('phone');
    if (phone && !validatePhone(phone.value)) {
        showError(phone, 'Please enter a valid phone number');
        isValid = false;
    } else if (phone) {
        clearError(phone);
    }

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
    errorDiv.style.color = 'red';
    errorDiv.style.fontSize = '0.875rem';
    errorDiv.style.marginTop = '0.25rem';

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

// Real-time validation
document.addEventListener('DOMContentLoaded', function() {
    // Add real-time validation to forms
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
});
