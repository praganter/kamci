package dev.batuhanyetgin.authservice.scheduler;

import dev.batuhanyetgin.authservice.document.AuthDocument;
import dev.batuhanyetgin.authservice.document.SubscriptionDocument;
import dev.batuhanyetgin.authservice.enums.SubscriptionStatus;
import dev.batuhanyetgin.authservice.repositoryservice.AuthRepositoryService;
import dev.batuhanyetgin.authservice.repositoryservice.SubscriptionRepositoryService;
import dev.batuhanyetgin.commonmodule.exception.BaseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionExpirySchedulerTest {

    @Mock private SubscriptionRepositoryService subscriptionRS;
    @Mock private AuthRepositoryService authRS;

    @InjectMocks
    private SubscriptionExpiryScheduler scheduler;

    private static final String USER_ID = "user123";

    private SubscriptionDocument buildExpiredSub() {
        final SubscriptionDocument sub = SubscriptionDocument.builder()
                .userId(USER_ID)
                .status(SubscriptionStatus.ACTIVE)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(sub, "id", "sub1");
        return sub;
    }

    private AuthDocument buildAuthDoc() {
        final AuthDocument doc = AuthDocument.builder().email("user@example.com").build();
        doc.setPremium(true);
        org.springframework.test.util.ReflectionTestUtils.setField(doc, "id", USER_ID);
        return doc;
    }

    // ─── expireSubscriptions ──────────────────────────────────────────────────

    @Nested
    @DisplayName("expireSubscriptions")
    class ExpireSubscriptions {

        @Test
        @DisplayName("no expired subscriptions → no saves")
        void noneExpired_noSaves() {
            when(subscriptionRS.findActiveExpired(any(LocalDateTime.class))).thenReturn(List.of());

            assertThatNoException().isThrownBy(() -> scheduler.expireSubscriptions());

            verify(subscriptionRS, never()).save(any());
            verify(authRS, never()).save(any());
        }

        @Test
        @DisplayName("two expired subscriptions → both expired and auth updated")
        void twoExpired_bothProcessed() {
            final SubscriptionDocument sub1 = buildExpiredSub();
            final SubscriptionDocument sub2 = buildExpiredSub();
            org.springframework.test.util.ReflectionTestUtils.setField(sub2, "id", "sub2");

            final String userId2 = "user456";
            sub2.setUserId(userId2);

            final AuthDocument auth1 = buildAuthDoc();
            final AuthDocument auth2 = AuthDocument.builder().email("other@example.com").build();
            auth2.setPremium(true);

            when(subscriptionRS.findActiveExpired(any(LocalDateTime.class))).thenReturn(List.of(sub1, sub2));
            when(authRS.findById(USER_ID)).thenReturn(auth1);
            when(authRS.findById(userId2)).thenReturn(auth2);
            when(subscriptionRS.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(authRS.save(any())).thenAnswer(inv -> inv.getArgument(0));

            scheduler.expireSubscriptions();

            assertThat(sub1.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
            assertThat(sub2.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
            assertThat(auth1.isPremium()).isFalse();
            assertThat(auth2.isPremium()).isFalse();
            verify(subscriptionRS, times(2)).save(any());
            verify(authRS, times(2)).save(any());
        }

        @Test
        @DisplayName("one subscription fails → continues processing others")
        void oneFailsOtherContinues() {
            final SubscriptionDocument sub1 = buildExpiredSub();
            final SubscriptionDocument sub2 = buildExpiredSub();
            sub2.setUserId("user456");

            final AuthDocument auth2 = AuthDocument.builder().email("other@example.com").build();
            auth2.setPremium(true);

            when(subscriptionRS.findActiveExpired(any(LocalDateTime.class))).thenReturn(List.of(sub1, sub2));
            when(authRS.findById(USER_ID))
                    .thenThrow(BaseException.notFound("error.user.not_found"));
            when(authRS.findById("user456")).thenReturn(auth2);
            when(subscriptionRS.save(sub2)).thenReturn(sub2);
            when(authRS.save(auth2)).thenReturn(auth2);

            assertThatNoException().isThrownBy(() -> scheduler.expireSubscriptions());

            assertThat(sub2.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
            assertThat(auth2.isPremium()).isFalse();
        }

        @Test
        @DisplayName("single expired subscription → status set to EXPIRED, premium removed from auth")
        void singleExpired_statusExpiredAndPremiumRemoved() {
            final SubscriptionDocument sub = buildExpiredSub();
            final AuthDocument auth = buildAuthDoc();

            when(subscriptionRS.findActiveExpired(any(LocalDateTime.class))).thenReturn(List.of(sub));
            when(authRS.findById(USER_ID)).thenReturn(auth);
            when(subscriptionRS.save(sub)).thenReturn(sub);
            when(authRS.save(auth)).thenReturn(auth);

            scheduler.expireSubscriptions();

            assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
            assertThat(auth.isPremium()).isFalse();
        }
    }
}
