package dev.batuhanyetgin.authservice.service;

import dev.batuhanyetgin.authservice.document.AuthDocument;
import dev.batuhanyetgin.authservice.document.HouseholdDocument;
import dev.batuhanyetgin.authservice.dto.request.InviteHouseholdRequest;
import dev.batuhanyetgin.authservice.dto.response.HouseholdResponse;
import dev.batuhanyetgin.authservice.enums.HouseholdStatus;
import dev.batuhanyetgin.authservice.producer.AuthEventProducer;
import dev.batuhanyetgin.authservice.repositoryservice.AuthRepositoryService;
import dev.batuhanyetgin.authservice.repositoryservice.HouseholdRepositoryService;
import dev.batuhanyetgin.commonmodule.exception.BaseException;
import dev.batuhanyetgin.commonmodule.util.RequestContextUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HouseholdServiceTest {

    @Mock private HouseholdRepositoryService householdRS;
    @Mock private AuthRepositoryService authRS;
    @Mock private AuthEventProducer authEventProducer;

    @InjectMocks
    private HouseholdService householdService;

    private static final String OWNER_ID = "owner123";
    private static final String OWNER_EMAIL = "owner@example.com";
    private static final String PARTNER_ID = "partner456";
    private static final String PARTNER_EMAIL = "partner@example.com";
    private static final String HOUSEHOLD_ID = "household789";

    private AuthDocument buildAuth(final String id, final String email) {
        final AuthDocument doc = AuthDocument.builder().email(email).password("hash").build();
        ReflectionTestUtils.setField(doc, "id", id);
        return doc;
    }

    private HouseholdDocument buildHousehold(final HouseholdStatus status) {
        final HouseholdDocument doc = HouseholdDocument.builder()
                .ownerId(OWNER_ID).ownerEmail(OWNER_EMAIL)
                .partnerId(PARTNER_ID).partnerEmail(PARTNER_EMAIL)
                .status(status).build();
        ReflectionTestUtils.setField(doc, "id", HOUSEHOLD_ID);
        return doc;
    }

    @Nested
    @DisplayName("invite")
    class Invite {

        @Test
        @DisplayName("valid invite → household saved as PENDING and event produced")
        void validInvite_savesAndProducesEvent() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);
                ctx.when(RequestContextUtil::getLocale).thenReturn("tr");

                when(authRS.findById(OWNER_ID)).thenReturn(buildAuth(OWNER_ID, OWNER_EMAIL));
                when(authRS.findByEmailOptional(PARTNER_EMAIL)).thenReturn(Optional.of(buildAuth(PARTNER_ID, PARTNER_EMAIL)));
                when(householdRS.findActiveForUser(OWNER_ID)).thenReturn(Optional.empty());
                when(householdRS.findActiveForUser(PARTNER_ID)).thenReturn(Optional.empty());
                when(householdRS.save(any(HouseholdDocument.class))).thenReturn(buildHousehold(HouseholdStatus.PENDING));

                final InviteHouseholdRequest request = new InviteHouseholdRequest();
                request.setEmail(PARTNER_EMAIL);

                final HouseholdResponse response = householdService.invite(request);

                assertThat(response.getStatus()).isEqualTo(HouseholdStatus.PENDING);
                assertThat(response.isOwner()).isTrue();
                verify(authEventProducer).produce(anyString(), anyString(), any());
            }
        }

        @Test
        @DisplayName("invitee email not registered → 404 error.household.invitee_not_found")
        void inviteeNotFound_throws404() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                when(authRS.findById(OWNER_ID)).thenReturn(buildAuth(OWNER_ID, OWNER_EMAIL));
                when(authRS.findByEmailOptional(PARTNER_EMAIL)).thenReturn(Optional.empty());

                final InviteHouseholdRequest request = new InviteHouseholdRequest();
                request.setEmail(PARTNER_EMAIL);

                assertThatThrownBy(() -> householdService.invite(request))
                        .isInstanceOf(BaseException.class)
                        .extracting(e -> ((BaseException) e).getStatus())
                        .isEqualTo(HttpStatus.NOT_FOUND);
            }
        }

        @Test
        @DisplayName("inviting self → 409 error.household.cannot_invite_self")
        void inviteSelf_throws409() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                when(authRS.findById(OWNER_ID)).thenReturn(buildAuth(OWNER_ID, OWNER_EMAIL));
                when(authRS.findByEmailOptional(OWNER_EMAIL)).thenReturn(Optional.of(buildAuth(OWNER_ID, OWNER_EMAIL)));

                final InviteHouseholdRequest request = new InviteHouseholdRequest();
                request.setEmail(OWNER_EMAIL);

                assertThatThrownBy(() -> householdService.invite(request))
                        .isInstanceOf(BaseException.class)
                        .extracting(e -> ((BaseException) e).getStatus())
                        .isEqualTo(HttpStatus.CONFLICT);
            }
        }

        @Test
        @DisplayName("owner already has an active/pending household → 409 error.household.already_linked")
        void ownerAlreadyLinked_throws409() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                when(authRS.findById(OWNER_ID)).thenReturn(buildAuth(OWNER_ID, OWNER_EMAIL));
                when(authRS.findByEmailOptional(PARTNER_EMAIL)).thenReturn(Optional.of(buildAuth(PARTNER_ID, PARTNER_EMAIL)));
                when(householdRS.findActiveForUser(OWNER_ID)).thenReturn(Optional.of(buildHousehold(HouseholdStatus.ACCEPTED)));

                final InviteHouseholdRequest request = new InviteHouseholdRequest();
                request.setEmail(PARTNER_EMAIL);

                assertThatThrownBy(() -> householdService.invite(request))
                        .isInstanceOf(BaseException.class)
                        .extracting(e -> ((BaseException) e).getStatus())
                        .isEqualTo(HttpStatus.CONFLICT);
                verifyNoInteractions(authEventProducer);
            }
        }

        @Test
        @DisplayName("invitee already has an active/pending household → 409 error.household.invitee_already_linked")
        void inviteeAlreadyLinked_throws409() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                when(authRS.findById(OWNER_ID)).thenReturn(buildAuth(OWNER_ID, OWNER_EMAIL));
                when(authRS.findByEmailOptional(PARTNER_EMAIL)).thenReturn(Optional.of(buildAuth(PARTNER_ID, PARTNER_EMAIL)));
                when(householdRS.findActiveForUser(OWNER_ID)).thenReturn(Optional.empty());
                when(householdRS.findActiveForUser(PARTNER_ID)).thenReturn(Optional.of(buildHousehold(HouseholdStatus.PENDING)));

                final InviteHouseholdRequest request = new InviteHouseholdRequest();
                request.setEmail(PARTNER_EMAIL);

                assertThatThrownBy(() -> householdService.invite(request))
                        .isInstanceOf(BaseException.class)
                        .extracting(e -> ((BaseException) e).getStatus())
                        .isEqualTo(HttpStatus.CONFLICT);
            }
        }
    }

    @Nested
    @DisplayName("accept")
    class Accept {

        @Test
        @DisplayName("partner accepts a pending invite → status becomes ACCEPTED")
        void partnerAccepts_setsAccepted() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(PARTNER_ID);

                final HouseholdDocument pending = buildHousehold(HouseholdStatus.PENDING);
                when(householdRS.findById(HOUSEHOLD_ID)).thenReturn(pending);
                when(householdRS.save(any(HouseholdDocument.class))).thenAnswer(inv -> inv.getArgument(0));

                final HouseholdResponse response = householdService.accept(HOUSEHOLD_ID);

                assertThat(response.getStatus()).isEqualTo(HouseholdStatus.ACCEPTED);
                assertThat(response.isOwner()).isFalse();
            }
        }

        @Test
        @DisplayName("non-invitee tries to accept → 403 error.household.permission_denied")
        void nonInvitee_throws403() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn("someoneElse");

                when(householdRS.findById(HOUSEHOLD_ID)).thenReturn(buildHousehold(HouseholdStatus.PENDING));

                assertThatThrownBy(() -> householdService.accept(HOUSEHOLD_ID))
                        .isInstanceOf(BaseException.class)
                        .extracting(e -> ((BaseException) e).getStatus())
                        .isEqualTo(HttpStatus.FORBIDDEN);
            }
        }

        @Test
        @DisplayName("already accepted invite → 409 error.household.not_pending")
        void alreadyAccepted_throws409() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(PARTNER_ID);

                when(householdRS.findById(HOUSEHOLD_ID)).thenReturn(buildHousehold(HouseholdStatus.ACCEPTED));

                assertThatThrownBy(() -> householdService.accept(HOUSEHOLD_ID))
                        .isInstanceOf(BaseException.class)
                        .extracting(e -> ((BaseException) e).getStatus())
                        .isEqualTo(HttpStatus.CONFLICT);
            }
        }
    }

    @Nested
    @DisplayName("decline / leave")
    class DeclineAndLeave {

        @Test
        @DisplayName("partner declines a pending invite → household deleted")
        void partnerDeclines_deletes() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(PARTNER_ID);

                final HouseholdDocument pending = buildHousehold(HouseholdStatus.PENDING);
                when(householdRS.findById(HOUSEHOLD_ID)).thenReturn(pending);

                householdService.decline(HOUSEHOLD_ID);

                verify(householdRS).delete(pending);
            }
        }

        @Test
        @DisplayName("leave with active household → deleted")
        void leave_deletesActiveHousehold() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                final HouseholdDocument accepted = buildHousehold(HouseholdStatus.ACCEPTED);
                when(householdRS.findActiveForUser(OWNER_ID)).thenReturn(Optional.of(accepted));

                householdService.leave();

                verify(householdRS).delete(accepted);
            }
        }

        @Test
        @DisplayName("leave with no active household → 404 error.household.not_found")
        void leave_noHousehold_throws404() {
            try (MockedStatic<RequestContextUtil> ctx = mockStatic(RequestContextUtil.class)) {
                ctx.when(RequestContextUtil::getUserId).thenReturn(OWNER_ID);

                when(householdRS.findActiveForUser(OWNER_ID)).thenReturn(Optional.empty());

                assertThatThrownBy(() -> householdService.leave())
                        .isInstanceOf(BaseException.class)
                        .extracting(e -> ((BaseException) e).getStatus())
                        .isEqualTo(HttpStatus.NOT_FOUND);
            }
        }
    }

    @Nested
    @DisplayName("getMemberIds")
    class GetMemberIds {

        @Test
        @DisplayName("accepted household → returns both member ids")
        void accepted_returnsBothIds() {
            when(householdRS.findAcceptedForUser(OWNER_ID)).thenReturn(Optional.of(buildHousehold(HouseholdStatus.ACCEPTED)));

            final List<String> ids = householdService.getMemberIds(OWNER_ID);

            assertThat(ids).containsExactlyInAnyOrder(OWNER_ID, PARTNER_ID);
        }

        @Test
        @DisplayName("no accepted household → returns only the requested user id")
        void none_returnsSelfOnly() {
            when(householdRS.findAcceptedForUser(OWNER_ID)).thenReturn(Optional.empty());

            final List<String> ids = householdService.getMemberIds(OWNER_ID);

            assertThat(ids).containsExactly(OWNER_ID);
        }
    }
}
