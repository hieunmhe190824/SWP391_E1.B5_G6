/**
 * Profile Page - Force reload images after document upload
 * This script ensures that newly uploaded document images are displayed immediately
 * when redirected back to the profile page after adding/editing a document
 */

document.addEventListener('DOMContentLoaded', function () {
    // Check if we just came from a document add/edit operation
    const hasSuccess = document.querySelector('.alert-success') !== null;
    const successMessage = hasSuccess ? document.querySelector('.alert-success')?.textContent?.trim() : '';

    // Check if the success message is about adding or updating a document
    const isDocumentOperation = successMessage &&
        (successMessage.includes('Thêm giấy tờ thành công') ||
            successMessage.includes('Cập nhật giấy tờ thành công'));

    if (isDocumentOperation) {
        // Check if we've already reloaded (to prevent infinite loop)
        const hasReloaded = sessionStorage.getItem('documentReloaded');

        if (!hasReloaded) {
            // Mark that we're about to reload
            sessionStorage.setItem('documentReloaded', 'true');

            // Force a hard reload to bypass all caches
            // Use a small delay to ensure the flash message is visible briefly
            setTimeout(function () {
                window.location.reload(true);
            }, 100);

            return; // Exit early since we're reloading
        } else {
            // We've already reloaded, clear the flag
            sessionStorage.removeItem('documentReloaded');
        }
    }

    // Additional cache-busting for all document images
    // This runs on every page load to ensure fresh images
    const allDocImages = document.querySelectorAll('.document-image, .document-image-wrapper img');
    allDocImages.forEach(function (img) {
        if (img.src && img.src.includes('/uploads/')) {
            // Add timestamp to prevent caching
            const url = new URL(img.src);
            url.searchParams.set('t', new Date().getTime());
            img.src = url.toString();
        }
    });

    // Also handle lazy-loaded images
    const observer = new MutationObserver(function (mutations) {
        mutations.forEach(function (mutation) {
            mutation.addedNodes.forEach(function (node) {
                if (node.nodeType === 1) { // Element node
                    const images = node.querySelectorAll ? node.querySelectorAll('img[src*="/uploads/"]') : [];
                    images.forEach(function (img) {
                        const url = new URL(img.src);
                        url.searchParams.set('t', new Date().getTime());
                        img.src = url.toString();
                    });
                }
            });
        });
    });

    // Observe the documents container for changes
    const documentsContainer = document.querySelector('.documents-grid');
    if (documentsContainer) {
        observer.observe(documentsContainer, { childList: true, subtree: true });
    }
});
