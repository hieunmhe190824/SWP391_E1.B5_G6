// Main JavaScript for Car Rental System
console.log('main.js loaded!');

// Initialize on page load
document.addEventListener('DOMContentLoaded', function () {
    console.log('Car Rental System Initialized');

    // Initialize tooltips if any
    initializeTooltips();

    // Initialize modals if any
    initializeModals();

    // Initialize delete confirmation modal
    initializeDeleteModal();

    // Add confirmation to delete buttons
    addDeleteConfirmations();

    // Initialize dropdown menu
    initializeDropdown();
});

// Initialize tooltips
function initializeTooltips() {
    const tooltips = document.querySelectorAll('[data-tooltip]');
    tooltips.forEach(element => {
        element.addEventListener('mouseenter', showTooltip);
        element.addEventListener('mouseleave', hideTooltip);
    });
}

function showTooltip(event) {
    const text = event.target.getAttribute('data-tooltip');
    const tooltip = document.createElement('div');
    tooltip.className = 'tooltip';
    tooltip.textContent = text;
    document.body.appendChild(tooltip);

    const rect = event.target.getBoundingClientRect();
    tooltip.style.top = (rect.top - tooltip.offsetHeight - 5) + 'px';
    tooltip.style.left = (rect.left + rect.width / 2 - tooltip.offsetWidth / 2) + 'px';
}

function hideTooltip() {
    const tooltip = document.querySelector('.tooltip');
    if (tooltip) {
        tooltip.remove();
    }
}

// Initialize modals
function initializeModals() {
    const modalTriggers = document.querySelectorAll('[data-modal]');
    modalTriggers.forEach(trigger => {
        trigger.addEventListener('click', function (e) {
            e.preventDefault();
            const modalId = this.getAttribute('data-modal');
            openModal(modalId);
        });
    });

    const modalClosers = document.querySelectorAll('.modal-close');
    modalClosers.forEach(closer => {
        closer.addEventListener('click', closeModal);
    });
}

function openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.add('active');
    }
}

function closeModal() {
    const modals = document.querySelectorAll('.modal');
    modals.forEach(modal => {
        modal.classList.remove('active');
    });
}

// Add confirmation to delete actions using modal
function addDeleteConfirmations() {
    const deleteForms = document.querySelectorAll('form[data-delete-form]');
    const deleteButtons = document.querySelectorAll('[data-delete-trigger]');

    // Handle forms with data-delete-form attribute
    deleteForms.forEach(form => {
        const submitButton = form.querySelector('button[type="submit"]');
        if (submitButton) {
            submitButton.addEventListener('click', function (e) {
                e.preventDefault();
                const formId = form.id || 'deleteForm_' + Date.now();
                if (!form.id) form.id = formId;
                showDeleteModal(form);
            });
        }
    });

    // Handle buttons with data-delete-trigger attribute
    deleteButtons.forEach(button => {
        button.addEventListener('click', function (e) {
            e.preventDefault();
            const formId = this.getAttribute('data-delete-trigger');
            const form = document.getElementById(formId) || this.closest('form');
            if (form) {
                showDeleteModal(form);
            }
        });
    });
}

// Store current form being deleted
let currentDeleteForm = null;

// Show delete confirmation modal
function showDeleteModal(form) {
    const modal = document.getElementById('deleteConfirmModal');
    if (!modal) {
        console.error('Delete modal not found');
        return;
    }

    // Store form reference
    currentDeleteForm = form;

    // Get custom message if available
    const customMessage = form.getAttribute('data-delete-message') ||
        'Bạn có chắc chắn muốn xóa mục này? Hành động này không thể hoàn tác.';

    // Update modal message
    const messageElement = modal.querySelector('.delete-modal-message');
    if (messageElement) {
        messageElement.textContent = customMessage;
    }

    // Show modal
    modal.classList.add('active');
    document.body.style.overflow = 'hidden'; // Prevent background scrolling
}

// Close delete modal
function closeDeleteModal() {
    const modal = document.getElementById('deleteConfirmModal');
    if (modal) {
        modal.classList.remove('active');
        document.body.style.overflow = ''; // Restore scrolling
        currentDeleteForm = null;
    }
}

// Initialize delete modal event listeners (called once on page load)
function initializeDeleteModal() {
    const modal = document.getElementById('deleteConfirmModal');
    if (!modal) return;

    // Confirm button
    const confirmBtn = modal.querySelector('.delete-modal-confirm');
    if (confirmBtn) {
        confirmBtn.addEventListener('click', function () {
            if (currentDeleteForm) {
                // IMPORTANT: Save reference before closing modal
                // closeDeleteModal() sets currentDeleteForm to null
                const formToSubmit = currentDeleteForm;
                closeDeleteModal();
                formToSubmit.submit();
            }
        });
    }

    // Cancel button
    const cancelBtn = modal.querySelector('.delete-modal-cancel');
    if (cancelBtn) {
        cancelBtn.addEventListener('click', closeDeleteModal);
    }

    // Overlay click
    const overlay = modal.querySelector('.delete-modal-overlay');
    if (overlay) {
        overlay.addEventListener('click', closeDeleteModal);
    }

    // Escape key
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') {
            const modal = document.getElementById('deleteConfirmModal');
            if (modal && modal.classList.contains('active')) {
                closeDeleteModal();
            }
        }
    });
}

// Form validation helper
function validateForm(formId) {
    const form = document.getElementById(formId);
    if (!form) return false;

    const inputs = form.querySelectorAll('input[required], select[required], textarea[required]');
    let isValid = true;

    inputs.forEach(input => {
        if (!input.value.trim()) {
            input.classList.add('error');
            isValid = false;
        } else {
            input.classList.remove('error');
        }
    });

    return isValid;
}

// Date range picker helper
function calculateDays(startDate, endDate) {
    const start = new Date(startDate);
    const end = new Date(endDate);
    const diffTime = Math.abs(end - start);
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return diffDays;
}

// Price calculator
function calculateTotalPrice(dailyRate, days) {
    return (parseFloat(dailyRate) * parseInt(days)).toFixed(2);
}

// Show alert message
function showAlert(message, type = 'info') {
    const alert = document.createElement('div');
    alert.className = `alert alert-${type}`;
    alert.textContent = message;

    const container = document.querySelector('.container');
    if (container) {
        container.insertBefore(alert, container.firstChild);

        setTimeout(() => {
            alert.remove();
        }, 5000);
    }
}

// Initialize dropdown menu
function initializeDropdown() {
    // Wait a bit to ensure DOM is fully loaded
    setTimeout(function () {
        const dropdownToggle = document.getElementById('userDropdownToggle');
        const dropdownMenu = document.getElementById('userDropdownMenu');

        if (dropdownToggle && dropdownMenu) {
            console.log('Dropdown initialized successfully');

            // Toggle dropdown when clicking on toggle
            dropdownToggle.addEventListener('click', function (e) {
                e.preventDefault();
                e.stopPropagation();
                console.log('Toggle clicked, current state:', dropdownMenu.classList.contains('active'));
                dropdownMenu.classList.toggle('active');
                console.log('New state:', dropdownMenu.classList.contains('active'));
            });

            // Close dropdown when clicking outside
            document.addEventListener('click', function (e) {
                if (dropdownMenu && dropdownToggle) {
                    const isClickInside = dropdownToggle.contains(e.target) || dropdownMenu.contains(e.target);
                    if (!isClickInside && dropdownMenu.classList.contains('active')) {
                        console.log('Clicking outside, closing dropdown');
                        dropdownMenu.classList.remove('active');
                    }
                }
            });

            // Close dropdown when clicking on a menu item
            const menuItems = dropdownMenu.querySelectorAll('a');
            menuItems.forEach(item => {
                item.addEventListener('click', function () {
                    console.log('Menu item clicked, closing dropdown');
                    dropdownMenu.classList.remove('active');
                });
            });
        } else {
            console.error('Dropdown elements not found:', {
                toggle: dropdownToggle,
                menu: dropdownMenu
            });
        }
    }, 100);
}

// Export functions for use in other scripts
window.CarRental = {
    validateForm,
    calculateDays,
    calculateTotalPrice,
    showAlert,
    openModal,
    closeModal,
    showDeleteModal,
    closeDeleteModal
};
