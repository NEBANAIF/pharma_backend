package com.pms.pmsbackend.repository;

import com.pms.pmsbackend.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorRepository extends JpaRepository<Doctor, Integer> {
}
