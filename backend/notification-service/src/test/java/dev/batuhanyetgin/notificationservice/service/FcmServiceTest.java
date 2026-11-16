package dev.batuhanyetgin.notificationservice.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FcmServiceTest {

    @Mock private FirebaseApp firebaseApp;

    // ─── send ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("send")
    class Send {

        @Test
        @DisplayName("null FirebaseApp → graceful skip, no exception")
        void nullFirebaseApp_skipsGracefully() {
            final FcmService service = new FcmService(null);
            assertThatNoException()
                    .isThrownBy(() -> service.send("token", "title", "body", Map.of()));
        }

        @Test
        @DisplayName("blank FCM token → graceful skip, no exception")
        void blankToken_skipsGracefully() {
            final FcmService service = new FcmService(firebaseApp);
            assertThatNoException()
                    .isThrownBy(() -> service.send("   ", "title", "body", Map.of()));
        }

        @Test
        @DisplayName("empty FCM token → graceful skip, no exception")
        void emptyToken_skipsGracefully() {
            final FcmService service = new FcmService(firebaseApp);
            assertThatNoException()
                    .isThrownBy(() -> service.send("", "title", "body", Map.of()));
        }
    }

    // ─── sendMulticast ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("sendMulticast")
    class SendMulticast {

        @Test
        @DisplayName("null FirebaseApp → graceful skip, no exception")
        void nullFirebaseApp_skipsGracefully() {
            final FcmService service = new FcmService(null);
            assertThatNoException()
                    .isThrownBy(() -> service.sendMulticast(List.of("token1"), "title", "body", Map.of()));
        }

        @Test
        @DisplayName("empty token list → graceful skip, no exception")
        void emptyTokenList_skipsGracefully() {
            final FcmService service = new FcmService(firebaseApp);
            assertThatNoException()
                    .isThrownBy(() -> service.sendMulticast(List.of(), "title", "body", Map.of()));
        }

        @Test
        @DisplayName("list with only blank tokens → filtered out, graceful skip")
        void onlyBlankTokens_skipsGracefully() {
            final FcmService service = new FcmService(firebaseApp);
            assertThatNoException()
                    .isThrownBy(() -> service.sendMulticast(List.of("", "  "), "title", "body", Map.of()));
        }
    }

    // ─── Firebase calls ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("with an initialised FirebaseApp")
    class WithFirebase {

        @Mock private FirebaseMessaging messaging;
        @Mock private BatchResponse batchResponse;

        @Test
        @DisplayName("send → delegates to FirebaseMessaging")
        void send_delegates() throws Exception {
            try (MockedStatic<FirebaseMessaging> fm = mockStatic(FirebaseMessaging.class)) {
                fm.when(FirebaseMessaging::getInstance).thenReturn(messaging);
                when(messaging.send(any(Message.class))).thenReturn("msg-id");

                new FcmService(firebaseApp).send("token", "title", "body", Map.of("k", "v"));

                verify(messaging).send(any(Message.class));
            }
        }

        @Test
        @DisplayName("send → FirebaseMessagingException is logged, not propagated")
        void send_failureSwallowed() throws Exception {
            try (MockedStatic<FirebaseMessaging> fm = mockStatic(FirebaseMessaging.class)) {
                fm.when(FirebaseMessaging::getInstance).thenReturn(messaging);
                when(messaging.send(any(Message.class))).thenThrow(mock(FirebaseMessagingException.class));

                assertThatNoException().isThrownBy(
                        () -> new FcmService(firebaseApp).send("token", "title", "body", Map.of()));
            }
        }

        @Test
        @DisplayName("sendMulticast → delegates with only the non-blank tokens")
        void multicast_delegates() throws Exception {
            try (MockedStatic<FirebaseMessaging> fm = mockStatic(FirebaseMessaging.class)) {
                fm.when(FirebaseMessaging::getInstance).thenReturn(messaging);
                when(messaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(batchResponse);

                new FcmService(firebaseApp).sendMulticast(List.of("t1", " "), "title", "body", Map.of());

                verify(messaging).sendEachForMulticast(any(MulticastMessage.class));
            }
        }

        @Test
        @DisplayName("sendMulticast → FirebaseMessagingException is logged, not propagated")
        void multicast_failureSwallowed() throws Exception {
            try (MockedStatic<FirebaseMessaging> fm = mockStatic(FirebaseMessaging.class)) {
                fm.when(FirebaseMessaging::getInstance).thenReturn(messaging);
                when(messaging.sendEachForMulticast(any(MulticastMessage.class)))
                        .thenThrow(mock(FirebaseMessagingException.class));

                assertThatNoException().isThrownBy(
                        () -> new FcmService(firebaseApp).sendMulticast(List.of("t1"), "title", "body", Map.of()));
            }
        }
    }
}
