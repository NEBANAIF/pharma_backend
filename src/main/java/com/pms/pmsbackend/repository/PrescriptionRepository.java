package com.pms.pmsbackend.repository;

import com.pms.pmsbackend.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrescriptionRepository extends JpaRepository<Prescription, Integer> {
    List<Prescription> findAllByOrderByCreatedAtDesc();
    List<Prescription> findByCustomerIdOrderByCreatedAtDesc(Integer customerId);
}
