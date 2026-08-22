package com.pms.pmsbackend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class BestSellingItemDto {
    private Integer medicineId;
    private String medicineName;
    private Long quantitySold;
    private BigDecimal revenue;
}
