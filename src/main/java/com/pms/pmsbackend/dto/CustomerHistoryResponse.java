package com.pms.pmsbackend.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

// Backs Patient Management's "medication history" -- pulled together from
// the existing Prescription and Sale tables rather than duplicated storage.
@Getter
@Setter
public class CustomerHistoryResponse {
    private List<PrescriptionResponse> prescriptions;
    private List<SaleResponse> sales;
}