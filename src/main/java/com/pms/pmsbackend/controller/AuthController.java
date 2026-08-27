package com.pms.pmsbackend.controller;

import com.pms.pmsbackend.dto.LoginRequest;
import com.pms.pmsbackend.dto.LoginResponse;
import com.pms.pmsbackend.dto.RegisterRequest;
import com.pms.pmsbackend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    // Reachable while unauthenticated ONLY if no users exist yet (bootstrap
    // case) -- see BootstrapGuard. Once the first account is created, this
    // permanently requires an authenticated ADMIN, same as before.
    @PreAuthorize("hasRole('ADMIN') or @bootstrapGuard.noUsersExist()")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
    }

    // TEMPORARY diagnostic endpoint -- reports exactly what authorities the
    // backend currently sees for the logged-in caller, straight from the
    // security context, so role/permission issues can be confirmed without
    // guessing through the database. Safe to delete once things are sorted
    // out; only requires being logged in (any role), doesn't leak anything
    // sensitive beyond the caller's own authorities.
    @GetMapping("/whoami")
    public Map<String, Object> whoami(Authentication authentication) {
        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        return Map.of(
                "username", authentication.getName(),
                "authorities", authorities
        );
    }
}
