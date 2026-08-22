package com.pms.pmsbackend.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class PrescriptionResponse {
    private Integer id;
    private Integer customerId;
    private String customerName;
    private Integer doctorId;
    private String doctorName;
    private String status;
    private String notes;
    private String createdByName;
    private String verifiedByName;
    private LocalDateTime createdAt;
    private Integer saleId; // set once dispensed
    private List<PrescriptionItemResponse> items;
}
