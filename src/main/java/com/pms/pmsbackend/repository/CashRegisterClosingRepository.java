package com.pms.pmsbackend.repository;

import com.pms.pmsbackend.entity.CashRegisterClosing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CashRegisterClosingRepository extends JpaRepository<CashRegisterClosing, Integer> {
    Optional<CashRegisterClosing> findByClosingDate(LocalDate date);
    List<CashRegisterClosing> findAllByOrderByClosingDateDesc();
}
