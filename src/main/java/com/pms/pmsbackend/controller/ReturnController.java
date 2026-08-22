package com.pms.pmsbackend.controller;

import com.pms.pmsbackend.dto.ReturnRequest;
import com.pms.pmsbackend.dto.ReturnResponse;
import com.pms.pmsbackend.service.ReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/returns")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;

    @GetMapping
    public List<ReturnResponse> getAll() {
        return returnService.findAll();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'PHARMACIST', 'MANAGER')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReturnResponse create(@Valid @RequestBody ReturnRequest request) {
        return returnService.create(request);
    }
}
