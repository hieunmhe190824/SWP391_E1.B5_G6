package com.carrental.repository;

import com.carrental.model.SupportTicket;
import com.carrental.model.SupportTicket.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    /**
     * Find tickets by customer, sorted by newest first
     */
    List<SupportTicket> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    
    /**
     * Find tickets assigned to a staff member
     */
    List<SupportTicket> findByAssignedToId(Long staffId);
    
    /**
     * Find all tickets sorted by newest first (for staff/admin)
     */
    List<SupportTicket> findAllByOrderByCreatedAtDesc();
    
    /**
     * Find tickets by status, sorted by newest first
     */
    List<SupportTicket> findByStatusOrderByCreatedAtDesc(TicketStatus status);
}
