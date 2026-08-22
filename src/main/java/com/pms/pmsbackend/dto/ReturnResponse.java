package com.pms.pmsbackend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class ReturnResponse {
    private Integer id;
    private Integer saleItemId;
    private Integer saleId;
    private String medicineName;
    private Integer quantity;
    private String reason;
    private BigDecimal refundAmount;
    private String processedByName;
    private LocalDateTime createdAt;
}
