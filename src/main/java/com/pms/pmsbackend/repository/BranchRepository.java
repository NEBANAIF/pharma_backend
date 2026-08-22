package com.pms.pmsbackend.repository;

import com.pms.pmsbackend.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Integer> {
    Optional<Branch> findByIsMainTrue();
}
