package dev.batuhanyetgin.authservice.service;

import dev.batuhanyetgin.authservice.document.AuthDocument;
import dev.batuhanyetgin.authservice.document.HouseholdDocument;
import dev.batuhanyetgin.authservice.dto.request.InviteHouseholdRequest;
import dev.batuhanyetgin.authservice.dto.response.HouseholdResponse;
import dev.batuhanyetgin.authservice.enums.HouseholdStatus;
import dev.batuhanyetgin.authservice.producer.AuthEventProducer;
import dev.batuhanyetgin.authservice.repositoryservice.AuthRepositoryService;
import dev.batuhanyetgin.authservice.repositoryservice.HouseholdRepositoryService;
import dev.batuhanyetgin.commonmodule.event.HouseholdInviteEvent;
import dev.batuhanyetgin.commonmodule.event.RabbitMQConstants;
import dev.batuhanyetgin.commonmodule.exception.BaseException;
import dev.batuhanyetgin.commonmodule.util.RequestContextUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HouseholdService {

    private final HouseholdRepositoryService householdRS;
    private final AuthRepositoryService authRS;
    private final AuthEventProducer authEventProducer;

    public HouseholdResponse invite(final InviteHouseholdRequest request) {
        final String userId = RequestContextUtil.getUserId();
        final AuthDocument owner = authRS.findById(userId);

        final AuthDocument invitee = authRS.findByEmailOptional(request.getEmail())
                .orElseThrow(() -> BaseException.notFound("error.household.invitee_not_found", request.getEmail()));

        if (invitee.getId().equals(userId)) {
            throw BaseException.conflict("error.household.cannot_invite_self");
        }

        if (householdRS.findActiveForUser(userId).isPresent()) {
            throw BaseException.conflict("error.household.already_linked");
        }
        if (householdRS.findActiveForUser(invitee.getId()).isPresent()) {
            throw BaseException.conflict("error.household.invitee_already_linked");
        }

        final HouseholdDocument household = householdRS.save(HouseholdDocument.builder()
                .ownerId(userId)
                .ownerEmail(owner.getEmail())
                .partnerId(invitee.getId())
                .partnerEmail(invitee.getEmail())
                .status(HouseholdStatus.PENDING)
                .build());

        authEventProducer.produce(
                RabbitMQConstants.NOTIFICATION_EXCHANGE,
                RabbitMQConstants.HOUSEHOLD_INVITE_ROUTING_KEY,
                HouseholdInviteEvent.builder()
                        .householdId(household.getId())
                        .inviterUserId(userId)
                        .inviterEmail(owner.getEmail())
                        .inviteeUserId(invitee.getId())
                        .inviteeEmail(invitee.getEmail())
                        .locale(RequestContextUtil.getLocale())
                        .build()
        );

        log.info("Household invite {} created by {} for {}", household.getId(), userId, invitee.getId());
        return toResponse(household, userId);
    }

    public HouseholdResponse accept(final String householdId) {
        final String userId = RequestContextUtil.getUserId();
        final HouseholdDocument household = householdRS.findById(householdId);

        if (!household.getPartnerId().equals(userId)) {
            throw BaseException.forbidden("error.household.permission_denied");
        }
        if (household.getStatus() != HouseholdStatus.PENDING) {
            throw BaseException.conflict("error.household.not_pending");
        }

        household.setStatus(HouseholdStatus.ACCEPTED);
        householdRS.save(household);
        log.info("Household invite {} accepted by {}", householdId, userId);
        return toResponse(household, userId);
    }

    public void decline(final String householdId) {
        final String userId = RequestContextUtil.getUserId();
        final HouseholdDocument household = householdRS.findById(householdId);

        if (!household.getPartnerId().equals(userId)) {
            throw BaseException.forbidden("error.household.permission_denied");
        }
        if (household.getStatus() != HouseholdStatus.PENDING) {
            throw BaseException.conflict("error.household.not_pending");
        }

        householdRS.delete(household);
        log.info("Household invite {} declined by {}", householdId, userId);
    }

    public void leave() {
        final String userId = RequestContextUtil.getUserId();
        final HouseholdDocument household = householdRS.findActiveForUser(userId)
                .orElseThrow(() -> BaseException.notFound("error.household.not_found"));

        householdRS.delete(household);
        log.info("Household {} left/cancelled by {}", household.getId(), userId);
    }

    public Optional<HouseholdResponse> getCurrent() {
        final String userId = RequestContextUtil.getUserId();
        return householdRS.findActiveForUser(userId).map(h -> toResponse(h, userId));
    }

    public List<String> getMemberIds(final String userId) {
        return householdRS.findAcceptedForUser(userId)
                .map(h -> List.of(h.getOwnerId(), h.getPartnerId()))
                .orElse(List.of(userId));
    }

    private HouseholdResponse toResponse(final HouseholdDocument household, final String requestingUserId) {
        return HouseholdResponse.builder()
                .id(household.getId())
                .ownerId(household.getOwnerId())
                .ownerEmail(household.getOwnerEmail())
                .partnerId(household.getPartnerId())
                .partnerEmail(household.getPartnerEmail())
                .status(household.getStatus())
                .isOwner(household.getOwnerId().equals(requestingUserId))
                .build();
    }
}
