package com.carrental.repository;

import com.carrental.model.BookingDocument;
import com.carrental.model.BookingDocument.BookingDocumentId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingDocumentRepository extends JpaRepository<BookingDocument, BookingDocumentId> {
    
    /**
     * Find all documents for a specific booking
     */
    List<BookingDocument> findByBookingId(Long bookingId);
    
    /**
     * Find all bookings that use a specific document
     */
    List<BookingDocument> findByDocumentId(Long documentId);
    
    /**
     * Delete all documents for a specific booking
     */
    void deleteByBookingId(Long bookingId);
    
    /**
     * Find booking documents with document details loaded
     */
    @Query("SELECT bd FROM BookingDocument bd " +
           "LEFT JOIN FETCH bd.document d " +
           "LEFT JOIN FETCH d.user " +
           "WHERE bd.booking.id = :bookingId")
    List<BookingDocument> findByBookingIdWithDocuments(@Param("bookingId") Long bookingId);
}

