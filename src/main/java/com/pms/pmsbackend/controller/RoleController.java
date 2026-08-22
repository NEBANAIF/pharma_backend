package com.pms.pmsbackend.controller;

import com.pms.pmsbackend.entity.Role;
import com.pms.pmsbackend.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleRepository roleRepository;

    // Only used to populate dropdowns on the (ADMIN-only) user-management
    // screen, so it's locked down the same way rather than left open.
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<Role> getAll() {
        return roleRepository.findAll();
    }
}
