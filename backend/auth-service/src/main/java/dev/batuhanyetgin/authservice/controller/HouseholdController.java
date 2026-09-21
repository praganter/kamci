package dev.batuhanyetgin.authservice.controller;

import dev.batuhanyetgin.authservice.dto.request.InviteHouseholdRequest;
import dev.batuhanyetgin.authservice.dto.response.HouseholdResponse;
import dev.batuhanyetgin.authservice.service.HouseholdService;
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
@RequestMapping("auth/household/")
@RequiredArgsConstructor
@Tag(name = "Household", description = "Household/partnership linking for combined income view")
public class HouseholdController {

    private final HouseholdService householdService;

    @Operation(summary = "Invite a partner (by email) to form a household")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invite created"),
            @ApiResponse(responseCode = "404", description = "Invitee not found"),
            @ApiResponse(responseCode = "409", description = "Already linked, or invitee already linked")
    })
    @PostMapping("invite")
    ResponseEntity<HouseholdResponse> invite(@RequestBody @Valid final InviteHouseholdRequest request) {
        return ResponseEntity.ok(householdService.invite(request));
    }

    @Operation(summary = "Accept a pending household invite")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invite accepted"),
            @ApiResponse(responseCode = "403", description = "Not the invitee"),
            @ApiResponse(responseCode = "409", description = "Invite is not pending")
    })
    @PostMapping("accept/{householdId}")
    ResponseEntity<HouseholdResponse> accept(@PathVariable final String householdId) {
        return ResponseEntity.ok(householdService.accept(householdId));
    }

    @Operation(summary = "Decline a pending household invite")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Invite declined"),
            @ApiResponse(responseCode = "403", description = "Not the invitee"),
            @ApiResponse(responseCode = "409", description = "Invite is not pending")
    })
    @PostMapping("decline/{householdId}")
    ResponseEntity<Void> decline(@PathVariable final String householdId) {
        householdService.decline(householdId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Leave/dissolve the current household")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Household left"),
            @ApiResponse(responseCode = "404", description = "No active household")
    })
    @DeleteMapping
    ResponseEntity<Void> leave() {
        householdService.leave();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get the authenticated user's current household (pending or accepted)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Household returned"),
            @ApiResponse(responseCode = "204", description = "No active household")
    })
    @GetMapping
    ResponseEntity<HouseholdResponse> getCurrent() {
        return householdService.getCurrent()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Operation(summary = "Get household member user IDs for a user (internal inter-service use)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Returns [userId] if no household, or [ownerId, partnerId] if accepted")
    })
    @GetMapping("members/{userId}")
    ResponseEntity<List<String>> getMemberIds(@PathVariable final String userId) {
        return ResponseEntity.ok(householdService.getMemberIds(userId));
    }
}
