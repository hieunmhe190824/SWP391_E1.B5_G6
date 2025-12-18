/**
 * Document Upload - Image Preview Functionality
 * Handles immediate image preview when uploading documents
 */

document.addEventListener('DOMContentLoaded', function() {
    const imageFileInput = document.querySelector('input[name="imageFile"]');
    
    if (imageFileInput) {
        // Create preview container if it doesn't exist
        let previewContainer = document.getElementById('image-preview-container');
        if (!previewContainer) {
            previewContainer = document.createElement('div');
            previewContainer.id = 'image-preview-container';
            previewContainer.className = 'image-preview-container';
            previewContainer.style.cssText = `
                margin-top: 1rem;
                padding: 1rem;
                border: 2px dashed #e5e7eb;
                border-radius: 8px;
                background: #f9fafb;
                display: none;
                text-align: center;
            `;
            
            // Insert after the file input's parent form-group
            const formGroup = imageFileInput.closest('.form-group');
            if (formGroup) {
                formGroup.parentNode.insertBefore(previewContainer, formGroup.nextSibling);
            }
        }
        
        // Handle file selection
        imageFileInput.addEventListener('change', function(e) {
            const file = e.target.files[0];
            
            if (file) {
                // Check file size (5MB limit)
                const MAX_SIZE = 5 * 1024 * 1024; // 5MB in bytes
                if (file.size > MAX_SIZE) {
                    alert('Kích thước tệp quá lớn. Vui lòng chọn tệp nhỏ hơn 5MB.\nFile too large. Please choose a file smaller than 5MB.');
                    e.target.value = ''; // Clear the input
                    previewContainer.style.display = 'none';
                    return;
                }

                // Check if it's an image or PDF
                const fileType = file.type;
                const fileName = file.name.toLowerCase();
                
                if (fileType.startsWith('image/')) {
                    // Show image preview
                    const reader = new FileReader();
                    
                    reader.onload = function(event) {
                        previewContainer.innerHTML = `
                            <div style="margin-bottom: 0.5rem;">
                                <strong style="color: #374151;">Xem trước ảnh:</strong>
                            </div>
                            <img src="${event.target.result}" 
                                 alt="Preview" 
                                 style="max-width: 100%; max-height: 400px; border-radius: 8px; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);">
                            <div style="margin-top: 0.5rem; color: #6b7280; font-size: 0.875rem;">
                                ${file.name} (${formatFileSize(file.size)})
                            </div>
                        `;
                        previewContainer.style.display = 'block';
                    };
                    
                    reader.readAsDataURL(file);
                } else if (fileName.endsWith('.pdf')) {
                    // Show PDF indicator
                    previewContainer.innerHTML = `
                        <div style="display: flex; flex-direction: column; align-items: center; gap: 0.5rem;">
                            <svg width="64" height="64" viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
                                <path d="M24 20H40M24 28H40M24 36H32" stroke="#3b82f6" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                                <path d="M42.6667 8H21.3333C18.3878 8 16 10.3878 16 13.3333V50.6667C16 53.6122 18.3878 56 21.3333 56H42.6667C45.6122 56 48 53.6122 48 50.6667V13.3333C48 10.3878 45.6122 8 42.6667 8Z" stroke="#3b82f6" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                            </svg>
                            <div style="color: #374151; font-weight: 500;">Tệp PDF đã chọn</div>
                            <div style="color: #6b7280; font-size: 0.875rem;">
                                ${file.name} (${formatFileSize(file.size)})
                            </div>
                        </div>
                    `;
                    previewContainer.style.display = 'block';
                } else {
                    // Unsupported file type
                    previewContainer.innerHTML = `
                        <div style="color: #ef4444;">
                            Loại tệp không được hỗ trợ. Vui lòng chọn ảnh hoặc PDF.
                        </div>
                    `;
                    previewContainer.style.display = 'block';
                }
            } else {
                // No file selected, hide preview
                previewContainer.style.display = 'none';
            }
        });
    }
});

/**
 * Format file size to human-readable format
 */
function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
}
