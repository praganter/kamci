package dev.batuhanyetgin.kamci.controller;

import dev.batuhanyetgin.kamci.service.AccountDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("debt/internal/")
@RequiredArgsConstructor
@Tag(name = "Internal", description = "Inter-service endpoints, not exposed through the gateway")
public class InternalAccountController {

    private final AccountDataService accountDataService;

    @Operation(summary = "Delete all debt-service data (debts, categories, income, memberships) for a user, called by auth-service on account deletion")
    @DeleteMapping("account-data/{userId}")
    ResponseEntity<Void> deleteAccountData(@PathVariable final String userId) {
        accountDataService.deleteUserData(userId);
        return ResponseEntity.noContent().build();
    }
}
