package com.pms.pmsbackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PurchaseRequest {

    private Integer supplierId; // optional, but strongly recommended

    private String invoiceNumber;

    private Integer branchId; // optional -- defaults to the current user's branch

    @NotEmpty(message = "Purchase must contain at least one item")
    @Valid
    private List<PurchaseItemRequest> items;
}
