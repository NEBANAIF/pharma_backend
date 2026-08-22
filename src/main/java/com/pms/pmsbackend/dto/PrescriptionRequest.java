package com.pms.pmsbackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PrescriptionRequest {

    @NotNull(message = "Customer (patient) is required")
    private Integer customerId;

    private Integer doctorId;

    private String notes;

    @NotEmpty(message = "Prescription must contain at least one medicine")
    @Valid
    private List<PrescriptionItemRequest> items;
}
