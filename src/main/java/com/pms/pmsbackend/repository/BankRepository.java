package com.pms.pmsbackend.repository;

import com.pms.pmsbackend.entity.Bank;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankRepository extends JpaRepository<Bank, Integer> {
}
