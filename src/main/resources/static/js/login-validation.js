// Login Form Real-time Validation
// Kiểm tra email real-time và null check cho email và mật khẩu

document.addEventListener('DOMContentLoaded', function() {
    const emailInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');
    const loginForm = document.querySelector('form[th\\:action*="/auth/login"]') || 
                      document.querySelector('form[action*="/auth/login"]');

    // Email validation function
    function validateEmail(email) {
        if (!email || email.trim() === '') {
            return { valid: false, message: 'Email không được để trống' };
        }
        const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!re.test(email)) {
            return { valid: false, message: 'Email không đúng định dạng' };
        }
        return { valid: true, message: '' };
    }

    // Password validation function
    function validatePassword(password) {
        if (!password || password.trim() === '') {
            return { valid: false, message: 'Mật khẩu không được để trống' };
        }
        return { valid: true, message: '' };
    }

    // Show error message dưới input
    function showError(input, message) {
        // Remove existing error class
        input.classList.remove('error');
        input.classList.add('error');

        // Remove existing error message
        const existingError = input.parentElement.querySelector('.error-message');
        if (existingError) {
            existingError.remove();
        }

        // Add new error message
        const errorDiv = document.createElement('div');
        errorDiv.className = 'error-message';
        errorDiv.textContent = message;
        input.parentElement.appendChild(errorDiv);
    }

    // Clear error message
    function clearError(input) {
        input.classList.remove('error');
        const errorMessage = input.parentElement.querySelector('.error-message');
        if (errorMessage) {
            errorMessage.remove();
        }
    }

    // Real-time email validation
    if (emailInput) {
        emailInput.addEventListener('input', function() {
            const email = this.value.trim();
            const validation = validateEmail(email);
            
            if (email.length > 0) {
                if (validation.valid) {
                    clearError(this);
                } else {
                    showError(this, validation.message);
                }
            } else {
                // Clear error khi người dùng xóa hết
                clearError(this);
            }
        });

        emailInput.addEventListener('blur', function() {
            const email = this.value.trim();
            const validation = validateEmail(email);
            
            if (!validation.valid) {
                showError(this, validation.message);
            }
        });
    }

    // Real-time password validation
    if (passwordInput) {
        passwordInput.addEventListener('input', function() {
            const password = this.value;
            const validation = validatePassword(password);
            
            if (password.length > 0) {
                if (validation.valid) {
                    clearError(this);
                } else {
                    showError(this, validation.message);
                }
            } else {
                // Clear error khi người dùng xóa hết
                clearError(this);
            }
        });

        passwordInput.addEventListener('blur', function() {
            const password = this.value;
            const validation = validatePassword(password);
            
            if (!validation.valid) {
                showError(this, validation.message);
            }
        });
    }

    // Form submission validation
    if (loginForm) {
        loginForm.addEventListener('submit', function(e) {
            let isValid = true;

            // Validate email
            if (emailInput) {
                const email = emailInput.value.trim();
                const emailValidation = validateEmail(email);
                if (!emailValidation.valid) {
                    showError(emailInput, emailValidation.message);
                    isValid = false;
                }
            }

            // Validate password
            if (passwordInput) {
                const password = passwordInput.value;
                const passwordValidation = validatePassword(password);
                if (!passwordValidation.valid) {
                    showError(passwordInput, passwordValidation.message);
                    isValid = false;
                }
            }

            if (!isValid) {
                e.preventDefault();
                return false;
            }
        });
    }
});

