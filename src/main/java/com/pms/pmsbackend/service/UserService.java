package com.pms.pmsbackend.service;

import com.pms.pmsbackend.dto.PasswordResetRequest;
import com.pms.pmsbackend.dto.UserResponse;
import com.pms.pmsbackend.dto.UserUpdateRequest;
import com.pms.pmsbackend.entity.Branch;
import com.pms.pmsbackend.entity.Role;
import com.pms.pmsbackend.entity.User;
import com.pms.pmsbackend.repository.BranchRepository;
import com.pms.pmsbackend.repository.RoleRepository;
import com.pms.pmsbackend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse update(Integer id, UserUpdateRequest request) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));

        // Guard against an admin locking themselves out -- deactivating their
        // own account or demoting themselves out of ADMIN would leave nobody
        // able to fix it back through this same screen.
        String currentUsername = currentUsername();
        boolean editingSelf = currentUsername != null && currentUsername.equals(existing.getUsername());
        if (editingSelf) {
            if (Boolean.FALSE.equals(request.getIsActive())) {
                throw new IllegalArgumentException("You cannot deactivate your own account.");
            }
            if (!"ADMIN".equals(request.getRoleName())) {
                throw new IllegalArgumentException("You cannot remove your own ADMIN role.");
            }
        }

        Role role = roleRepository.findByName(request.getRoleName())
                .orElseThrow(() -> new EntityNotFoundException("Role not found: " + request.getRoleName()));

        existing.setFullName(request.getFullName());
        existing.setEmail(request.getEmail());
        existing.setRole(role);
        existing.setIsActive(request.getIsActive());

        if (request.getBranchId() == null) {
            existing.setBranch(null);
        } else {
            Branch branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new EntityNotFoundException("Branch not found: " + request.getBranchId()));
            existing.setBranch(branch);
        }

        return toResponse(userRepository.save(existing));
    }

    @Transactional
    public void resetPassword(Integer id, PasswordResetRequest request) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
        existing.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(existing);
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : null;
    }

    private UserResponse toResponse(User user) {
        UserResponse dto = new UserResponse();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setRoleName(user.getRole() != null ? user.getRole().getName() : null);
        dto.setBranchId(user.getBranch() != null ? user.getBranch().getId() : null);
        dto.setBranchName(user.getBranch() != null ? user.getBranch().getName() : null);
        dto.setIsActive(user.getIsActive());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}
