package com.pms.pmsbackend.repository;

import com.pms.pmsbackend.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Integer> {

    @Query("SELECT m FROM StockMovement m " +
           "JOIN FETCH m.medicine " +
           "LEFT JOIN FETCH m.branch " +
           "LEFT JOIN FETCH m.batch " +
           "LEFT JOIN FETCH m.performedBy " +
           "ORDER BY m.createdAt DESC, m.id DESC")
    List<StockMovement> findAllWithDetailsOrderByCreatedAtDesc();

    @Query("SELECT m FROM StockMovement m " +
           "JOIN FETCH m.medicine " +
           "LEFT JOIN FETCH m.branch " +
           "LEFT JOIN FETCH m.batch " +
           "LEFT JOIN FETCH m.performedBy " +
           "WHERE m.medicine.id = :medicineId " +
           "ORDER BY m.createdAt DESC, m.id DESC")
    List<StockMovement> findByMedicineIdWithDetailsOrderByCreatedAtDesc(@Param("medicineId") Integer medicineId);
}
