package com.pms.pmsbackend.service;

import com.pms.pmsbackend.dto.SettingsDto;
import com.pms.pmsbackend.entity.Settings;
import com.pms.pmsbackend.repository.SettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// A placeholder client-side marker so the "secret is already set" state is
// distinguishable from "no secret set" without ever sending the real value.
@Service
@RequiredArgsConstructor
public class SettingsService {

    private static final String SECRET_PLACEHOLDER = "••••••••";

    private final SettingsRepository settingsRepository;

    /**
     * Returns the singleton settings row, creating it with defaults on first
     * access (fresh installs won't have run a seed insert yet).
     */
    public Settings getOrCreate() {
        return settingsRepository.findById(1)
                .orElseGet(() -> settingsRepository.save(new Settings()));
    }

    public SettingsDto get() {
        return toDto(getOrCreate());
    }

    public SettingsDto update(SettingsDto dto) {
        Settings settings = getOrCreate();

        settings.setPharmacyName(dto.getPharmacyName());
        settings.setAddress(dto.getAddress());
        settings.setPhone(dto.getPhone());
        settings.setEmail(dto.getEmail());
        settings.setCurrencyCode(dto.getCurrencyCode());
        settings.setCurrencySymbol(dto.getCurrencySymbol());
        settings.setDefaultTaxPercent(dto.getDefaultTaxPercent() != null ? dto.getDefaultTaxPercent() : settings.getDefaultTaxPercent());
        settings.setReceiptHeader(dto.getReceiptHeader());
        settings.setReceiptFooter(dto.getReceiptFooter());
        settings.setUnitsOfMeasure(dto.getUnitsOfMeasure());

        settings.setEmailNotificationsEnabled(Boolean.TRUE.equals(dto.getEmailNotificationsEnabled()));
        settings.setSmtpHost(dto.getSmtpHost());
        settings.setSmtpPort(dto.getSmtpPort());
        settings.setSmtpUsername(dto.getSmtpUsername());
        // Only overwrite the stored password if the client actually sent a new
        // one -- the GET response never contains the real secret, so a blank
        // field here means "user didn't touch this", not "clear it".
        if (dto.getSmtpPassword() != null && !dto.getSmtpPassword().isBlank()
                && !dto.getSmtpPassword().equals(SECRET_PLACEHOLDER)) {
            settings.setSmtpPassword(dto.getSmtpPassword());
        }

        settings.setSmsNotificationsEnabled(Boolean.TRUE.equals(dto.getSmsNotificationsEnabled()));
        settings.setSmsProvider(dto.getSmsProvider());
        settings.setSmsSenderId(dto.getSmsSenderId());
        if (dto.getSmsApiKey() != null && !dto.getSmsApiKey().isBlank()
                && !dto.getSmsApiKey().equals(SECRET_PLACEHOLDER)) {
            settings.setSmsApiKey(dto.getSmsApiKey());
        }

        Settings saved = settingsRepository.save(settings);
        return toDto(saved);
    }

    private SettingsDto toDto(Settings settings) {
        SettingsDto dto = new SettingsDto();
        dto.setPharmacyName(settings.getPharmacyName());
        dto.setAddress(settings.getAddress());
        dto.setPhone(settings.getPhone());
        dto.setEmail(settings.getEmail());
        dto.setCurrencyCode(settings.getCurrencyCode());
        dto.setCurrencySymbol(settings.getCurrencySymbol());
        dto.setDefaultTaxPercent(settings.getDefaultTaxPercent());
        dto.setReceiptHeader(settings.getReceiptHeader());
        dto.setReceiptFooter(settings.getReceiptFooter());
        dto.setUnitsOfMeasure(settings.getUnitsOfMeasure());

        dto.setEmailNotificationsEnabled(settings.getEmailNotificationsEnabled());
        dto.setSmtpHost(settings.getSmtpHost());
        dto.setSmtpPort(settings.getSmtpPort());
        dto.setSmtpUsername(settings.getSmtpUsername());
        dto.setSmtpPassword(settings.getSmtpPassword() != null && !settings.getSmtpPassword().isBlank() ? SECRET_PLACEHOLDER : "");

        dto.setSmsNotificationsEnabled(settings.getSmsNotificationsEnabled());
        dto.setSmsProvider(settings.getSmsProvider());
        dto.setSmsSenderId(settings.getSmsSenderId());
        dto.setSmsApiKey(settings.getSmsApiKey() != null && !settings.getSmsApiKey().isBlank() ? SECRET_PLACEHOLDER : "");

        return dto;
    }
}
