package com.pms.pmsbackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReceivePurchaseRequest {

    @NotEmpty(message = "At least one item must be specified to receive")
    @Valid
    private List<ReceiveItemRequest> items;
}
