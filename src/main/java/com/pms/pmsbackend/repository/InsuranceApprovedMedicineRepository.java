package com.pms.pmsbackend.repository;

import com.pms.pmsbackend.entity.InsuranceApprovedMedicine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InsuranceApprovedMedicineRepository extends JpaRepository<InsuranceApprovedMedicine, Integer> {
    List<InsuranceApprovedMedicine> findByInsuranceProviderId(Integer providerId);
    Optional<InsuranceApprovedMedicine> findByInsuranceProviderIdAndMedicineId(Integer providerId, Integer medicineId);
    boolean existsByInsuranceProviderIdAndMedicineId(Integer providerId, Integer medicineId);
}
