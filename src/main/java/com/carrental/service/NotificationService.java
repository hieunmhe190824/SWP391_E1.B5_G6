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
     * Format currency
     */
    private String formatCurrency(BigDecimal amount) {
        return String.format("%,.0f", amount.doubleValue());
    }
}

