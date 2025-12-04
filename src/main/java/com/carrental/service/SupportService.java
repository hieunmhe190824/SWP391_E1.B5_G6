package com.carrental.service;

import com.carrental.model.SupportTicket;
import com.carrental.model.SupportTicket.TicketStatus;
import com.carrental.repository.SupportTicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SupportService {

    @Autowired
    private SupportTicketRepository supportTicketRepository;

    public List<SupportTicket> getAllTickets() {
        return supportTicketRepository.findAll();
    }

    public Optional<SupportTicket> getTicketById(Long id) {
        return supportTicketRepository.findById(id);
    }

    public List<SupportTicket> getTicketsByCustomer(Long customerId) {
        return supportTicketRepository.findByCustomerId(customerId);
    }

    public List<SupportTicket> getTicketsByStaff(Long staffId) {
        return supportTicketRepository.findByAssignedToId(staffId);
    }

    public SupportTicket createTicket(SupportTicket ticket) {
        ticket.setStatus(TicketStatus.OPEN);
        return supportTicketRepository.save(ticket);
    }

    public SupportTicket updateTicketStatus(Long id, TicketStatus status) {
        SupportTicket ticket = supportTicketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        ticket.setStatus(status);

        if (status == TicketStatus.RESOLVED || status == TicketStatus.CLOSED) {
            ticket.setResolvedAt(LocalDateTime.now());
        }

        return supportTicketRepository.save(ticket);
    }

    public SupportTicket assignTicket(Long ticketId, Long staffId) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        com.carrental.model.User staff = new com.carrental.model.User();
        staff.setId(staffId);
        ticket.setAssignedTo(staff);
        ticket.setStatus(TicketStatus.IN_PROGRESS);

        return supportTicketRepository.save(ticket);
    }
}
