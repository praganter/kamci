package dev.batuhanyetgin.debttracker.controller;

import dev.batuhanyetgin.debttracker.dto.request.CreateDebtRequest;
import dev.batuhanyetgin.debttracker.dto.request.UpdateDebtRequest;
import dev.batuhanyetgin.debttracker.dto.response.DebtResponse;
import dev.batuhanyetgin.debttracker.dto.response.DeleteDebtResponse;
import dev.batuhanyetgin.debttracker.dto.response.GetDebtResponse;
import dev.batuhanyetgin.debttracker.service.DebtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    ResponseEntity<DebtResponse> pay(@PathVariable final String id) {
        return ResponseEntity.ok(debtService.pay(id));
    }

    @Operation(summary = "Get all debts for the authenticated user, grouped by category")
    @ApiResponse(responseCode = "200", description = "Debts returned")
    @GetMapping("get")
    ResponseEntity<GetDebtResponse> get() {
        return ResponseEntity.ok(debtService.get());
    }
}