package com.pms.pmsbackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class MedicineDto {

    private Integer id;

    @NotBlank(message = "Name is required")
    private String name;

    private String genericName;
    private String brand;
    private Integer categoryId;
    private String categoryName; // populated in responses only
    private String barcode;
    private String unit;

    @NotNull(message = "Purchase price is required")
    private BigDecimal purchasePrice;

    @NotNull(message = "Selling price is required")
    private BigDecimal sellingPrice;

    private BigDecimal taxPercent;
    private Integer reorderLevel;
    private String imageUrl;
    private Boolean isActive;

    // Computed: sum of quantities across all non-expired batches (populated by service layer)
    private Integer currentStock;
}
