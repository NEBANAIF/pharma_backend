package com.pms.pmsbackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SettingsDto {

    @NotBlank(message = "Pharmacy name is required")
    private String pharmacyName;

    private String address;
    private String phone;
    private String email;

    @NotBlank(message = "Currency code is required")
    private String currencyCode;

    @NotBlank(message = "Currency symbol is required")
    private String currencySymbol;

    private BigDecimal defaultTaxPercent;

    private String receiptHeader;
    private String receiptFooter;
    private String unitsOfMeasure;

    private Boolean emailNotificationsEnabled;
    private String smtpHost;
    private Integer smtpPort;
    private String smtpUsername;
    // Never sent back to the client with its real value -- see SettingsService.
    // A blank value on update means "leave the stored secret unchanged".
    private String smtpPassword;

    private Boolean smsNotificationsEnabled;
    private String smsProvider;
    private String smsApiKey;
    private String smsSenderId;
}
