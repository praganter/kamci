package dev.batuhanyetgin.authservice.repositoryservice;

import dev.batuhanyetgin.authservice.document.AuthDocument;
import dev.batuhanyetgin.authservice.repository.AuthRepository;
import dev.batuhanyetgin.commonmodule.exception.BaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthRepositoryServiceTest {

    @Mock
    private AuthRepository authRepository;

    private AuthRepositoryService authRepositoryService;

    private static final String EMAIL = "test@example.com";

    private AuthDocument buildAuthDoc() {
        return AuthDocument.builder()
                .email(EMAIL)
                .password("$2a$hash")
                .build();
    }

    @BeforeEach
    void setUp() {
        authRepositoryService = new AuthRepositoryService(authRepository);
    }

    // ─── findByEMail ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findByEMail")
    class FindByEMail {

        @Test
        @DisplayName("existing email → returns document")
        void existingEmail_returnsDocument() {
            final AuthDocument doc = buildAuthDoc();
            when(authRepository.findByEmail(EMAIL)).thenReturn(Optional.of(doc));

            assertThat(authRepositoryService.findByEMail(EMAIL)).isEqualTo(doc);
        }

        @Test
        @DisplayName("missing email → throws NOT_FOUND")
        void missingEmail_throwsNotFound() {
            when(authRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatExceptionOfType(BaseException.class)
                    .isThrownBy(() -> authRepositoryService.findByEMail(EMAIL))
                    .matches(e -> e.getStatus() == HttpStatus.NOT_FOUND);
        }
    }

    // ─── findByEmailOptional ──────────────────────────────────────────────────

    @Nested
    @DisplayName("findByEmailOptional")
    class FindByEmailOptional {

        @Test
        @DisplayName("existing email → returns Optional with document")
        void existingEmail_returnsPresent() {
            final AuthDocument doc = buildAuthDoc();
            when(authRepository.findByEmail(EMAIL)).thenReturn(Optional.of(doc));

            assertThat(authRepositoryService.findByEmailOptional(EMAIL)).contains(doc);
        }

        @Test
        @DisplayName("missing email → returns empty Optional")
        void missingEmail_returnsEmpty() {
            when(authRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThat(authRepositoryService.findByEmailOptional(EMAIL)).isEmpty();
        }
    }

    // ─── existsByEmail ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("existsByEmail")
    class ExistsByEmail {

        @Test
        @DisplayName("existing email → returns true")
        void existingEmail_returnsTrue() {
            when(authRepository.existsByEmail(EMAIL)).thenReturn(true);
            assertThat(authRepositoryService.existsByEmail(EMAIL)).isTrue();
        }

        @Test
        @DisplayName("missing email → returns false")
        void missingEmail_returnsFalse() {
            when(authRepository.existsByEmail(EMAIL)).thenReturn(false);
            assertThat(authRepositoryService.existsByEmail(EMAIL)).isFalse();
        }
    }

    // ─── findById (from BaseRepositoryService) ────────────────────────────────

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("existing id → returns document")
        void existingId_returnsDocument() {
            final AuthDocument doc = buildAuthDoc();
            when(authRepository.findById("id123")).thenReturn(Optional.of(doc));

            assertThat(authRepositoryService.findById("id123")).isEqualTo(doc);
        }

        @Test
        @DisplayName("missing id → throws IllegalArgumentException")
        void missingId_throwsIllegalArgument() {
            when(authRepository.findById("id123")).thenReturn(Optional.empty());

            assertThatIllegalArgumentException()
                    .isThrownBy(() -> authRepositoryService.findById("id123"));
        }
    }

    // ─── existsById ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("existsById")
    class ExistsById {

        @Test
        @DisplayName("existing id → returns true")
        void existingId_returnsTrue() {
            when(authRepository.existsById("id123")).thenReturn(true);

            assertThat(authRepositoryService.existsById("id123")).isTrue();
        }

        @Test
        @DisplayName("missing id → returns false")
        void missingId_returnsFalse() {
            when(authRepository.existsById("id123")).thenReturn(false);

            assertThat(authRepositoryService.existsById("id123")).isFalse();
        }
    }
}
