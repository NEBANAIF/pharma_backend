package com.pms.pmsbackend.repository;

import com.pms.pmsbackend.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, Integer> {
}
