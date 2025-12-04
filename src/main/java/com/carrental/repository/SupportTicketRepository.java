package com.carrental.repository;

import com.carrental.model.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByCustomerId(Long customerId);
    List<SupportTicket> findByAssignedToId(Long staffId);
}
