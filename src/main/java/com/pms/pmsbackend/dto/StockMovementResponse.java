package com.pms.pmsbackend.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class StockMovementResponse {
    private Integer id;
    private Integer medicineId;
    private String medicineName;
    private Integer branchId;
    private String branchName;
    private String batchNumber;
    private String source;
    private Integer quantityBefore;
    private Integer quantityAfter;
    private Integer change;
    private String reference;
    private String notes;
    private String performedByName;
    private LocalDateTime createdAt;
}
