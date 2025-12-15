package com.carrental.service;

import com.carrental.model.Notification;
import com.carrental.model.User;
import com.carrental.repository.NotificationRepository;
import com.carrental.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Create notification for user
     */
    @Transactional
    public Notification createNotification(Long userId, String title, String message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        return notificationRepository.save(notification);
    }

    /**
     * Create notification for payment after return
     * Now uses Payment instead of Bill
     */
    @Transactional
    public Notification createPaymentNotification(Long userId, String contractNumber, String billNumber, BigDecimal totalAmount, BigDecimal depositAmount) {
        String title = "Yêu cầu thanh toán hóa đơn";
        String message = String.format(
            "Hợp đồng %s đã hoàn tất trả xe. Vui lòng thanh toán hóa đơn %s với tổng số tiền: %s VND.\n\n" +
            "Lưu ý: Chúng tôi vẫn đang giữ %s VND tiền cọc của bạn. Tiền cọc sẽ được hoàn lại sau 14 ngày nếu không có vi phạm giao thông hoặc hư hỏng phát sinh.",
            contractNumber,
            billNumber,
            formatCurrency(totalAmount),
            formatCurrency(depositAmount)
        );

        return createNotification(userId, title, message);
    }

    /**
     * Get all notifications for user
     */
    public List<Notification> getNotificationsByUserId(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Get unread notifications for user
     */
    public List<Notification> getUnreadNotificationsByUserId(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    /**
     * Get notification by ID
     */
    public Optional<Notification> getNotificationById(Long notificationId) {
        return notificationRepository.findById(notificationId);
    }

    /**
     * Mark notification as read
     */
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    /**
     * Mark all notifications as read for user
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unreadNotifications = getUnreadNotificationsByUserId(userId);
        for (Notification notification : unreadNotifications) {
            notification.setIsRead(true);
        }
        notificationRepository.saveAll(unreadNotifications);
    }

    /**
     * Get unread count for user
     */
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * Create notification for booking approval
     * Informs customer that booking is approved and requests deposit payment
     */
    @Transactional
    public Notification createBookingApprovalNotification(Long userId, Long bookingId, BigDecimal depositAmount) {
        String title = "Đơn đặt xe đã được duyệt";
        String message = String.format(
            "Đơn đặt xe #%d của bạn đã được duyệt thành công!\n\n" +
            "Vui lòng chuyển tiền cọc: %s VND để hoàn tất thủ tục.\n\n" +
            "Sau khi thanh toán cọc, hợp đồng sẽ được kích hoạt và bạn có thể nhận xe theo lịch đã đặt.",
            bookingId,
            formatCurrency(depositAmount)
        );

        return createNotification(userId, title, message);
    }

    /**
     * Create notification for booking rejection
     * Informs customer that booking is rejected with optional reason
     */
    @Transactional
    public Notification createBookingRejectionNotification(Long userId, Long bookingId, String reason) {
        String title = "Đơn đặt xe đã bị từ chối";
        String message = String.format(
            "Đơn đặt xe #%d của bạn đã bị từ chối.\n\n",
            bookingId
        );
        
        if (reason != null && !reason.trim().isEmpty()) {
            message += "Lý do: " + reason + "\n\n";
        }
        
        message += "Vui lòng liên hệ với chúng tôi nếu bạn có thắc mắc hoặc muốn đặt lại.";

        return createNotification(userId, title, message);
    }

    // ========== CONTRACT NOTIFICATIONS ==========
    
    /**
     * Create notification when contract is created
     */
    @Transactional
    public Notification createContractCreatedNotification(Long userId, String contractNumber, BigDecimal depositAmount) {
        String title = "Hợp đồng đã được tạo";
        String message = String.format(
            "Hợp đồng %s đã được tạo thành công!\n\n" +
            "Vui lòng thanh toán tiền cọc: %s VND để kích hoạt hợp đồng.\n\n" +
            "Sau khi thanh toán, bạn có thể nhận xe theo lịch đã đặt.",
            contractNumber,
            formatCurrency(depositAmount)
        );
        return createNotification(userId, title, message);
    }

    /**
     * Create notification when contract is activated (deposit paid)
     */
    @Transactional
    public Notification createContractActivatedNotification(Long userId, String contractNumber, String startDate, String endDate) {
        String title = "Hợp đồng đã được kích hoạt";
        String message = String.format(
            "Hợp đồng %s đã được kích hoạt thành công!\n\n" +
            "Thời gian thuê: %s đến %s\n\n" +
            "Vui lòng đến nhận xe đúng thời gian đã hẹn.",
            contractNumber,
            startDate,
            endDate
        );
        return createNotification(userId, title, message);
    }

    /**
     * Create notification when contract is cancelled
     */
    @Transactional
    public Notification createContractCancelledNotification(Long userId, String contractNumber, String reason) {
        String title = "Hợp đồng đã bị hủy";
        String message = String.format(
            "Hợp đồng %s đã bị hủy.\n\n",
            contractNumber
        );
        
        if (reason != null && !reason.trim().isEmpty()) {
            message += "Lý do: " + reason + "\n\n";
        }
        
        message += "Tiền cọc sẽ được hoàn lại trong vòng 3-5 ngày làm việc.";
        return createNotification(userId, title, message);
    }

    // ========== HANDOVER NOTIFICATIONS ==========
    
    /**
     * Create notification when vehicle is ready for pickup
     */
    @Transactional
    public Notification createVehicleReadyNotification(Long userId, String contractNumber, String vehicleName, String pickupLocation) {
        String title = "Xe đã sẵn sàng để nhận";
        String message = String.format(
            "Xe %s của hợp đồng %s đã sẵn sàng!\n\n" +
            "Địa điểm nhận xe: %s\n\n" +
            "Vui lòng mang theo CMND/CCCD và GPLX khi đến nhận xe.",
            vehicleName,
            contractNumber,
            pickupLocation
        );
        return createNotification(userId, title, message);
    }

    /**
     * Create notification when pickup is completed
     */
    @Transactional
    public Notification createPickupCompletedNotification(Long userId, String contractNumber, String vehicleName) {
        String title = "Đã nhận xe thành công";
        String message = String.format(
            "Bạn đã nhận xe %s (Hợp đồng %s) thành công!\n\n" +
            "Chúc bạn có chuyến đi an toàn và vui vẻ.\n\n" +
            "Vui lòng trả xe đúng hạn và giữ xe trong tình trạng tốt.",
            vehicleName,
            contractNumber
        );
        return createNotification(userId, title, message);
    }

    /**
     * Create notification when return is completed
     */
    @Transactional
    public Notification createReturnCompletedNotification(Long userId, String contractNumber, String billNumber) {
        String title = "Đã trả xe thành công";
        String message = String.format(
            "Bạn đã trả xe (Hợp đồng %s) thành công!\n\n" +
            "Hóa đơn %s đã được tạo. Vui lòng thanh toán để hoàn tất thủ tục.\n\n" +
            "Tiền cọc sẽ được giữ trong 14 ngày để kiểm tra vi phạm giao thông.",
            contractNumber,
            billNumber
        );
        return createNotification(userId, title, message);
    }

    // ========== SUPPORT NOTIFICATIONS ==========
    
    /**
     * Create notification when ticket is assigned to staff
     */
    @Transactional
    public Notification createTicketAssignedNotification(Long userId, String ticketNumber, String staffName) {
        String title = "Yêu cầu hỗ trợ đã được tiếp nhận";
        String message = String.format(
            "Yêu cầu hỗ trợ %s của bạn đã được giao cho %s.\n\n" +
            "Chúng tôi sẽ phản hồi trong thời gian sớm nhất.",
            ticketNumber,
            staffName
        );
        return createNotification(userId, title, message);
    }

    /**
     * Create notification when staff responds to ticket
     */
    @Transactional
    public Notification createTicketResponseNotification(Long userId, String ticketNumber) {
        String title = "Có phản hồi mới cho yêu cầu hỗ trợ";
        String message = String.format(
            "Nhân viên đã phản hồi yêu cầu hỗ trợ %s của bạn.\n\n" +
            "Vui lòng kiểm tra và phản hồi nếu cần thêm hỗ trợ.",
            ticketNumber
        );
        return createNotification(userId, title, message);
    }

    /**
     * Create notification when ticket is resolved
     */
    @Transactional
    public Notification createTicketResolvedNotification(Long userId, String ticketNumber) {
        String title = "Yêu cầu hỗ trợ đã được giải quyết";
        String message = String.format(
            "Yêu cầu hỗ trợ %s của bạn đã được đánh dấu là đã giải quyết.\n\n" +
            "Nếu vấn đề vẫn chưa được giải quyết, vui lòng liên hệ lại với chúng tôi.",
            ticketNumber
        );
        return createNotification(userId, title, message);
    }

    // ========== REFUND NOTIFICATIONS ==========
    
    /**
     * Create notification when refund is initiated
     */
    @Transactional
    public Notification createRefundInitiatedNotification(Long userId, String contractNumber, BigDecimal refundAmount) {
        String title = "Đang xử lý hoàn tiền cọc";
        String message = String.format(
            "Yêu cầu hoàn tiền cọc cho hợp đồng %s đang được xử lý.\n\n" +
            "Số tiền hoàn: %s VND\n\n" +
            "Tiền sẽ được chuyển vào tài khoản của bạn trong 3-5 ngày làm việc.",
            contractNumber,
            formatCurrency(refundAmount)
        );
        return createNotification(userId, title, message);
    }

    /**
     * Create notification when refund is completed
     */
    @Transactional
    public Notification createRefundCompletedNotification(Long userId, String contractNumber, BigDecimal refundAmount, String method) {
        String title = "Đã hoàn tiền cọc thành công";
        String message = String.format(
            "Tiền cọc cho hợp đồng %s đã được hoàn lại.\n\n" +
            "Số tiền: %s VND\n" +
            "Phương thức: %s\n\n" +
            "Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi!",
            contractNumber,
            formatCurrency(refundAmount),
            method.equals("Transfer") ? "Chuyển khoản" : "Tiền mặt"
        );
        return createNotification(userId, title, message);
    }

    /**
     * Create notification when traffic violation is detected
     */
    @Transactional
    public Notification createTrafficViolationNotification(Long userId, String contractNumber, String violationType, BigDecimal fineAmount) {
        String title = "Phát hiện vi phạm giao thông";
        String message = String.format(
            "Phát hiện vi phạm giao thông trong thời gian thuê xe (Hợp đồng %s).\n\n" +
            "Loại vi phạm: %s\n" +
            "Số tiền phạt: %s VND\n\n" +
            "Số tiền này sẽ được trừ vào tiền cọc của bạn.",
            contractNumber,
            violationType,
            formatCurrency(fineAmount)
        );
        return createNotification(userId, title, message);
    }

    /**
     * Format currency
     */
    private String formatCurrency(BigDecimal amount) {
        return String.format("%,.0f", amount.doubleValue());
    }
}

