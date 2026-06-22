package dev.batuhanyetgin.kamci.controller;

import dev.batuhanyetgin.kamci.dto.request.CreateIncomeRequest;
import dev.batuhanyetgin.kamci.dto.response.IncomeResponse;
import dev.batuhanyetgin.kamci.enums.CurrencyType;
import dev.batuhanyetgin.kamci.service.IncomeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncomeControllerTest {

    @Mock private IncomeService incomeService;

    @InjectMocks
    private IncomeController controller;

    private IncomeResponse buildResponse() {
        return IncomeResponse.builder()
                .id("income1")
                .amount(BigDecimal.valueOf(5000))
                .currency(CurrencyType.TRY)
                .effectiveFrom(LocalDateTime.of(2025, 1, 1, 0, 0))
                .build();
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("delegates to service and returns 200")
        void delegatesAndReturns200() {
            final CreateIncomeRequest req = CreateIncomeRequest.builder()
                    .amount(BigDecimal.valueOf(5000))
                    .currency(CurrencyType.TRY)
                    .effectiveFrom(LocalDateTime.of(2025, 1, 1, 0, 0))
                    .build();

            final IncomeResponse resp = buildResponse();
            when(incomeService.create(req)).thenReturn(resp);

            final ResponseEntity<IncomeResponse> response = controller.save(req);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isEqualTo(resp);
            verify(incomeService).create(req);
        }
    }

    @Nested
    @DisplayName("history")
    class History {

        @Test
        @DisplayName("delegates to service and returns list with 200")
        void delegatesAndReturnsList() {
            when(incomeService.getHistory()).thenReturn(List.of(buildResponse(), buildResponse()));

            final ResponseEntity<List<IncomeResponse>> response = controller.history();

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).hasSize(2);
            verify(incomeService).getHistory();
        }

        @Test
        @DisplayName("empty history → returns empty list with 200")
        void emptyHistory_returnsEmptyList() {
            when(incomeService.getHistory()).thenReturn(List.of());

            final ResponseEntity<List<IncomeResponse>> response = controller.history();

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("current")
    class Current {

        @Test
        @DisplayName("delegates to service and returns 200")
        void delegatesAndReturns200() {
            final IncomeResponse resp = buildResponse();
            when(incomeService.getCurrent()).thenReturn(resp);

            final ResponseEntity<IncomeResponse> response = controller.current();

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isEqualTo(resp);
            verify(incomeService).getCurrent();
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("delegates to service and returns 200")
        void delegatesAndReturns200() {
            final ResponseEntity<Void> response = controller.delete("income1");

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verify(incomeService).delete("income1");
        }
    }
}
