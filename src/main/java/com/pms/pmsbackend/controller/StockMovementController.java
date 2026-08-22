package com.pms.pmsbackend.controller;

import com.pms.pmsbackend.dto.StockMovementResponse;
import com.pms.pmsbackend.service.StockMovementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock-movements")
@RequiredArgsConstructor
public class StockMovementController {

    private final StockMovementService stockMovementService;

    @GetMapping
    public List<StockMovementResponse> getAll(@RequestParam(required = false) Integer medicineId) {
        if (medicineId != null) {
            return stockMovementService.findByMedicine(medicineId);
        }
        return stockMovementService.findAll();
    }
}
