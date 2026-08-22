package com.pms.pmsbackend.service;

import com.pms.pmsbackend.dto.CashClosingRequest;
import com.pms.pmsbackend.dto.CashClosingResponse;
import com.pms.pmsbackend.dto.TodayCashSummaryDto;
import com.pms.pmsbackend.entity.CashRegisterClosing;
import com.pms.pmsbackend.repository.CashRegisterClosingRepository;
import com.pms.pmsbackend.repository.ExpenseRepository;
import com.pms.pmsbackend.repository.SaleRepository;
import com.pms.pmsbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CashRegisterService {

    private final CashRegisterClosingRepository closingRepository;
    private final SaleRepository saleRepository;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    public TodayCashSummaryDto getTodaySummary() {
        LocalDate today = LocalDate.now();
        TodayCashSummaryDto dto = new TodayCashSummaryDto();
        dto.setCashSales(saleRepository.sumCashSalesForDate(today));
        dto.setCashExpenses(expenseRepository.sumForDate(today));

        closingRepository.findByClosingDate(today).ifPresentOrElse(
                closing -> {
                    dto.setAlreadyClosed(true);
                    dto.setExistingClosing(toResponse(closing));
                },
                () -> dto.setAlreadyClosed(false)
        );

        return dto;
    }

    @Transactional
    public CashClosingResponse closeToday(CashClosingRequest request) {
        LocalDate today = LocalDate.now();

        if (closingRepository.findByClosingDate(today).isPresent()) {
            throw new IllegalArgumentException("Today's register has already been closed.");
        }

        CashRegisterClosing closing = new CashRegisterClosing();
        closing.setClosingDate(today);
        closing.setOpeningBalance(request.getOpeningBalance());

        BigDecimal cashSales = saleRepository.sumCashSalesForDate(today);
        BigDecimal cashExpenses = expenseRepository.sumForDate(today);
        closing.setCashSales(cashSales);
        closing.setCashExpenses(cashExpenses);

        BigDecimal expectedCash = request.getOpeningBalance().add(cashSales).subtract(cashExpenses);
        closing.setExpectedCash(expectedCash);
        closing.setActualCash(request.getActualCash());
        closing.setDifference(request.getActualCash().subtract(expectedCash));
        closing.setNotes(request.getNotes());

        String username = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : null;
        if (username != null) {
            userRepository.findByUsername(username).ifPresent(closing::setClosedBy);
        }

        CashRegisterClosing saved = closingRepository.save(closing);
        return toResponse(saved);
    }

    public List<CashClosingResponse> findAll() {
        return closingRepository.findAllByOrderByClosingDateDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private CashClosingResponse toResponse(CashRegisterClosing c) {
        CashClosingResponse dto = new CashClosingResponse();
        dto.setId(c.getId());
        dto.setClosingDate(c.getClosingDate());
        dto.setOpeningBalance(c.getOpeningBalance());
        dto.setCashSales(c.getCashSales());
        dto.setCashExpenses(c.getCashExpenses());
        dto.setExpectedCash(c.getExpectedCash());
        dto.setActualCash(c.getActualCash());
        dto.setDifference(c.getDifference());
        dto.setNotes(c.getNotes());
        dto.setClosedByName(c.getClosedBy() != null ? c.getClosedBy().getFullName() : null);
        return dto;
    }
}
