package com.pms.pmsbackend.repository;

import com.pms.pmsbackend.entity.SaleReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SaleReturnRepository extends JpaRepository<SaleReturn, Integer> {

    @Query("SELECT COALESCE(SUM(r.quantity), 0) FROM SaleReturn r WHERE r.saleItem.id = :saleItemId")
    Integer sumQuantityBySaleItemId(@Param("saleItemId") Integer saleItemId);

    List<SaleReturn> findAllByOrderByCreatedAtDesc();
}
