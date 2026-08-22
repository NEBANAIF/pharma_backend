package com.pms.pmsbackend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class PurchaseResponse {
    private Integer id;
    private Integer supplierId;
    private String supplierName;
    private String invoiceNumber;
    private LocalDate purchaseDate;
    private BigDecimal totalAmount;
    private String status;
    private String createdByName;
    private LocalDateTime createdAt;
    private Integer branchId;
    private String branchName;
    private List<PurchaseItemResponse> items;
}
