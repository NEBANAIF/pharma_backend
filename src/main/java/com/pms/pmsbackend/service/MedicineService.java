package com.pms.pmsbackend.service;

import com.pms.pmsbackend.dto.MedicineDto;
import com.pms.pmsbackend.entity.Category;
import com.pms.pmsbackend.entity.Medicine;
import com.pms.pmsbackend.repository.BatchRepository;
import com.pms.pmsbackend.repository.CategoryRepository;
import com.pms.pmsbackend.repository.MedicineRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedicineService {

    private final MedicineRepository medicineRepository;
    private final CategoryRepository categoryRepository;
    private final BatchRepository batchRepository;
    private final SecureRandom random = new SecureRandom();

    public List<MedicineDto> findAll() {
        return medicineRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public MedicineDto findById(Integer id) {
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Medicine not found: " + id));
        return toDto(medicine);
    }

    public MedicineDto findByBarcode(String barcode) {
        Medicine medicine = medicineRepository.findByBarcode(barcode)
                .orElseThrow(() -> new EntityNotFoundException("No medicine found with barcode: " + barcode));
        return toDto(medicine);
    }

    public MedicineDto create(MedicineDto dto) {
        Medicine medicine = new Medicine();
        applyDtoToEntity(dto, medicine);
        if (medicine.getBarcode() == null || medicine.getBarcode().isBlank()) {
            medicine.setBarcode(generateUniqueBarcode());
        }
        Medicine saved = medicineRepository.save(medicine);
        return toDto(saved);
    }

    /**
     * Assigns a fresh, unique EAN-13 style barcode to a medicine that doesn't have one yet
     * (e.g. one created before barcode/label printing existed, or imported without a code).
     */
    public MedicineDto regenerateBarcode(Integer id) {
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Medicine not found: " + id));
        medicine.setBarcode(generateUniqueBarcode());
        Medicine saved = medicineRepository.save(medicine);
        return toDto(saved);
    }

    /**
     * Generates a random 13-digit, Code128/EAN-13-compatible numeric barcode with a valid
     * EAN-13 check digit, retrying on the rare collision with an existing barcode.
     */
    private String generateUniqueBarcode() {
        String candidate;
        int attempts = 0;
        do {
            StringBuilder sb = new StringBuilder("20"); // 20-29 prefix range is reserved for in-store use
            for (int i = 0; i < 10; i++) {
                sb.append(random.nextInt(10));
            }
            candidate = sb.toString() + computeEan13CheckDigit(sb.toString());
            attempts++;
        } while (medicineRepository.findByBarcode(candidate).isPresent() && attempts < 20);
        return candidate;
    }

    private int computeEan13CheckDigit(String twelveDigits) {
        int sum = 0;
        for (int i = 0; i < twelveDigits.length(); i++) {
            int digit = Character.getNumericValue(twelveDigits.charAt(i));
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        int mod = sum % 10;
        return mod == 0 ? 0 : 10 - mod;
    }

    public MedicineDto update(Integer id, MedicineDto dto) {
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Medicine not found: " + id));
        applyDtoToEntity(dto, medicine);
        Medicine saved = medicineRepository.save(medicine);
        return toDto(saved);
    }

    public void delete(Integer id) {
        if (!medicineRepository.existsById(id)) {
            throw new EntityNotFoundException("Medicine not found: " + id);
        }
        medicineRepository.deleteById(id);
    }

    private void applyDtoToEntity(MedicineDto dto, Medicine medicine) {
        medicine.setName(dto.getName());
        medicine.setGenericName(dto.getGenericName());
        medicine.setBrand(dto.getBrand());
        medicine.setBarcode(dto.getBarcode());
        medicine.setUnit(dto.getUnit());
        medicine.setPurchasePrice(dto.getPurchasePrice());
        medicine.setSellingPrice(dto.getSellingPrice());
        if (dto.getTaxPercent() != null) medicine.setTaxPercent(dto.getTaxPercent());
        if (dto.getReorderLevel() != null) medicine.setReorderLevel(dto.getReorderLevel());
        medicine.setImageUrl(dto.getImageUrl());
        if (dto.getIsActive() != null) medicine.setIsActive(dto.getIsActive());

        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Category not found: " + dto.getCategoryId()));
            medicine.setCategory(category);
        } else {
            medicine.setCategory(null);
        }
    }

    private MedicineDto toDto(Medicine medicine) {
        MedicineDto dto = new MedicineDto();
        dto.setId(medicine.getId());
        dto.setName(medicine.getName());
        dto.setGenericName(medicine.getGenericName());
        dto.setBrand(medicine.getBrand());
        dto.setCategoryId(medicine.getCategory() != null ? medicine.getCategory().getId() : null);
        dto.setCategoryName(medicine.getCategory() != null ? medicine.getCategory().getName() : null);
        dto.setBarcode(medicine.getBarcode());
        dto.setUnit(medicine.getUnit());
        dto.setPurchasePrice(medicine.getPurchasePrice());
        dto.setSellingPrice(medicine.getSellingPrice());
        dto.setTaxPercent(medicine.getTaxPercent());
        dto.setReorderLevel(medicine.getReorderLevel());
        dto.setImageUrl(medicine.getImageUrl());
        dto.setIsActive(medicine.getIsActive());
        dto.setCurrentStock(batchRepository.sumQuantityByMedicineId(medicine.getId()));
        return dto;
    }
}
