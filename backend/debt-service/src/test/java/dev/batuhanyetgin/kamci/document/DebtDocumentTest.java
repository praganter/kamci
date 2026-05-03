package dev.batuhanyetgin.kamci.document;

import dev.batuhanyetgin.kamci.document.embedded.PaymentEmbedded;
import dev.batuhanyetgin.kamci.enums.CurrencyType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DebtDocumentTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2024, 3, 1, 12, 0);
    private static final LocalDateTime PERIOD_START = LocalDateTime.of(2024, 3, 1, 0, 0);
    private static final LocalDateTime PERIOD_END = LocalDateTime.of(2024, 3, 31, 23, 59);

    @Nested
    class AddPayment {

        @Test
        void addsPaymentEntry() {
            final DebtDocument doc = DebtDocument.builder().build();

            doc.addPayment(NOW, PERIOD_START, PERIOD_END, BigDecimal.valueOf(100));

            assertThat(doc.getPayments()).hasSize(1);
            final PaymentEmbedded p = doc.getPayments().get(0);
            assertThat(p.isPaid()).isTrue();
            assertThat(p.getPaidTime()).isEqualTo(NOW);
            assertThat(p.getPeriodStart()).isEqualTo(PERIOD_START);
            assertThat(p.getPeriodEnd()).isEqualTo(PERIOD_END);
            assertThat(p.getAmountPaid()).isEqualByComparingTo(BigDecimal.valueOf(100));
        }

        @Test
        void multiplePayments_accumulate() {
            final DebtDocument doc = DebtDocument.builder().build();

            doc.addPayment(NOW, PERIOD_START, PERIOD_END, BigDecimal.valueOf(100));
            doc.addPayment(NOW.plusMonths(1), PERIOD_END, PERIOD_END.plusMonths(1), BigDecimal.valueOf(100));

            assertThat(doc.getPayments()).hasSize(2);
        }
    }

    @Nested
    class SkipPayment {

        @Test
        void addsUnpaidEntry() {
            final DebtDocument doc = DebtDocument.builder().build();

            doc.skipPayment(NOW, PERIOD_START, PERIOD_END);

            assertThat(doc.getPayments()).hasSize(1);
            final PaymentEmbedded p = doc.getPayments().get(0);
            assertThat(p.isPaid()).isFalse();
            assertThat(p.getPaidTime()).isEqualTo(NOW);
            assertThat(p.getPeriodStart()).isEqualTo(PERIOD_START);
            assertThat(p.getPeriodEnd()).isEqualTo(PERIOD_END);
            assertThat(p.getAmountPaid()).isNull();
        }
    }

    @Nested
    class UpdateAmount {

        @Test
        void sameAmount_noHistoryCreated() {
            final DebtDocument doc = DebtDocument.builder()
                    .amount(BigDecimal.valueOf(100))
                    .currency(CurrencyType.TRY)
                    .build();

            doc.updateAmount(BigDecimal.valueOf(100), NOW);

            assertThat(doc.getAmountHistory()).isEmpty();
            assertThat(doc.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
        }

        @Test
        void differentAmount_createsHistoryEntryAndUpdatesAmount() {
            final DebtDocument doc = DebtDocument.builder()
                    .amount(BigDecimal.valueOf(100))
                    .currency(CurrencyType.TRY)
                    .build();

            doc.updateAmount(BigDecimal.valueOf(200), NOW);

            assertThat(doc.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(200));
            assertThat(doc.getAmountHistory()).hasSize(1);
            assertThat(doc.getAmountHistory().get(0).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
            assertThat(doc.getAmountHistory().get(0).getCurrency()).isEqualTo(CurrencyType.TRY);
            assertThat(doc.getAmountHistory().get(0).getEffectiveFrom()).isEqualTo(NOW);
            assertThat(doc.getAmountHistory().get(0).getEffectiveTo()).isNull();
        }

        @Test
        void secondUpdate_closesOpenHistoryEntry() {
            final LocalDateTime t1 = NOW;
            final LocalDateTime t2 = NOW.plusMonths(1);

            final DebtDocument doc = DebtDocument.builder()
                    .amount(BigDecimal.valueOf(100))
                    .currency(CurrencyType.TRY)
                    .build();

            doc.updateAmount(BigDecimal.valueOf(200), t1);
            doc.updateAmount(BigDecimal.valueOf(300), t2);

            assertThat(doc.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(300));
            assertThat(doc.getAmountHistory()).hasSize(2);
            // First entry closed at t2 (effectiveFrom of second update)
            assertThat(doc.getAmountHistory().get(0).getEffectiveTo()).isEqualTo(t2);
            assertThat(doc.getAmountHistory().get(1).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(200));
            assertThat(doc.getAmountHistory().get(1).getEffectiveTo()).isNull();
        }
    }
}
