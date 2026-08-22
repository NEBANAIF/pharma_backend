package com.pms.pmsbackend.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UserResponse {
    private Integer id;
    private String username;
    private String fullName;
    private String email;
    private String roleName;
    private Integer branchId;
    private String branchName;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
