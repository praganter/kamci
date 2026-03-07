package dev.batuhanyetgin.debttracker.controller;

import dev.batuhanyetgin.debttracker.dto.request.CreateDebtRequest;
import dev.batuhanyetgin.debttracker.dto.request.UpdateDebtRequest;
import dev.batuhanyetgin.debttracker.dto.response.DebtResponse;
import dev.batuhanyetgin.debttracker.dto.response.DeleteDebtResponse;
import dev.batuhanyetgin.debttracker.dto.response.GetDebtResponse;
import dev.batuhanyetgin.debttracker.service.DebtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping("debt/")
@RequiredArgsConstructor
public class DebtController {

    private final DebtService debtService;
    private final MessageSource messageSource;

    @PostMapping("save")
    ResponseEntity<DebtResponse> save(@RequestBody @Valid final CreateDebtRequest request) {
        return ResponseEntity.ok(debtService.create(request));
    }

    @PostMapping("update")
    ResponseEntity<DebtResponse> update(@RequestBody @Valid final UpdateDebtRequest request) {
        return ResponseEntity.ok(debtService.update(request));
    }

    @DeleteMapping("delete/{id}")
    ResponseEntity<DeleteDebtResponse> delete(@PathVariable final String id) {
        debtService.delete(id);

        final String msg = messageSource.getMessage(
                "debt.delete.success",
                null,
                "Delete successful",
                Locale.ENGLISH);

        return ResponseEntity.ok(DeleteDebtResponse.builder().message(msg).build());
    }

    @GetMapping("get")
    ResponseEntity<GetDebtResponse> get() {
        return ResponseEntity.ok(debtService.get());
    }
}