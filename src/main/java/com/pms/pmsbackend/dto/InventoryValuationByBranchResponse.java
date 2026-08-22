package com.pms.pmsbackend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class InventoryValuationByBranchResponse {
    private List<BranchValuation> branches;

    @Getter
    @Setter
    public static class BranchValuation {
        private Integer branchId;
        private String branchName;
        private BigDecimal totalCostValue;
        private BigDecimal totalRetailValue;
        private List<InventoryValuationResponse.Item> items;
    }
}
