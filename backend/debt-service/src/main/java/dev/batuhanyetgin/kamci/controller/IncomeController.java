package dev.batuhanyetgin.kamci.controller;

import dev.batuhanyetgin.kamci.dto.request.CreateIncomeRequest;
import dev.batuhanyetgin.kamci.dto.response.IncomeResponse;
import dev.batuhanyetgin.kamci.service.IncomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("income/")
@RequiredArgsConstructor
@Tag(name = "Income", description = "User income management")
public class IncomeController {

    private final IncomeService incomeService;

    @Operation(summary = "Record a new income entry")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Income recorded"),
            @ApiResponse(responseCode = "422", description = "Validation error")
    })
    @PostMapping("save")
    ResponseEntity<IncomeResponse> save(@RequestBody @Valid final CreateIncomeRequest request) {
        return ResponseEntity.ok(incomeService.create(request));
    }

    @Operation(summary = "Get full income history (newest first)")
    @ApiResponse(responseCode = "200", description = "History returned")
    @GetMapping("history")
    ResponseEntity<List<IncomeResponse>> history() {
        return ResponseEntity.ok(incomeService.getHistory());
    }

    @Operation(summary = "Get current (latest) income record")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current income returned"),
            @ApiResponse(responseCode = "404", description = "No income record found")
    })
    @GetMapping("current")
    ResponseEntity<IncomeResponse> current() {
        return ResponseEntity.ok(incomeService.getCurrent());
    }

    @Operation(summary = "Delete an income record by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Deleted"),
            @ApiResponse(responseCode = "403", description = "Not your record"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    @DeleteMapping("delete/{id}")
    ResponseEntity<Void> delete(@PathVariable final String id) {
        incomeService.delete(id);
        return ResponseEntity.ok().build();
    }
}
