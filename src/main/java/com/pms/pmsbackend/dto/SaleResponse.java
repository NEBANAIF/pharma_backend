package com.pms.pmsbackend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class SaleResponse {
    private Integer id;
    private LocalDateTime saleDate;
    private Integer customerId;
    private String customerName;
    private String cashierName;
    private Integer branchId;
    private String branchName;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private String transactionNumber;
    private Integer bankId;
    private String bankName;
    private String paymentStatus;
    private BigDecimal amountPaid;
    private BigDecimal amountDue;
    private Integer pointsEarned;
    private Integer pointsRedeemed;
    private Integer customerLoyaltyPoints;
    private List<SaleItemResponse> items;
}