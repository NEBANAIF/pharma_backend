package com.pms.pmsbackend.controller;

import com.pms.pmsbackend.dto.LoginRequest;
import com.pms.pmsbackend.dto.LoginResponse;
import com.pms.pmsbackend.dto.RegisterRequest;
import com.pms.pmsbackend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    // Now locked to ADMIN only. During initial bootstrap (no users exist yet),
    // this endpoint was left open long enough to create the first admin account.
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
    }
}
