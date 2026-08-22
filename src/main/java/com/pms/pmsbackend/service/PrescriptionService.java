package com.pms.pmsbackend.service;

import com.pms.pmsbackend.dto.*;
import com.pms.pmsbackend.entity.*;
import com.pms.pmsbackend.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final CustomerRepository customerRepository;
    private final DoctorRepository doctorRepository;
    private final MedicineRepository medicineRepository;
    private final BatchRepository batchRepository;
    private final UserRepository userRepository;
    private final SaleService saleService;
    private final SaleRepository saleRepository;

    @Transactional
    public PrescriptionResponse create(PrescriptionRequest request) {
        Prescription prescription = new Prescription();

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + request.getCustomerId()));
        prescription.setCustomer(customer);

        if (request.getDoctorId() != null) {
            Doctor doctor = doctorRepository.findById(request.getDoctorId())
                    .orElseThrow(() -> new EntityNotFoundException("Doctor not found: " + request.getDoctorId()));
            prescription.setDoctor(doctor);
        }

        prescription.setNotes(request.getNotes());
        prescription.setStatus("PENDING");
        currentUser().ifPresent(prescription::setCreatedBy);

        List<PrescriptionItem> items = new ArrayList<>();
        for (PrescriptionItemRequest itemReq : request.getItems()) {
            Medicine medicine = medicineRepository.findById(itemReq.getMedicineId())
                    .orElseThrow(() -> new EntityNotFoundException("Medicine not found: " + itemReq.getMedicineId()));

            PrescriptionItem item = new PrescriptionItem();
            item.setPrescription(prescription);
            item.setMedicine(medicine);
            item.setQuantity(itemReq.getQuantity());
            item.setDosageInstructions(itemReq.getDosageInstructions());
            items.add(item);
        }
        prescription.setItems(items);

        Prescription saved = prescriptionRepository.save(prescription);
        return toResponse(saved, null);
    }

    @Transactional
    public PrescriptionResponse verify(Integer id) {
        Prescription prescription = getOrThrow(id);
        requireStatus(prescription, "PENDING", "verified");
        prescription.setStatus("VERIFIED");
        currentUser().ifPresent(prescription::setVerifiedBy);
        return toResponse(prescriptionRepository.save(prescription), null);
    }

    @Transactional
    public PrescriptionResponse cancel(Integer id) {
        Prescription prescription = getOrThrow(id);
        if ("DISPENSED".equals(prescription.getStatus())) {
            throw new IllegalArgumentException("A dispensed prescription cannot be cancelled.");
        }
        prescription.setStatus("CANCELLED");
        return toResponse(prescriptionRepository.save(prescription), null);
    }

    @Transactional
    public PrescriptionResponse dispense(Integer id) {
        Prescription prescription = getOrThrow(id);
        requireStatus(prescription, "VERIFIED", "dispensed");

        List<SaleItemRequest> saleItems = prescription.getItems().stream().map(item -> {
            SaleItemRequest req = new SaleItemRequest();
            req.setMedicineId(item.getMedicine().getId());
            req.setQuantity(item.getQuantity());
            return req;
        }).collect(Collectors.toList());

        Sale sale = saleService.checkoutForPrescription(prescription.getCustomer(), prescription, saleItems);

        prescription.setStatus("DISPENSED");
        Prescription saved = prescriptionRepository.save(prescription);
        return toResponse(saved, sale.getId());
    }

    public List<PrescriptionResponse> findAll() {
        return prescriptionRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(p -> toResponse(p, resolveSaleId(p)))
                .collect(Collectors.toList());
    }

    public List<PrescriptionResponse> findByCustomer(Integer customerId) {
        return prescriptionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(p -> toResponse(p, resolveSaleId(p)))
                .collect(Collectors.toList());
    }

    public PrescriptionResponse findById(Integer id) {
        Prescription p = getOrThrow(id);
        return toResponse(p, resolveSaleId(p));
    }

    private Integer resolveSaleId(Prescription p) {
        if (!"DISPENSED".equals(p.getStatus())) return null;
        return saleRepository.findByPrescriptionId(p.getId()).map(Sale::getId).orElse(null);
    }

    private Prescription getOrThrow(Integer id) {
        return prescriptionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Prescription not found: " + id));
    }

    private void requireStatus(Prescription prescription, String required, String action) {
        if (!required.equals(prescription.getStatus())) {
            throw new IllegalArgumentException(
                    "Only " + required + " prescriptions can be " + action + ". Current status: " + prescription.getStatus());
        }
    }

    private Optional<User> currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : null;
        return username != null ? userRepository.findByUsername(username) : Optional.empty();
    }

    private PrescriptionResponse toResponse(Prescription p, Integer saleId) {
        PrescriptionResponse dto = new PrescriptionResponse();
        dto.setId(p.getId());
        dto.setCustomerId(p.getCustomer().getId());
        dto.setCustomerName(p.getCustomer().getName());
        if (p.getDoctor() != null) {
            dto.setDoctorId(p.getDoctor().getId());
            dto.setDoctorName(p.getDoctor().getName());
        }
        dto.setStatus(p.getStatus());
        dto.setNotes(p.getNotes());
        dto.setCreatedByName(p.getCreatedBy() != null ? p.getCreatedBy().getFullName() : null);
        dto.setVerifiedByName(p.getVerifiedBy() != null ? p.getVerifiedBy().getFullName() : null);
        dto.setCreatedAt(p.getCreatedAt());
        dto.setSaleId(saleId);

        List<PrescriptionItemResponse> items = p.getItems().stream().map(item -> {
            PrescriptionItemResponse itemDto = new PrescriptionItemResponse();
            itemDto.setId(item.getId());
            itemDto.setMedicineId(item.getMedicine().getId());
            itemDto.setMedicineName(item.getMedicine().getName());
            itemDto.setQuantity(item.getQuantity());
            itemDto.setDosageInstructions(item.getDosageInstructions());
            itemDto.setAvailableStock(batchRepository.sumQuantityByMedicineId(item.getMedicine().getId()));
            return itemDto;
        }).collect(Collectors.toList());

        dto.setItems(items);
        return dto;
    }
}