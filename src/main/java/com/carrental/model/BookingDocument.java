package com.carrental.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * BookingDocument Entity - Junction table for Booking and UserDocument (Many-to-Many)
 * Represents the relationship between bookings and user documents
 * Maps to: booking_documents table
 */
@Entity
@Table(name = "booking_documents")
public class BookingDocument {
    
    @EmbeddedId
    private BookingDocumentId id;
    
    @ManyToOne
    @MapsId("bookingId")
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;
    
    @ManyToOne
    @MapsId("documentId")
    @JoinColumn(name = "document_id", nullable = false)
    private UserDocument document;
    
    @Column(name = "submitted_at", insertable = false, updatable = false)
    private LocalDateTime submittedAt;
    
    // Constructors
    public BookingDocument() {
    }
    
    public BookingDocument(Booking booking, UserDocument document) {
        this.booking = booking;
        this.document = document;
        this.id = new BookingDocumentId(booking.getId(), document.getId());
    }
    
    // Getters and Setters
    public BookingDocumentId getId() {
        return id;
    }
    
    public void setId(BookingDocumentId id) {
        this.id = id;
    }
    
    public Booking getBooking() {
        return booking;
    }
    
    public void setBooking(Booking booking) {
        this.booking = booking;
    }
    
    public UserDocument getDocument() {
        return document;
    }
    
    public void setDocument(UserDocument document) {
        this.document = document;
    }
    
    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }
    
    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
    
    /**
     * Composite Primary Key for BookingDocument
     */
    @Embeddable
    public static class BookingDocumentId implements java.io.Serializable {
        
        @Column(name = "booking_id")
        private Long bookingId;
        
        @Column(name = "document_id")
        private Long documentId;
        
        // Constructors
        public BookingDocumentId() {
        }
        
        public BookingDocumentId(Long bookingId, Long documentId) {
            this.bookingId = bookingId;
            this.documentId = documentId;
        }
        
        // Getters and Setters
        public Long getBookingId() {
            return bookingId;
        }
        
        public void setBookingId(Long bookingId) {
            this.bookingId = bookingId;
        }
        
        public Long getDocumentId() {
            return documentId;
        }
        
        public void setDocumentId(Long documentId) {
            this.documentId = documentId;
        }
        
        // equals and hashCode
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            
            BookingDocumentId that = (BookingDocumentId) o;
            
            if (!bookingId.equals(that.bookingId)) return false;
            return documentId.equals(that.documentId);
        }
        
        @Override
        public int hashCode() {
            int result = bookingId.hashCode();
            result = 31 * result + documentId.hashCode();
            return result;
        }
    }
}

