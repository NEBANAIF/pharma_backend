package com.pms.pmsbackend.repository;

import com.pms.pmsbackend.entity.PurchaseItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, Integer> {
}
