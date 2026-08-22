package com.pms.pmsbackend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class InventoryValuationResponse {
    private BigDecimal totalCostValue;
    private BigDecimal totalRetailValue;
    private List<Item> items;

    @Getter
    @Setter
    public static class Item {
        private Integer medicineId;
        private String medicineName;
        private Integer stock;
        private BigDecimal costValue;
        private BigDecimal retailValue;
    }
}
