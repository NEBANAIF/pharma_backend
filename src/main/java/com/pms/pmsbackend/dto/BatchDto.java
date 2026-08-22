package com.pms.pmsbackend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class BatchDto {

    private Integer id;

    @NotNull(message = "Medicine is required")
    private Integer medicineId;

    private String medicineName; // populated in responses only

    @NotBlank(message = "Batch number is required")
    private String batchNumber;

    private LocalDate manufactureDate;

    @NotNull(message = "Expiry date is required")
    private LocalDate expiryDate;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    private Integer supplierId;
    private String supplierName; // populated in responses only

    private Integer branchId; // required on create; which branch this stock physically sits in
    private String branchName; // populated in responses only
}
