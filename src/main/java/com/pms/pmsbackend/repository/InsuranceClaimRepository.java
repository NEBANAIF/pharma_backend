package com.pms.pmsbackend.repository;

import com.pms.pmsbackend.entity.InsuranceClaim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InsuranceClaimRepository extends JpaRepository<InsuranceClaim, Integer> {

    List<InsuranceClaim> findAllByOrderByCreatedAtDesc();

    List<InsuranceClaim> findBySaleIdOrderByCreatedAtDesc(Integer saleId);
}
