package com.pms.pmsbackend.service;

import com.pms.pmsbackend.dto.InsuranceClaimRequest;
import com.pms.pmsbackend.dto.InsuranceClaimResponse;
import com.pms.pmsbackend.dto.ResolveClaimRequest;
import com.pms.pmsbackend.entity.Customer;
import com.pms.pmsbackend.entity.InsuranceClaim;
import com.pms.pmsbackend.entity.InsuranceProvider;
import com.pms.pmsbackend.entity.Sale;
import com.pms.pmsbackend.repository.InsuranceClaimRepository;
import com.pms.pmsbackend.repository.InsuranceProviderRepository;
import com.pms.pmsbackend.repository.SaleRepository;
import com.pms.pmsbackend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InsuranceClaimService {

    private final InsuranceClaimRepository insuranceClaimRepository;
    private final InsuranceProviderRepository insuranceProviderRepository;
    private final SaleRepository saleRepository;
    private final UserRepository userRepository;

    @Transactional
    public InsuranceClaimResponse create(InsuranceClaimRequest request) {
        Sale sale = saleRepository.findById(request.getSaleId())
                .orElseThrow(() -> new EntityNotFoundException("Sale not found: " + request.getSaleId()));

        InsuranceProvider provider = insuranceProviderRepository.findById(request.getInsuranceProviderId())
                .orElseThrow(() -> new EntityNotFoundException("Insurance provider not found: " + request.getInsuranceProviderId()));

        Customer customer = sale.getCustomer();
        if (customer == null) {
            throw new IllegalArgumentException("This sale has no customer attached -- a claim needs a covered customer.");
        }

        BigDecimal coveragePct = request.getCoveragePctOverride() != null
                ? request.getCoveragePctOverride()
                : (customer.getInsuranceCoveragePct() != null ? customer.getInsuranceCoveragePct() : provider.getDefaultCoveragePct());

        if (coveragePct == null || coveragePct.signum() <= 0) {
            throw new IllegalArgumentException("Coverage percentage is not set for this customer or provider -- cannot calculate a claim amount.");
        }

        BigDecimal claimedAmount = sale.getTotalAmount()
                .multiply(coveragePct)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        InsuranceClaim claim = new InsuranceClaim();
        claim.setSale(sale);
        claim.setInsuranceProvider(provider);
        claim.setPolicyNumber(customer.getInsurancePolicyNumber());
        claim.setClaimedAmount(claimedAmount);
        claim.setStatus("PENDING");
        claim.setNotes(request.getNotes());
        claim.setCreatedBy(currentUser());

        InsuranceClaim saved = insuranceClaimRepository.save(claim);
        return toResponse(saved);
    }

    @Transactional
    public InsuranceClaimResponse submit(Integer id) {
        InsuranceClaim claim = getOrThrow(id);
        if (!"PENDING".equals(claim.getStatus())) {
            throw new IllegalArgumentException("Only a PENDING claim can be submitted (current status: " + claim.getStatus() + ").");
        }
        claim.setStatus("SUBMITTED");
        claim.setSubmittedAt(LocalDateTime.now());
        return toResponse(insuranceClaimRepository.save(claim));
    }

    @Transactional
    public InsuranceClaimResponse resolve(Integer id, ResolveClaimRequest request) {
        InsuranceClaim claim = getOrThrow(id);
        if ("APPROVED".equals(request.getStatus()) || "PAID".equals(request.getStatus())) {
            if (request.getApprovedAmount() == null) {
                throw new IllegalArgumentException("Approved amount is required when approving or marking a claim as paid.");
            }
            claim.setApprovedAmount(request.getApprovedAmount());
        }
        claim.setStatus(request.getStatus());
        claim.setNotes(request.getNotes() != null ? request.getNotes() : claim.getNotes());
        claim.setResolvedBy(currentUser());
        claim.setResolvedAt(LocalDateTime.now());
        return toResponse(insuranceClaimRepository.save(claim));
    }

    public List<InsuranceClaimResponse> findAll() {
        return insuranceClaimRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private InsuranceClaim getOrThrow(Integer id) {
        return insuranceClaimRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Insurance claim not found: " + id));
    }

    private com.pms.pmsbackend.entity.User currentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : null;
        return username != null ? userRepository.findByUsername(username).orElse(null) : null;
    }

    private InsuranceClaimResponse toResponse(InsuranceClaim claim) {
        InsuranceClaimResponse dto = new InsuranceClaimResponse();
        dto.setId(claim.getId());
        dto.setSaleId(claim.getSale().getId());
        dto.setSaleTotalAmount(claim.getSale().getTotalAmount());
        dto.setCustomerName(claim.getSale().getCustomer() != null ? claim.getSale().getCustomer().getName() : null);
        dto.setInsuranceProviderId(claim.getInsuranceProvider().getId());
        dto.setInsuranceProviderName(claim.getInsuranceProvider().getName());
        dto.setPolicyNumber(claim.getPolicyNumber());
        dto.setClaimedAmount(claim.getClaimedAmount());
        dto.setApprovedAmount(claim.getApprovedAmount());
        dto.setStatus(claim.getStatus());
        dto.setNotes(claim.getNotes());
        dto.setCreatedByName(claim.getCreatedBy() != null ? claim.getCreatedBy().getFullName() : null);
        dto.setResolvedByName(claim.getResolvedBy() != null ? claim.getResolvedBy().getFullName() : null);
        dto.setCreatedAt(claim.getCreatedAt());
        dto.setSubmittedAt(claim.getSubmittedAt());
        dto.setResolvedAt(claim.getResolvedAt());
        return dto;
    }
}
