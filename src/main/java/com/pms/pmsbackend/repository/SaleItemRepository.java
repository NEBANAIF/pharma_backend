package com.pms.pmsbackend.repository;

import com.pms.pmsbackend.entity.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface SaleItemRepository extends JpaRepository<SaleItem, Integer> {

    @Query("SELECT COALESCE(SUM((si.unitPrice - si.medicine.purchasePrice) * si.quantity), 0) " +
           "FROM SaleItem si WHERE CAST(si.sale.saleDate AS date) = CURRENT_DATE")
    BigDecimal sumProfitForToday();

    @Query("SELECT COALESCE(SUM((si.unitPrice - si.medicine.purchasePrice) * si.quantity), 0) " +
           "FROM SaleItem si WHERE CAST(si.sale.saleDate AS date) = :date")
    BigDecimal sumProfitForDate(@Param("date") LocalDate date);

    @Query(value =
        "SELECT CAST(s.sale_date AS date) AS day, " +
        "SUM((si.unit_price - m.purchase_price) * si.quantity) AS total " +
        "FROM sale_items si " +
        "JOIN sales s ON s.id = si.sale_id " +
        "JOIN medicines m ON m.id = si.medicine_id " +
        "WHERE s.sale_date >= CURRENT_DATE - INTERVAL '6 days' " +
        "GROUP BY day ORDER BY day",
        nativeQuery = true)
    List<Object[]> sumProfitByDayLast7Days();

    @Query("SELECT COALESCE(SUM(si.subtotal), 0) FROM SaleItem si " +
           "WHERE CAST(si.sale.saleDate AS date) BETWEEN :from AND :to")
    BigDecimal sumRevenueForRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(si.medicine.purchasePrice * si.quantity), 0) FROM SaleItem si " +
           "WHERE CAST(si.sale.saleDate AS date) BETWEEN :from AND :to")
    BigDecimal sumCostForRange(@Param("from") LocalDate from, @Param("to") LocalDate to);
    @Query("SELECT si.medicine.id, si.medicine.name, SUM(si.quantity), SUM(si.subtotal) " +
           "FROM SaleItem si WHERE CAST(si.sale.saleDate AS date) BETWEEN :from AND :to " +
           "GROUP BY si.medicine.id, si.medicine.name ORDER BY SUM(si.quantity) DESC")
    List<Object[]> findBestSellingForRange(@Param("from") LocalDate from, @Param("to") LocalDate to);

    // Top movers over a fixed trailing window, for the dashboard snapshot
    // (distinct from findBestSellingForRange, which takes an arbitrary
    // caller-supplied range for the Reports page).
    @Query(value =
        "SELECT m.id, m.name, COALESCE(SUM(si.quantity), 0) AS qty " +
        "FROM medicines m " +
        "LEFT JOIN sale_items si ON si.medicine_id = m.id " +
        "  AND si.sale_id IN (SELECT id FROM sales WHERE sale_date >= CURRENT_DATE - INTERVAL '29 days') " +
        "WHERE m.is_active = true " +
        "GROUP BY m.id, m.name " +
        "ORDER BY qty DESC, m.name ASC " +
        "LIMIT :limit",
        nativeQuery = true)
    List<Object[]> findTopSellersLast30Days(@Param("limit") int limit);

    // Slowest movers over the same window. LEFT JOIN + COALESCE means a
    // medicine that sold zero units still shows up with qty = 0, which is
    // exactly what "least selling" should surface (dead stock), not just
    // whichever medicine happened to sell the fewest units among the ones
    // that sold at all.
    @Query(value =
        "SELECT m.id, m.name, COALESCE(SUM(si.quantity), 0) AS qty " +
        "FROM medicines m " +
        "LEFT JOIN sale_items si ON si.medicine_id = m.id " +
        "  AND si.sale_id IN (SELECT id FROM sales WHERE sale_date >= CURRENT_DATE - INTERVAL '29 days') " +
        "WHERE m.is_active = true " +
        "GROUP BY m.id, m.name " +
        "ORDER BY qty ASC, m.name ASC " +
        "LIMIT :limit",
        nativeQuery = true)
    List<Object[]> findLeastSellersLast30Days(@Param("limit") int limit);
}