package com.carrental.service;

import com.carrental.model.SupportMessage;
import com.carrental.model.SupportTicket;
import com.carrental.model.SupportTicket.TicketStatus;
import com.carrental.model.User;
import com.carrental.repository.SupportMessageRepository;
import com.carrental.repository.SupportTicketRepository;
import com.carrental.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class SupportService {

    @Autowired
    private SupportTicketRepository supportTicketRepository;
    
    @Autowired
    private SupportMessageRepository supportMessageRepository;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    /**
     * Get all tickets sorted by newest first (for staff/admin)
     */
    public List<SupportTicket> getAllTickets() {
        return supportTicketRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Get ticket by ID
     */
    public Optional<SupportTicket> getTicketById(Long id) {
        return supportTicketRepository.findById(id);
    }

    /**
     * Get tickets by customer, sorted by newest first
     */
    public List<SupportTicket> getTicketsByCustomer(Long customerId) {
        return supportTicketRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    /**
     * Get tickets assigned to a staff member
     */
    public List<SupportTicket> getTicketsByStaff(Long staffId) {
        return supportTicketRepository.findByAssignedToId(staffId);
    }
    
    /**
     * Get tickets by status
     */
    public List<SupportTicket> getTicketsByStatus(TicketStatus status) {
        return supportTicketRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    /**
     * Generate unique ticket number in format: TK-YYYY-NNNN
     * Example: TK-2025-0001
     */
    private String generateTicketNumber() {
        String year = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy"));
        
        // Count existing tickets this year to get next number
        String prefix = "TK-" + year + "-";
        long count = supportTicketRepository.count() + 1;
        
        // Format with leading zeros (4 digits)
        String number = String.format("%04d", count);
        
        return prefix + number;
    }

    /**
     * UC19: Create new support ticket
     * Can be created by customer or guest (customerId can be null)
     */
    @Transactional
    public SupportTicket createTicket(SupportTicket ticket) {
        // Generate unique ticket number
        ticket.setTicketNumber(generateTicketNumber());
        
        // Set initial status
        ticket.setStatus(TicketStatus.OPEN);
        
        // Save ticket
        SupportTicket savedTicket = supportTicketRepository.save(ticket);
        
        // Create initial message from ticket description
        if (ticket.getCustomer() != null) {
            SupportMessage initialMessage = new SupportMessage();
            initialMessage.setTicket(savedTicket);
            initialMessage.setSender(ticket.getCustomer());
            initialMessage.setMessageText(ticket.getDescription());
            supportMessageRepository.save(initialMessage);
        }
        
        return savedTicket;
    }

    /**
     * UC20: Update ticket status
     */
    @Transactional
    public SupportTicket updateTicketStatus(Long id, TicketStatus status) {
        SupportTicket ticket = supportTicketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        
        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(status);

        // Set resolved time when ticket is resolved or closed
        if (status == TicketStatus.RESOLVED || status == TicketStatus.CLOSED) {
            ticket.setResolvedAt(LocalDateTime.now());
        }

        SupportTicket savedTicket = supportTicketRepository.save(ticket);

        // Send notification when ticket is resolved
        if (status == TicketStatus.RESOLVED && oldStatus != TicketStatus.RESOLVED && ticket.getCustomer() != null) {
            try {
                notificationService.createTicketResolvedNotification(
                    ticket.getCustomer().getId(),
                    ticket.getTicketNumber()
                );
            } catch (Exception e) {
                System.err.println("Failed to send ticket resolved notification: " + e.getMessage());
            }
        }

        return savedTicket;
    }

    /**
     * UC20: Assign ticket to staff
     */
    @Transactional
    public SupportTicket assignTicket(Long ticketId, Long staffId) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));
        
        ticket.setAssignedTo(staff);
        
        // Auto-update status to IN_PROGRESS when assigned
        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }

        SupportTicket savedTicket = supportTicketRepository.save(ticket);

        // Send notification to customer about ticket assignment
        if (ticket.getCustomer() != null) {
            try {
                notificationService.createTicketAssignedNotification(
                    ticket.getCustomer().getId(),
                    ticket.getTicketNumber(),
                    staff.getFullName()
                );
            } catch (Exception e) {
                System.err.println("Failed to send ticket assigned notification: " + e.getMessage());
            }
        }

        return savedTicket;
    }
    
    /**
     * Get all messages for a ticket
     */
    public List<SupportMessage> getMessages(Long ticketId) {
        return supportMessageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
    }
    
    /**
     * Add a message to a ticket (for replies)
     */
    @Transactional
    public SupportMessage addMessage(Long ticketId, Long senderId, String messageText) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        SupportMessage message = new SupportMessage();
        message.setTicket(ticket);
        message.setSender(sender);
        message.setMessageText(messageText);
        
        SupportMessage savedMessage = supportMessageRepository.save(message);

        // If sender is staff/admin and ticket has a customer, send notification
        if (ticket.getCustomer() != null && 
            (sender.getRole() == User.UserRole.STAFF || sender.getRole() == User.UserRole.ADMIN)) {
            try {
                notificationService.createTicketResponseNotification(
                    ticket.getCustomer().getId(),
                    ticket.getTicketNumber()
                );
            } catch (Exception e) {
                System.err.println("Failed to send ticket response notification: " + e.getMessage());
            }
        }
        
        return savedMessage;
    }
    
    /**
     * UC21: Rate support ticket
     * Customer can rate a resolved/closed ticket
     */
    @Transactional
    public SupportMessage rateSupport(Long ticketId, Long customerId, Integer rating, String feedbackText) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        
        // Verify ticket belongs to customer
        if (ticket.getCustomer() == null || !ticket.getCustomer().getId().equals(customerId)) {
            throw new RuntimeException("Unauthorized: Ticket does not belong to this customer");
        }
        
        // Verify ticket is resolved or closed
        if (ticket.getStatus() != TicketStatus.RESOLVED && ticket.getStatus() != TicketStatus.CLOSED) {
            throw new RuntimeException("Can only rate resolved or closed tickets");
        }
        
        // Check if already rated
        Optional<SupportMessage> existingRating = supportMessageRepository.findByTicketIdAndRatingIsNotNull(ticketId);
        if (existingRating.isPresent()) {
            throw new RuntimeException("Ticket has already been rated");
        }
        
        // Validate rating (1-5)
        if (rating < 1 || rating > 5) {
            throw new RuntimeException("Rating must be between 1 and 5");
        }
        
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        // Create rating message
        SupportMessage ratingMessage = new SupportMessage();
        ratingMessage.setTicket(ticket);
        ratingMessage.setSender(customer);
        ratingMessage.setMessageText(feedbackText != null ? feedbackText : "");
        ratingMessage.setRating(rating);
        
        return supportMessageRepository.save(ratingMessage);
    }
    
    /**
     * Get rating for a ticket (if exists)
     */
    public Optional<SupportMessage> getTicketRating(Long ticketId) {
        return supportMessageRepository.findByTicketIdAndRatingIsNotNull(ticketId);
    }
}
