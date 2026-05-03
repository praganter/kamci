package dev.batuhanyetgin.kamci.mapper;

import dev.batuhanyetgin.kamci.document.DebtDocument;
import dev.batuhanyetgin.kamci.document.embedded.PaymentEmbedded;
import dev.batuhanyetgin.kamci.dto.request.CreateDebtRequest;
import dev.batuhanyetgin.kamci.dto.request.UpdateDebtRequest;
import dev.batuhanyetgin.kamci.dto.response.DebtDetailResponse;
import dev.batuhanyetgin.kamci.dto.response.DebtItemResponse;
import dev.batuhanyetgin.kamci.enums.CurrencyType;
import dev.batuhanyetgin.kamci.enums.PaymentPeriodType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DebtMapperTest {

    private final DebtMapper mapper = new DebtMapper();

    private static final LocalDateTime DATE = LocalDateTime.of(2024, 6, 1, 10, 0);

    @Nested
    class ConvertCreateRequest {

        @Test
        void allFieldsMapped() {
            final CreateDebtRequest req = CreateDebtRequest.builder()
                    .name("Rent")
                    .description("Monthly rent")
                    .firstPaymentDate(DATE)
                    .installmentCount(12)
                    .paymentPeriodType(PaymentPeriodType.MONTHLY)
                    .amount(BigDecimal.valueOf(1500))
                    .currency(CurrencyType.TRY)
                    .categoryId("cat1")
                    .autoPayment(false)
                    .build();

            final DebtDocument doc = mapper.convert(req);

            assertThat(doc.getName()).isEqualTo("Rent");
            assertThat(doc.getDescription()).isEqualTo("Monthly rent");
            assertThat(doc.getFirstPaymentDate()).isEqualTo(DATE);
            assertThat(doc.getInstallmentCount()).isEqualTo(12);
            assertThat(doc.getPaymentPeriodType()).isEqualTo(PaymentPeriodType.MONTHLY);
            assertThat(doc.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1500));
            assertThat(doc.getCurrency()).isEqualTo(CurrencyType.TRY);
            assertThat(doc.getCategoryId()).isEqualTo("cat1");
        }
    }

    @Nested
    class ConvertUpdateRequest {

        @Test
        void allFieldsMapped() {
            final UpdateDebtRequest req = UpdateDebtRequest.builder()
                    .id("debt1")
                    .name("Updated Rent")
                    .description("Updated desc")
                    .firstPaymentDate(DATE)
                    .installmentCount(6)
                    .paymentPeriodType(PaymentPeriodType.YEARLY)
                    .amount(BigDecimal.valueOf(2000))
                    .currency(CurrencyType.USD)
                    .categoryId("cat2")
                    .autoPayment(true)
                    .build();

            final DebtDocument doc = mapper.convert(req);

            assertThat(doc.getName()).isEqualTo("Updated Rent");
            assertThat(doc.getDescription()).isEqualTo("Updated desc");
            assertThat(doc.getFirstPaymentDate()).isEqualTo(DATE);
            assertThat(doc.getInstallmentCount()).isEqualTo(6);
            assertThat(doc.getPaymentPeriodType()).isEqualTo(PaymentPeriodType.YEARLY);
            assertThat(doc.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(2000));
            assertThat(doc.getCurrency()).isEqualTo(CurrencyType.USD);
            assertThat(doc.getCategoryId()).isEqualTo("cat2");
        }
    }

    @Nested
    class ConvertDocumentToItem {

        @Test
        void allFieldsMapped() {
            final DebtDocument doc = DebtDocument.builder()
                    .name("Car Loan")
                    .amount(BigDecimal.valueOf(500))
                    .currency(CurrencyType.EUR)
                    .paymentPeriodType(PaymentPeriodType.MONTHLY)
                    .firstPaymentDate(DATE)
                    .build();

            final DebtItemResponse response = mapper.convert(doc);

            assertThat(response.getName()).isEqualTo("Car Loan");
            assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(500));
            assertThat(response.getCurrency()).isEqualTo(CurrencyType.EUR);
            assertThat(response.getPaymentPeriodType()).isEqualTo(PaymentPeriodType.MONTHLY);
            assertThat(response.getFirstPaymentDate()).isEqualTo(DATE);
        }

        @Test
        void paidCurrentPeriod_isIgnoredAndDefaultsFalse() {
            final DebtDocument doc = DebtDocument.builder().name("Test").build();

            final DebtItemResponse response = mapper.convert(doc);

            assertThat(response.isPaidCurrentPeriod()).isFalse();
        }
    }

    @Nested
    class ConvertToDetail {

        @Test
        void withPayments_paidInstallmentCountMatchesSize() {
            final PaymentEmbedded payment = PaymentEmbedded.builder()
                    .paid(true)
                    .paidTime(DATE)
                    .periodStart(DATE)
                    .periodEnd(DATE.plusMonths(1))
                    .amountPaid(BigDecimal.valueOf(500))
                    .build();

            final DebtDocument doc = DebtDocument.builder()
                    .name("Mortgage")
                    .amount(BigDecimal.valueOf(1000))
                    .currency(CurrencyType.USD)
                    .payments(List.of(payment))
                    .build();

            final DebtDetailResponse response = mapper.convertToDetail(doc);

            assertThat(response.getName()).isEqualTo("Mortgage");
            assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
            assertThat(response.getPaidInstallmentCount()).isEqualTo(1);
            assertThat(response.getPayments()).hasSize(1);
        }

        @Test
        void nullPayments_paidInstallmentCountIsZero() {
            final DebtDocument doc = DebtDocument.builder()
                    .name("Test")
                    .payments(null)
                    .build();

            final DebtDetailResponse response = mapper.convertToDetail(doc);

            assertThat(response.getPaidInstallmentCount()).isZero();
        }

        @Test
        void emptyPayments_paidInstallmentCountIsZero() {
            final DebtDocument doc = DebtDocument.builder()
                    .name("Test")
                    .build();

            final DebtDetailResponse response = mapper.convertToDetail(doc);

            assertThat(response.getPaidInstallmentCount()).isZero();
        }
    }
}
