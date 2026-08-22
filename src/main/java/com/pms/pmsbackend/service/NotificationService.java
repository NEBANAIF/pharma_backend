package com.pms.pmsbackend.service;

import com.pms.pmsbackend.dto.MedicineDto;
import com.pms.pmsbackend.dto.NotificationDto;
import com.pms.pmsbackend.entity.Batch;
import com.pms.pmsbackend.repository.BatchRepository;
import com.pms.pmsbackend.repository.PurchaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int EXPIRY_WARNING_DAYS = 30;

    private final MedicineService medicineService;
    private final BatchRepository batchRepository;
    private final PurchaseRepository purchaseRepository;

    public List<NotificationDto> getNotifications() {
        List<NotificationDto> notifications = new ArrayList<>();

        List<MedicineDto> medicines = medicineService.findAll();

        for (MedicineDto m : medicines) {
            int stock = m.getCurrentStock() != null ? m.getCurrentStock() : 0;
            int reorderLevel = m.getReorderLevel() != null ? m.getReorderLevel() : 10;

            if (stock == 0) {
                notifications.add(new NotificationDto(
                        "OUT_OF_STOCK", "danger",
                        m.getName() + " is out of stock",
                        "/inventory/" + m.getId()));
            } else if (stock <= reorderLevel) {
                notifications.add(new NotificationDto(
                        "LOW_STOCK", "warning",
                        m.getName() + " is low on stock (" + stock + " left)",
                        "/inventory/" + m.getId()));
            }
        }

        LocalDate cutoff = LocalDate.now().plusDays(EXPIRY_WARNING_DAYS);
        List<Batch> expiring = batchRepository.findExpiringBySoonCutoff(cutoff);

        for (Batch b : expiring) {
            long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), b.getExpiryDate());
            String medicineName = b.getMedicine().getName();
            String link = "/inventory/" + b.getMedicine().getId();

            if (daysLeft < 0) {
                notifications.add(new NotificationDto(
                        "EXPIRED", "danger",
                        medicineName + " batch " + b.getBatchNumber() + " expired on " + b.getExpiryDate(),
                        link));
            } else {
                notifications.add(new NotificationDto(
                        "EXPIRING_SOON", "warning",
                        medicineName + " batch " + b.getBatchNumber() + " expires in " + daysLeft + " day(s)",
                        link));
            }
        }

        long pendingPurchases = purchaseRepository.countByStatus("PENDING");
        if (pendingPurchases > 0) {
            notifications.add(new NotificationDto(
                    "PENDING_PURCHASE", "warning",
                    pendingPurchases + " purchase order(s) awaiting receipt",
                    "/purchases"));
        }

        return notifications;
    }
}
