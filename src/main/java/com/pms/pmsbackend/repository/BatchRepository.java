package com.pms.pmsbackend.repository;

import com.pms.pmsbackend.entity.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BatchRepository extends JpaRepository<Batch, Integer> {

    @Query("SELECT COALESCE(SUM(b.quantity), 0) FROM Batch b WHERE b.medicine.id = :medicineId")
    Integer sumQuantityByMedicineId(@Param("medicineId") Integer medicineId);

    @Query("SELECT COALESCE(SUM(b.quantity), 0) FROM Batch b WHERE b.medicine.id = :medicineId AND b.branch.id = :branchId")
    Integer sumQuantityByMedicineIdAndBranchId(@Param("medicineId") Integer medicineId, @Param("branchId") Integer branchId);

    List<Batch> findByMedicineIdOrderByExpiryDateAsc(Integer medicineId);

    // Same rows as above, but fetch-joined: BatchService.toDto() touches
    // medicine, supplier, and branch on every row to build the DTO, and those
    // are all @ManyToOne(LAZY). Without this, that's one query for the batch
    // list plus one query per row per lazy association (N+1 x3). Use this
    // version wherever the result is going to be mapped to a DTO; keep the
    // plain one above for call sites (e.g. FEFO stock deduction) that only
    // ever touch batch.getQuantity().
    @Query("SELECT b FROM Batch b " +
           "JOIN FETCH b.medicine " +
           "JOIN FETCH b.branch " +
           "LEFT JOIN FETCH b.supplier " +
           "WHERE b.medicine.id = :medicineId " +
           "ORDER BY b.expiryDate ASC")
    List<Batch> findByMedicineIdWithDetailsOrderByExpiryDateAsc(@Param("medicineId") Integer medicineId);

    List<Batch> findByMedicineIdAndBranchIdOrderByExpiryDateAsc(Integer medicineId, Integer branchId);

    List<Batch> findByBranchIdOrderByExpiryDateAsc(Integer branchId);

    @Query("SELECT b FROM Batch b JOIN FETCH b.medicine WHERE b.quantity > 0 AND b.expiryDate <= :cutoff ORDER BY b.expiryDate ASC")
    List<Batch> findExpiringBySoonCutoff(@Param("cutoff") java.time.LocalDate cutoff);

    @Query("SELECT b.branch.id AS branchId, b.branch.name AS branchName, b.medicine.id AS medicineId, COALESCE(SUM(b.quantity), 0) AS qty " +
           "FROM Batch b WHERE b.branch IS NOT NULL " +
           "GROUP BY b.branch.id, b.branch.name, b.medicine.id")
    List<BranchMedicineStock> sumQuantityGroupedByBranchAndMedicine();

    // Projection for the aggregate query above -- one row per (branch, medicine)
    // with its total stock, used to build the per-branch inventory valuation.
    interface BranchMedicineStock {
        Integer getBranchId();
        String getBranchName();
        Integer getMedicineId();
        Integer getQty();
    }
}
