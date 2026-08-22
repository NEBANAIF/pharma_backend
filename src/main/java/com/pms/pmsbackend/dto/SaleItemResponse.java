package com.pms.pmsbackend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SaleItemResponse {
    private Integer id;
    private Integer medicineId;
    private String medicineName;
    private String batchNumber;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
}
