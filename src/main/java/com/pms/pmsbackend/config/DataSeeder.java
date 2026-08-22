package com.pms.pmsbackend.config;

import com.pms.pmsbackend.entity.Role;
import com.pms.pmsbackend.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Runs once on every startup and seeds the standard roles if they don't
 * exist yet. A brand-new Neon database has an empty `roles` table --
 * nothing else creates these rows -- and both /api/auth/register and the
 * bootstrap flow (see BootstrapGuard) need the ADMIN role to already exist
 * before the first account can be created.
 *
 * Safe to leave running permanently: once all five roles exist, this is a
 * cheap no-op on every subsequent boot.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private static final List<String> STANDARD_ROLES =
            List.of("ADMIN", "PHARMACIST", "CASHIER", "STOREKEEPER", "MANAGER");

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        for (String name : STANDARD_ROLES) {
            if (roleRepository.findByNameIgnoreCase(name).isEmpty()) {
                Role role = new Role();
                role.setName(name);
                roleRepository.save(role);
                log.info("Seeded role: {}", name);
            }
        }
    }
}
