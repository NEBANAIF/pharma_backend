package com.pms.pmsbackend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class ExpenseResponse {
    private Integer id;
    private String category;
    private String description;
    private BigDecimal amount;
    private LocalDate expenseDate;
    private Integer createdById;
    private String createdByName;
    private LocalDateTime createdAt;
}
