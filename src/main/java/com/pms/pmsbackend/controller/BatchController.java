package com.pms.pmsbackend.controller;

import com.pms.pmsbackend.dto.BatchDto;
import com.pms.pmsbackend.service.BatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/batches")
@RequiredArgsConstructor
public class BatchController {

    private final BatchService batchService;

    @GetMapping
    public List<BatchDto> getByMedicine(@RequestParam Integer medicineId) {
        return batchService.findByMedicine(medicineId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'STOREKEEPER', 'MANAGER')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BatchDto create(@Valid @RequestBody BatchDto dto) {
        return batchService.create(dto);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'STOREKEEPER', 'MANAGER')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        batchService.delete(id);
    }
}
