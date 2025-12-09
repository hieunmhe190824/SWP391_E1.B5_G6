package com.carrental.repository;

import com.carrental.model.Contract;
import com.carrental.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByContractId(Long contractId);

    /**
     * Find payment by contract and status
     */
    Optional<Payment> findByContractAndStatus(Contract contract, Payment.PaymentStatus status);

    /**
     * Find payment by transaction reference
     */
    Optional<Payment> findByTransactionRef(String transactionRef);
}
