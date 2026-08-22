package com.pms.pmsbackend.service;

import com.pms.pmsbackend.dto.LoginRequest;
import com.pms.pmsbackend.dto.LoginResponse;
import com.pms.pmsbackend.dto.RegisterRequest;
import com.pms.pmsbackend.entity.Branch;
import com.pms.pmsbackend.entity.Role;
import com.pms.pmsbackend.entity.User;
import com.pms.pmsbackend.repository.BranchRepository;
import com.pms.pmsbackend.repository.RoleRepository;
import com.pms.pmsbackend.repository.UserRepository;
import com.pms.pmsbackend.security.JwtService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + request.getUsername()));

        String token = jwtService.generateToken(user);
        String roleName = user.getRole() != null ? user.getRole().getName() : null;
        Integer branchId = user.getBranch() != null ? user.getBranch().getId() : null;
        String branchName = user.getBranch() != null ? user.getBranch().getName() : null;

        return new LoginResponse(token, user.getUsername(), user.getFullName(), roleName, branchId, branchName);
    }

    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already taken: " + request.getUsername());
        }

        Role role = roleRepository.findByNameIgnoreCase(request.getRoleName())
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + request.getRoleName()));

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setRole(role);

        if (request.getBranchId() != null) {
            Branch branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new EntityNotFoundException("Branch not found: " + request.getBranchId()));
            user.setBranch(branch);
        }

        userRepository.save(user);
    }
}
