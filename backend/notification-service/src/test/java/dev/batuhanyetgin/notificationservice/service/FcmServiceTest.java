package dev.batuhanyetgin.notificationservice.service;

import com.google.firebase.FirebaseApp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

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
}
