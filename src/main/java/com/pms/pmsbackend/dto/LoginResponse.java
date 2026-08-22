package com.pms.pmsbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String username;
    private String fullName;
    private String role;
    private Integer branchId;
    private String branchName;
}
