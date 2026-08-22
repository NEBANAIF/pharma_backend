package com.pms.pmsbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateRequest {

    private String fullName;
    private String email;

    @NotBlank(message = "Role is required (e.g. ADMIN, PHARMACIST, CASHIER, STOREKEEPER, MANAGER)")
    private String roleName;

    private Integer branchId; // optional -- null means "no branch assigned"

    @NotNull(message = "isActive is required")
    private Boolean isActive;
}
