package com.pms.pmsbackend.service;

import com.pms.pmsbackend.dto.ApprovedMedicineRequest;
import com.pms.pmsbackend.dto.ApprovedMedicineResponse;
import com.pms.pmsbackend.entity.InsuranceApprovedMedicine;
import com.pms.pmsbackend.entity.InsuranceProvider;
import com.pms.pmsbackend.entity.Medicine;
import com.pms.pmsbackend.repository.InsuranceApprovedMedicineRepository;
import com.pms.pmsbackend.repository.InsuranceProviderRepository;
import com.pms.pmsbackend.repository.MedicineRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InsuranceFormularyService {

    private final InsuranceApprovedMedicineRepository approvedMedicineRepository;
    private final InsuranceProviderRepository providerRepository;
    private final MedicineRepository medicineRepository;

    public List<ApprovedMedicineResponse> findByProvider(Integer providerId) {
        if (!providerRepository.existsById(providerId)) {
            throw new EntityNotFoundException("Insurance provider not found: " + providerId);
        }
        return approvedMedicineRepository.findByInsuranceProviderId(providerId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ApprovedMedicineResponse add(Integer providerId, ApprovedMedicineRequest request) {
        InsuranceProvider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new EntityNotFoundException("Insurance provider not found: " + providerId));
        Medicine medicine = medicineRepository.findById(request.getMedicineId())
                .orElseThrow(() -> new EntityNotFoundException("Medicine not found: " + request.getMedicineId()));

        if (approvedMedicineRepository.existsByInsuranceProviderIdAndMedicineId(providerId, medicine.getId())) {
            throw new IllegalArgumentException(medicine.getName() + " is already on this provider's approved list.");
        }

        InsuranceApprovedMedicine approved = new InsuranceApprovedMedicine();
        approved.setInsuranceProvider(provider);
        approved.setMedicine(medicine);
        approved.setCoveragePctOverride(request.getCoveragePctOverride());

        return toResponse(approvedMedicineRepository.save(approved));
    }

    public void remove(Integer providerId, Integer approvedMedicineId) {
        InsuranceApprovedMedicine approved = approvedMedicineRepository.findById(approvedMedicineId)
                .orElseThrow(() -> new EntityNotFoundException("Approved medicine entry not found: " + approvedMedicineId));

        if (!approved.getInsuranceProvider().getId().equals(providerId)) {
            throw new IllegalArgumentException("This entry does not belong to the given provider.");
        }

        approvedMedicineRepository.deleteById(approvedMedicineId);
    }

    /** Used elsewhere (e.g. POS or claims) to check coverage for a specific medicine, if ever wired in. */
    public boolean isApproved(Integer providerId, Integer medicineId) {
        return approvedMedicineRepository.existsByInsuranceProviderIdAndMedicineId(providerId, medicineId);
    }

    private ApprovedMedicineResponse toResponse(InsuranceApprovedMedicine a) {
        ApprovedMedicineResponse dto = new ApprovedMedicineResponse();
        dto.setId(a.getId());
        dto.setMedicineId(a.getMedicine().getId());
        dto.setMedicineName(a.getMedicine().getName());
        dto.setCoveragePctOverride(a.getCoveragePctOverride());
        dto.setEffectiveCoveragePct(
                a.getCoveragePctOverride() != null ? a.getCoveragePctOverride() : a.getInsuranceProvider().getDefaultCoveragePct());
        return dto;
    }
}
