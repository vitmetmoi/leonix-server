package com.example.myapp.features.wallet;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentId(String paymentId);
    Optional<Payment> findByTransferContent(String transferContent);
    List<Payment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.amountVnd), 0) FROM Payment p WHERE p.status = 'COMPLETED' OR p.status = 'SUCCESS'")
    BigDecimal sumTotalSuccessfulPayments();

    long countByStatus(String status);
}
