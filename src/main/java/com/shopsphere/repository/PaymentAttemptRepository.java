package com.shopsphere.repository;

import com.shopsphere.entity.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {

    List<PaymentAttempt> findByOrderId(Long orderId);

    Optional<PaymentAttempt> findTopByOrderIdOrderByAttemptedAtDesc(Long orderId);

    Optional<PaymentAttempt> findByTransactionReference(String transactionReference);
}
