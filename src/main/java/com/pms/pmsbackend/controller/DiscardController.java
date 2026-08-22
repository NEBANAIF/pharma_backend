package com.pms.pmsbackend.controller;

import com.pms.pmsbackend.dto.DiscardRequest;
import com.pms.pmsbackend.dto.StockMovementResponse;
import com.pms.pmsbackend.service.DiscardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/discards")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'STOREKEEPER', 'MANAGER')")
public class DiscardController {

    private final DiscardService discardService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StockMovementResponse create(@Valid @RequestBody DiscardRequest request) {
        return discardService.create(request);
    }
}
