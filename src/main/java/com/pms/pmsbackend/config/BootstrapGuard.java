package com.pms.pmsbackend.config;

import com.pms.pmsbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Backs the "or no users exist yet" half of the @PreAuthorize on
 * /api/auth/register (see AuthController). Lets exactly one unauthenticated
 * registration call through -- to create the first admin account -- and
 * then permanently closes itself the moment that account is saved, since
 * userRepository.count() becomes > 0 from then on.
 */
@Component("bootstrapGuard")
@RequiredArgsConstructor
public class BootstrapGuard {

    private final UserRepository userRepository;

    public boolean noUsersExist() {
        return userRepository.count() == 0;
    }
}
