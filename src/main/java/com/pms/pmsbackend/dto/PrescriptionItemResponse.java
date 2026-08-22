package com.pms.pmsbackend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PrescriptionItemResponse {
    private Integer id;
    private Integer medicineId;
    private String medicineName;
    private Integer quantity;
    private String dosageInstructions;
    private Integer availableStock; // helps the pharmacist see if it can be dispensed
}
