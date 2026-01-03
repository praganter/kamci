package dev.batuhanyetgin.debttracker.controller;

import dev.batuhanyetgin.debttracker.dto.request.CreateDebtRequest;
import dev.batuhanyetgin.debttracker.dto.response.CreateDebtResponse;
import dev.batuhanyetgin.debttracker.service.DebtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("debt/")
@RequiredArgsConstructor
public class DebtController {

    private final DebtService debtService;

    @PostMapping("save")
    ResponseEntity<CreateDebtResponse> save(@RequestBody @Valid final CreateDebtRequest request) {
        debtService.save(request);
        return ResponseEntity.ok(CreateDebtResponse.builder().message("başarılı save.").build());
    }

    @PostMapping("update")
    ResponseEntity<CreateDebtResponse> update(@RequestBody @Valid final CreateDebtRequest request) {
        debtService.update(request);
        return ResponseEntity.ok(CreateDebtResponse.builder().message("başarılı update.").build());
    }

    @DeleteMapping("delete/{id}")
    ResponseEntity<CreateDebtResponse> delete(@PathVariable final String id) {
        debtService.delete(id);
        return ResponseEntity.ok(CreateDebtResponse.builder().message("başarılı delete.").build());
    }
}