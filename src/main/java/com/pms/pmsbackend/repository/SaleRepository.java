package com.pms.pmsbackend.repository;

import com.pms.pmsbackend.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Integer> {

    List<Sale> findAllByOrderBySaleDateDesc();

    List<Sale> findByCustomerIdOrderBySaleDateDesc(Integer customerId);

    java.util.Optional<Sale> findByPrescriptionId(Integer prescriptionId);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s WHERE CAST(s.saleDate AS date) = CURRENT_DATE")
    java.math.BigDecimal sumTotalForToday();

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s WHERE CAST(s.saleDate AS date) = :date")
    java.math.BigDecimal sumTotalForDate(@Param("date") java.time.LocalDate date);

    // Per-branch totals for a date range, used to show each branch's share of
    // company-wide sales on the dashboard. Deliberately a share of the actual
    // total for the period, not "% of a monthly target" -- this system has no
    // concept of a sales target to compare against, so showing one would mean
    // making a number up.
    @Query("SELECT s.branch.id, s.branch.name, COALESCE(SUM(s.totalAmount), 0) FROM Sale s " +
           "WHERE CAST(s.saleDate AS date) BETWEEN :from AND :to " +
           "GROUP BY s.branch.id, s.branch.name ORDER BY SUM(s.totalAmount) DESC")
    List<Object[]> sumTotalsByBranchForRange(@Param("from") java.time.LocalDate from, @Param("to") java.time.LocalDate to);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s WHERE s.paymentMethod = 'CASH' AND CAST(s.saleDate AS date) = :date")
    java.math.BigDecimal sumCashSalesForDate(@Param("date") java.time.LocalDate date);

    @Query(value =
        "SELECT CAST(sale_date AS date) AS day, SUM(total_amount) AS total " +
        "FROM sales " +
        "WHERE sale_date >= CURRENT_DATE - INTERVAL '6 days' " +
        "GROUP BY day ORDER BY day",
        nativeQuery = true)
    List<Object[]> sumTotalsByDayLast7Days();
}