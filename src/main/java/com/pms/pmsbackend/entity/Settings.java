package com.pms.pmsbackend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pharmacy-wide configuration. This table only ever holds a single row
 * (id = 1) -- there's one pharmacy per deployment, so a singleton row is
 * simpler than a proper key/value settings table for what's a small, fixed
 * set of fields.
 */
@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "settings")
@Getter
@Setter
@NoArgsConstructor
public class Settings {

    @Id
    private Integer id = 1;

    // --- Pharmacy information ---
    @Column(name = "pharmacy_name")
    private String pharmacyName = "My Pharmacy";

    private String address;
    private String phone;
    private String email;

    // --- Currency & tax ---
    @Column(name = "currency_code")
    private String currencyCode = "USD";

    @Column(name = "currency_symbol")
    private String currencySymbol = "$";

    @Column(name = "default_tax_percent")
    private BigDecimal defaultTaxPercent = BigDecimal.ZERO;

    // --- Receipt template ---
    @Column(name = "receipt_header")
    private String receiptHeader;

    @Column(name = "receipt_footer")
    private String receiptFooter;

    // --- Units of measure (comma-separated list shown as dropdown options
    // on the medicine form, e.g. "Tablet,Bottle,Box,Strip,Vial") ---
    @Column(name = "units_of_measure")
    private String unitsOfMeasure = "Tablet,Bottle,Box,Strip,Vial,Ampoule";

    // --- Email notifications ---
    @Column(name = "email_notifications_enabled")
    private Boolean emailNotificationsEnabled = false;

    @Column(name = "smtp_host")
    private String smtpHost;

    @Column(name = "smtp_port")
    private Integer smtpPort;

    @Column(name = "smtp_username")
    private String smtpUsername;

    @Column(name = "smtp_password")
    private String smtpPassword;

    // --- SMS notifications ---
    @Column(name = "sms_notifications_enabled")
    private Boolean smsNotificationsEnabled = false;

    @Column(name = "sms_provider")
    private String smsProvider;

    @Column(name = "sms_api_key")
    private String smsApiKey;

    @Column(name = "sms_sender_id")
    private String smsSenderId;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    @PrePersist
    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
