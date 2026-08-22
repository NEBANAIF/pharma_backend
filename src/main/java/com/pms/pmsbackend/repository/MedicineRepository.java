package com.pms.pmsbackend.repository;

import com.pms.pmsbackend.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MedicineRepository extends JpaRepository<Medicine, Integer> {
    Optional<Medicine> findByBarcode(String barcode);
}
