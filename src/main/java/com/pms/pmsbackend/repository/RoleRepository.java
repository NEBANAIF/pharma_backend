package com.pms.pmsbackend.repository;

import com.pms.pmsbackend.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(String name);

    @org.springframework.data.jpa.repository.Query("SELECT r FROM Role r WHERE UPPER(r.name) = UPPER(:name)")
    Optional<Role> findByNameIgnoreCase(@org.springframework.data.repository.query.Param("name") String name);
}
