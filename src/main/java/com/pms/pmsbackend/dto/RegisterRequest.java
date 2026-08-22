package com.pms.pmsbackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    private String fullName;
    private String email;

    @NotBlank(message = "Role is required (e.g. ADMIN, PHARMACIST, CASHIER, STOREKEEPER, MANAGER)")
    private String roleName;

    private Integer branchId; // optional -- which branch this user works at
}
