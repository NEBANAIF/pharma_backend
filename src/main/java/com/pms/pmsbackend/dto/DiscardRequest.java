package com.pms.pmsbackend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiscardRequest {

    @NotNull(message = "Medicine is required")
    private Integer medicineId;

    private Integer branchId; // defaults to the current user's branch (or Main Branch) if omitted

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than 0")
    private Integer quantity;

    @NotNull(message = "Reason is required")
    private String reason; // EXPIRED, DAMAGED, OTHER

    private String notes;
}
