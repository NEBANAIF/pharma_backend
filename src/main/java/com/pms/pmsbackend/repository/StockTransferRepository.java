package com.pms.pmsbackend.repository;

import com.pms.pmsbackend.entity.StockTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockTransferRepository extends JpaRepository<StockTransfer, Integer> {
    List<StockTransfer> findAllByOrderByCreatedAtDesc();
}
