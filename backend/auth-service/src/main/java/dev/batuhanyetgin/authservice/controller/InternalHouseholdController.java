package dev.batuhanyetgin.authservice.controller;

import dev.batuhanyetgin.authservice.service.HouseholdService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Inter-service endpoints. The gateway rejects every {@code *}/internal/** path. */
@RestController
@RequestMapping("auth/internal/household/")
@RequiredArgsConstructor
@Tag(name = "Internal", description = "Inter-service only, not exposed by the gateway")
public class InternalHouseholdController {

    private final HouseholdService householdService;

    @Operation(summary = "Household member user IDs: [userId] without a household, [ownerId, partnerId] once accepted")
    @GetMapping("members/{userId}")
    ResponseEntity<List<String>> getMemberIds(@PathVariable final String userId) {
        return ResponseEntity.ok(householdService.getMemberIds(userId));
    }
}
