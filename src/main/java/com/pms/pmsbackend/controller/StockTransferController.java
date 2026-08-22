package com.pms.pmsbackend.controller;

import com.pms.pmsbackend.dto.StockTransferRequest;
import com.pms.pmsbackend.dto.StockTransferResponse;
import com.pms.pmsbackend.service.StockTransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock-transfers")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STOREKEEPER', 'MANAGER')")
public class StockTransferController {

    private final StockTransferService stockTransferService;

    @GetMapping
    public List<StockTransferResponse> getAll() {
        return stockTransferService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StockTransferResponse create(@Valid @RequestBody StockTransferRequest request) {
        return stockTransferService.create(request);
    }

    @PostMapping("/{id}/complete")
    public StockTransferResponse complete(@PathVariable Integer id) {
        return stockTransferService.complete(id);
    }

    @PostMapping("/{id}/cancel")
    public StockTransferResponse cancel(@PathVariable Integer id) {
        return stockTransferService.cancel(id);
    }
}
