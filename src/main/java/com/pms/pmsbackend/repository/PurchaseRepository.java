package com.pms.pmsbackend.repository;

import com.pms.pmsbackend.entity.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseRepository extends JpaRepository<Purchase, Integer> {
    List<Purchase> findAllByOrderByCreatedAtDesc();
    long countByStatus(String status);

    // "Overdue" here means still PENDING (not yet received) more than a week
    // after the purchase was raised. There's no separate "expected delivery
    // date" field on Purchase to compare against, so purchaseDate + a week is
    // the honest heuristic rather than inventing a due-date field.
    long countByStatusAndPurchaseDateBefore(String status, java.time.LocalDate date);
}
