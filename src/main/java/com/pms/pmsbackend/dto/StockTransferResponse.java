package com.pms.pmsbackend.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class StockTransferResponse {
    private Integer id;
    private Integer medicineId;
    private String medicineName;
    private Integer fromBranchId;
    private String fromBranchName;
    private Integer toBranchId;
    private String toBranchName;
    private Integer quantity;
    private String status;
    private String notes;
    private String createdByName;
    private String completedByName;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
