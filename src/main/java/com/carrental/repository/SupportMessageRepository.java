package com.carrental.repository;

import com.carrental.model.SupportMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {
    /**
     * Find all messages for a ticket, ordered by creation time (oldest first)
     */
    List<SupportMessage> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
    
    /**
     * Find rated message for a ticket (for UC21: Rate Support)
     * A ticket should only have one message with a rating
     */
    Optional<SupportMessage> findByTicketIdAndRatingIsNotNull(Long ticketId);
}
