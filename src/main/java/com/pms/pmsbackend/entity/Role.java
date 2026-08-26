package com.pms.pmsbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;   // ADMIN, PHARMACIST, CASHIER, STOREKEEPER, MANAGER

    // Role names are compared case-sensitively against literal constants
    // both here (@PreAuthorize("hasRole('ADMIN')")) and on the frontend
    // (roles.js), so a stray-case value (e.g. "admin" instead of "ADMIN")
    // silently fails every permission check tied to it. Normalize on every
    // save so that can't happen through the application -- it just won't
    // catch a role row inserted by hand directly in SQL.
    @jakarta.persistence.PrePersist
    @jakarta.persistence.PreUpdate
    private void normalizeName() {
        if (name != null) {
            name = name.trim().toUpperCase();
        }
    }
}
