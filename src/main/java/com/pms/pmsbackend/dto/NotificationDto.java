package com.pms.pmsbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class NotificationDto {
    private String type;      // LOW_STOCK, OUT_OF_STOCK, EXPIRING_SOON, EXPIRED, PENDING_PURCHASE
    private String severity;  // warning, danger
    private String message;
    private String link;      // frontend route to navigate to on click
}
