package com.pms.pmsbackend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaleItemRequest {

    @NotNull(message = "Medicine is required")
    private Integer medicineId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    // Optional: sell from this specific batch instead of the default
    // earliest-expiry-first (FEFO) allocation across all batches.
    private Integer batchId;
}
