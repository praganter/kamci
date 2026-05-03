package dev.batuhanyetgin.kamci.controller;

import dev.batuhanyetgin.kamci.dto.request.*;
import dev.batuhanyetgin.kamci.dto.response.*;
import dev.batuhanyetgin.kamci.enums.CurrencyType;
import dev.batuhanyetgin.kamci.enums.PaymentPeriodType;
import dev.batuhanyetgin.kamci.service.DebtService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DebtControllerTest {

    @Mock private DebtService debtService;
    @Mock private MessageSource messageSource;

    @InjectMocks
    private DebtController controller;

    private static final String DEBT_ID = "debt123";
    private static final DebtItemResponse ITEM = DebtItemResponse.builder().build();
    private static final DebtResponse DEBT_RESPONSE = DebtResponse.builder().debt(ITEM).build();

    @Nested
    class Save {

        @Test
        void delegatesToServiceAndReturns200() {
            final CreateDebtRequest req = CreateDebtRequest.builder()
                    .name("Test").categoryId("cat1").amount(BigDecimal.TEN)
                    .currency(CurrencyType.TRY).paymentPeriodType(PaymentPeriodType.MONTHLY)
                    .firstPaymentDate(LocalDateTime.now()).build();
            when(debtService.create(req)).thenReturn(DEBT_RESPONSE);

            final ResponseEntity<DebtResponse> response = controller.save(req);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isEqualTo(DEBT_RESPONSE);
            verify(debtService).create(req);
        }
    }

    @Nested
    class Update {

        @Test
        void delegatesToServiceAndReturns200() {
            final UpdateDebtRequest req = UpdateDebtRequest.builder()
                    .id(DEBT_ID).name("Updated").categoryId("cat1")
                    .amount(BigDecimal.TEN).currency(CurrencyType.TRY)
                    .paymentPeriodType(PaymentPeriodType.MONTHLY)
                    .firstPaymentDate(LocalDateTime.now()).build();
            when(debtService.update(req)).thenReturn(DEBT_RESPONSE);

            final ResponseEntity<DebtResponse> response = controller.update(req);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isEqualTo(DEBT_RESPONSE);
        }
    }

    @Nested
    class Delete {

        @Test
        void deletesAndReturnsMessage() {
            when(messageSource.getMessage(eq("debt.delete.success"), isNull(), anyString(), eq(Locale.ENGLISH)))
                    .thenReturn("Delete successful");

            final ResponseEntity<DeleteDebtResponse> response = controller.delete(DEBT_ID);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody().getMessage()).isEqualTo("Delete successful");
            verify(debtService).delete(DEBT_ID);
        }
    }

    @Nested
    class Pay {

        @Test
        void delegatesToServiceAndReturns200() {
            final PayDebtRequest req = PayDebtRequest.builder().build();
            when(debtService.pay(DEBT_ID, req)).thenReturn(DEBT_RESPONSE);

            final ResponseEntity<DebtResponse> response = controller.pay(DEBT_ID, req);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isEqualTo(DEBT_RESPONSE);
        }

        @Test
        void nullRequest_delegatesNullAndReturns200() {
            when(debtService.pay(DEBT_ID, null)).thenReturn(DEBT_RESPONSE);

            final ResponseEntity<DebtResponse> response = controller.pay(DEBT_ID, null);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verify(debtService).pay(DEBT_ID, null);
        }
    }

    @Nested
    class Skip {

        @Test
        void delegatesToServiceAndReturns200() {
            final SkipDebtRequest req = new SkipDebtRequest();
            when(debtService.skip(DEBT_ID, req)).thenReturn(DEBT_RESPONSE);

            final ResponseEntity<DebtResponse> response = controller.skip(DEBT_ID, req);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isEqualTo(DEBT_RESPONSE);
        }
    }

    @Nested
    class BulkPay {

        @Test
        void delegatesToServiceAndReturns200() {
            final List<PayDebtRequest> requests = List.of(PayDebtRequest.builder().build());
            when(debtService.bulkPay(DEBT_ID, requests)).thenReturn(DEBT_RESPONSE);

            final ResponseEntity<DebtResponse> response = controller.bulkPay(DEBT_ID, requests);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isEqualTo(DEBT_RESPONSE);
        }
    }

    @Nested
    class SetPaidInstallments {

        @Test
        void delegatesToServiceAndReturns200() {
            final SetPaidInstallmentsRequest req = SetPaidInstallmentsRequest.builder().count(3).build();
            when(debtService.setPaidInstallments(DEBT_ID, req)).thenReturn(DEBT_RESPONSE);

            final ResponseEntity<DebtResponse> response = controller.setPaidInstallments(DEBT_ID, req);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isEqualTo(DEBT_RESPONSE);
        }
    }

    @Nested
    class GetById {

        @Test
        void delegatesToServiceAndReturns200() {
            final DebtDetailResponse detail = DebtDetailResponse.builder().build();
            when(debtService.getById(DEBT_ID)).thenReturn(detail);

            final ResponseEntity<DebtDetailResponse> response = controller.getById(DEBT_ID);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isEqualTo(detail);
        }
    }

    @Nested
    class Get {

        @Test
        void delegatesToServiceAndReturns200() {
            final GetDebtResponse grouped = GetDebtResponse.builder().build();
            when(debtService.get("2024-03", true)).thenReturn(grouped);

            final ResponseEntity<GetDebtResponse> response = controller.get("2024-03", true);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).isEqualTo(grouped);
        }
    }

    @Nested
    class GetPaymentHistory {

        @Test
        void delegatesToServiceAndReturns200() {
            final LocalDateTime from = LocalDateTime.of(2024, 1, 1, 0, 0);
            final LocalDateTime to = LocalDateTime.of(2024, 3, 31, 23, 59);
            final List<PaymentHistoryResponse> history = List.of(PaymentHistoryResponse.builder().build());
            when(debtService.getPaymentHistory(DEBT_ID, from, to)).thenReturn(history);

            final ResponseEntity<List<PaymentHistoryResponse>> response = controller.getPaymentHistory(DEBT_ID, from, to);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).hasSize(1);
        }
    }

    @Nested
    class GetMonthlySummary {

        @Test
        void delegatesToServiceAndReturns200() {
            final MonthlySummaryResponse summary = MonthlySummaryResponse.builder().month("2024-03").build();
            when(debtService.getMonthlySummary("2024-03")).thenReturn(summary);

            final ResponseEntity<MonthlySummaryResponse> response = controller.getMonthlySummary("2024-03");

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody().getMonth()).isEqualTo("2024-03");
        }
    }
}
