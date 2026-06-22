package dev.batuhanyetgin.kamci.repositoryservice;

import dev.batuhanyetgin.commonmodule.exception.BaseException;
import dev.batuhanyetgin.kamci.document.UserIncomeDocument;
import dev.batuhanyetgin.kamci.repository.UserIncomeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserIncomeRepositoryServiceTest {

    @Mock private UserIncomeRepository userIncomeRepository;

    private UserIncomeRepositoryService service;

    private static final String USER_ID   = "user123";
    private static final String INCOME_ID = "income456";

    @BeforeEach
    void setUp() {
        service = new UserIncomeRepositoryService(userIncomeRepository);
    }

    private UserIncomeDocument buildDoc() {
        return UserIncomeDocument.builder()
                .ownerId(USER_ID)
                .amount(BigDecimal.valueOf(3000))
                .effectiveFrom(LocalDateTime.of(2025, 1, 1, 0, 0))
                .build();
    }

    // ─── save ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("delegates to repository and returns saved document")
        void delegatesAndReturns() {
            final UserIncomeDocument doc = buildDoc();
            when(userIncomeRepository.save(doc)).thenReturn(doc);

            assertThat(service.save(doc)).isEqualTo(doc);
            verify(userIncomeRepository).save(doc);
        }
    }

    // ─── findById ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("existing id → returns document")
        void existingId_returnsDocument() {
            final UserIncomeDocument doc = buildDoc();
            when(userIncomeRepository.findById(INCOME_ID)).thenReturn(Optional.of(doc));

            assertThat(service.findById(INCOME_ID)).isEqualTo(doc);
        }

        @Test
        @DisplayName("missing id → throws NOT_FOUND")
        void missingId_throwsNotFound() {
            when(userIncomeRepository.findById(INCOME_ID)).thenReturn(Optional.empty());

            assertThatExceptionOfType(BaseException.class)
                    .isThrownBy(() -> service.findById(INCOME_ID))
                    .matches(e -> e.getStatus() == HttpStatus.NOT_FOUND);
        }
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("delegates to repository deleteById")
        void delegatesToRepository() {
            service.delete(INCOME_ID);
            verify(userIncomeRepository).deleteById(INCOME_ID);
        }
    }

    // ─── findByOwnerIdOrderByEffectiveFromDesc ─────────────────────────────────

    @Nested
    @DisplayName("findByOwnerIdOrderByEffectiveFromDesc")
    class FindByOwnerIdOrderByEffectiveFromDesc {

        @Test
        @DisplayName("returns list from repository")
        void returnsList() {
            final List<UserIncomeDocument> docs = List.of(buildDoc());
            when(userIncomeRepository.findByOwnerIdOrderByEffectiveFromDesc(USER_ID)).thenReturn(docs);

            assertThat(service.findByOwnerIdOrderByEffectiveFromDesc(USER_ID)).isEqualTo(docs);
        }

        @Test
        @DisplayName("no records → returns empty list")
        void noRecords_returnsEmpty() {
            when(userIncomeRepository.findByOwnerIdOrderByEffectiveFromDesc(USER_ID)).thenReturn(List.of());

            assertThat(service.findByOwnerIdOrderByEffectiveFromDesc(USER_ID)).isEmpty();
        }
    }

    // ─── findCurrentByOwnerId ─────────────────────────────────────────────────

    @Nested
    @DisplayName("findCurrentByOwnerId")
    class FindCurrentByOwnerId {

        @Test
        @DisplayName("record exists → returns present Optional")
        void recordExists_returnsPresent() {
            when(userIncomeRepository.findTopByOwnerIdOrderByEffectiveFromDesc(USER_ID))
                    .thenReturn(Optional.of(buildDoc()));

            assertThat(service.findCurrentByOwnerId(USER_ID)).isPresent();
        }

        @Test
        @DisplayName("no record → returns empty Optional")
        void noRecord_returnsEmpty() {
            when(userIncomeRepository.findTopByOwnerIdOrderByEffectiveFromDesc(USER_ID))
                    .thenReturn(Optional.empty());

            assertThat(service.findCurrentByOwnerId(USER_ID)).isEmpty();
        }
    }

    // ─── findByOwnerIdAsOf ────────────────────────────────────────────────────

    @Nested
    @DisplayName("findByOwnerIdAsOf")
    class FindByOwnerIdAsOf {

        @Test
        @DisplayName("record exists as of date → returns present Optional")
        void recordExists_returnsPresent() {
            final LocalDateTime asOf = LocalDateTime.of(2025, 6, 1, 0, 0);
            when(userIncomeRepository.findTopByOwnerIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(USER_ID, asOf))
                    .thenReturn(Optional.of(buildDoc()));

            assertThat(service.findByOwnerIdAsOf(USER_ID, asOf)).isPresent();
        }

        @Test
        @DisplayName("no record as of date → returns empty Optional")
        void noRecord_returnsEmpty() {
            final LocalDateTime asOf = LocalDateTime.of(2024, 1, 1, 0, 0);
            when(userIncomeRepository.findTopByOwnerIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(USER_ID, asOf))
                    .thenReturn(Optional.empty());

            assertThat(service.findByOwnerIdAsOf(USER_ID, asOf)).isEmpty();
        }
    }
}
