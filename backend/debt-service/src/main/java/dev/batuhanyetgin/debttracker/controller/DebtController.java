package dev.batuhanyetgin.debttracker.controller;

import dev.batuhanyetgin.debttracker.dto.request.CreateDebtRequest;
import dev.batuhanyetgin.debttracker.dto.request.PayDebtRequest;
import dev.batuhanyetgin.debttracker.dto.request.UpdateDebtRequest;
import dev.batuhanyetgin.debttracker.dto.response.*;
import dev.batuhanyetgin.debttracker.service.DebtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("debt/")
@RequiredArgsConstructor
@Tag(name = "Debt", description = "Debt management")
public class DebtController {

    private final DebtService debtService;
    private final MessageSource messageSource;

    @Operation(summary = "Create a new debt")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Debt created"),
            @ApiResponse(responseCode = "400", description = "Debt limit exceeded or invalid category"),
            @ApiResponse(responseCode = "403", description = "No write access to the category"),
            @ApiResponse(responseCode = "422", description = "Validation error")
    })
    @PostMapping("save")
    ResponseEntity<DebtResponse> save(@RequestBody @Valid final CreateDebtRequest request) {
        return ResponseEntity.ok(debtService.create(request));
    }

    @Operation(summary = "Update an existing debt")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Debt updated"),
            @ApiResponse(responseCode = "403", description = "No write access"),
            @ApiResponse(responseCode = "404", description = "Debt not found"),
            @ApiResponse(responseCode = "422", description = "Validation error")
    })
    @PostMapping("update")
    ResponseEntity<DebtResponse> update(@RequestBody @Valid final UpdateDebtRequest request) {
        return ResponseEntity.ok(debtService.update(request));
    }

    @Operation(summary = "Delete a debt by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Debt deleted"),
            @ApiResponse(responseCode = "403", description = "No write access"),
            @ApiResponse(responseCode = "404", description = "Debt not found")
    })
    @DeleteMapping("delete/{id}")
    ResponseEntity<DeleteDebtResponse> delete(@PathVariable final String id) {
        debtService.delete(id);
        final String msg = messageSource.getMessage("debt.delete.success", null, "Delete successful", Locale.ENGLISH);
        return ResponseEntity.ok(DeleteDebtResponse.builder().message(msg).build());
    }

    @Operation(summary = "Record a payment for a debt")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment recorded"),
            @ApiResponse(responseCode = "403", description = "No write access"),
            @ApiResponse(responseCode = "404", description = "Debt not found")
    })
    @PostMapping("pay/{id}")
    ResponseEntity<DebtResponse> pay(@PathVariable final String id,
                                     @RequestBody(required = false) @Valid final PayDebtRequest request) {
        return ResponseEntity.ok(debtService.pay(id, request));
    }

    @Operation(summary = "Mark current period as skipped (unpaid) for a debt")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Period skipped"),
            @ApiResponse(responseCode = "403", description = "No write access"),
            @ApiResponse(responseCode = "404", description = "Debt not found"),
            @ApiResponse(responseCode = "409", description = "Debt completed, auto-payment enabled, or period already recorded")
    })
    @PostMapping("skip/{id}")
    ResponseEntity<DebtResponse> skip(@PathVariable final String id) {
        return ResponseEntity.ok(debtService.skip(id));
    }

    @Operation(summary = "Get a single debt by ID with full payment history")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Debt returned"),
            @ApiResponse(responseCode = "403", description = "No read access"),
            @ApiResponse(responseCode = "404", description = "Debt not found")
    })
    @GetMapping("{id}")
    ResponseEntity<DebtDetailResponse> getById(@PathVariable final String id) {
        return ResponseEntity.ok(debtService.getById(id));
    }

    @Operation(summary = "Get all debts for the authenticated user, grouped by category")
    @ApiResponse(responseCode = "200", description = "Debts returned")
    @GetMapping("get")
    ResponseEntity<GetDebtResponse> get() {
        return ResponseEntity.ok(debtService.get());
    }

    @Operation(summary = "Get payment history for a specific debt filtered by date range")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment history returned"),
            @ApiResponse(responseCode = "403", description = "No read access"),
            @ApiResponse(responseCode = "404", description = "Debt not found")
    })
    @GetMapping("{id}/payments")
    ResponseEntity<List<PaymentHistoryResponse>> getPaymentHistory(
            @PathVariable final String id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final LocalDateTime to) {
        return ResponseEntity.ok(debtService.getPaymentHistory(id, from, to));
    }

    @Operation(summary = "Get monthly debt summary with optional debt-to-income ratio")
    @ApiResponse(responseCode = "200", description = "Monthly summary returned")
    @GetMapping("summary")
    ResponseEntity<MonthlySummaryResponse> getMonthlySummary(@RequestParam final String month) {
        return ResponseEntity.ok(debtService.getMonthlySummary(month));
    }

}