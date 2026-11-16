package dev.batuhanyetgin.kamci.service;

import dev.batuhanyetgin.commonmodule.exception.BaseException;
import dev.batuhanyetgin.commonmodule.util.RequestContextUtil;
import dev.batuhanyetgin.kamci.client.AuthClient;
import dev.batuhanyetgin.kamci.client.ExchangeRateClient;
import dev.batuhanyetgin.kamci.document.CategoryDocument;
import dev.batuhanyetgin.kamci.document.DebtDocument;
import dev.batuhanyetgin.kamci.document.embedded.ReminderEmbedded;
import dev.batuhanyetgin.kamci.dto.request.CreateDebtRequest;
import dev.batuhanyetgin.kamci.enums.DebtStatus;
import dev.batuhanyetgin.kamci.enums.PaymentPeriodType;
import dev.batuhanyetgin.kamci.enums.PaymentStatus;
import dev.batuhanyetgin.kamci.mapper.DebtMapper;
import dev.batuhanyetgin.kamci.repositoryservice.CategoryRepositoryService;
import dev.batuhanyetgin.kamci.repositoryservice.DebtRepositoryService;
import dev.batuhanyetgin.kamci.repositoryservice.UserIncomeRepositoryService;
import dev.batuhanyetgin.kamci.util.PaymentPeriodUtil;
import net.javacrumbs.shedlock.core.DefaultLockingTaskExecutor;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.core.SimpleLock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Business rules applied while a debt is created: reminder preconditions and the
 * backfill of auto-payments for a first payment date in the past.
 */
@ExtendWith(MockitoExtension.class)
class DebtServiceCreateRulesTest {

    private static final String USER = "owner1";
    private static final String CAT = "cat1";

    @Mock private DebtRepositoryService debtRS;
    @Mock private CategoryRepositoryService categoryRS;
    @Mock private DebtMapper debtMapper;
    @Mock private DebtEventAsyncPublisher publisher;
    @Mock private UserIncomeRepositoryService incomeRS;
    @Mock private ExchangeRateClient exchangeRateClient;
    @Mock private AuthClient authClient;
    @Mock private LockProvider lockProvider;
    @Mock private SimpleLock simpleLock;

    private DebtService service;
    private MockedStatic<RequestContextUtil> ctx;
    private boolean premium = true;

    @BeforeEach
    void setUp() throws Throwable {
        lenient().when(lockProvider.lock(any())).thenReturn(Optional.of(simpleLock));
        service = new DebtService(debtRS, categoryRS, debtMapper, publisher, incomeRS, exchangeRateClient,
                authClient, new DefaultLockingTaskExecutor(lockProvider));

        ctx = mockStatic(RequestContextUtil.class);
        ctx.when(RequestContextUtil::getUserId).thenReturn(USER);
        ctx.when(RequestContextUtil::getLocale).thenReturn("en");
        ctx.when(RequestContextUtil::isPremium).thenAnswer(inv -> premium);

        final CategoryDocument category = CategoryDocument.builder().id(CAT).ownerId(USER).name("c").build();
        lenient().when(categoryRS.findById(CAT)).thenReturn(category);
        lenient().when(categoryRS.findByOwnerId(USER)).thenReturn(List.of(category));
        lenient().when(debtRS.countByOwnerIdAndCategoryIdIn(eq(USER), anyList())).thenReturn(0L);
        lenient().when(debtRS.save(any(DebtDocument.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        ctx.close();
    }

    private DebtDocument create(final DebtDocument converted) {
        when(debtMapper.convert(any(CreateDebtRequest.class))).thenReturn(converted);
        service.create(CreateDebtRequest.builder().categoryId(CAT).name("d").build());
        return converted;
    }

    private DebtDocument.DebtDocumentBuilder debt() {
        return DebtDocument.builder().id("d1").name("d").ownerId(USER).categoryId(CAT)
                .amount(BigDecimal.TEN).paymentPeriodType(PaymentPeriodType.MONTHLY);
    }

    private static ReminderEmbedded reminder(final LocalDate date, final LocalTime time) {
        return ReminderEmbedded.builder().enabled(true).reminderDate(date).reminderTime(time).build();
    }

    // ─── reminder ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("enabled reminder without premium → 403")
    void reminderRequiresPremium() {
        premium = false;
        final DebtDocument d = debt().firstPaymentDate(LocalDateTime.now().plusDays(1))
                .reminder(reminder(LocalDate.now(), LocalTime.NOON)).build();

        assertThatThrownBy(() -> create(d)).isInstanceOf(BaseException.class)
                .hasMessage("error.debt.premium_required");
    }

    @Test
    @DisplayName("enabled reminder without date or time → 400")
    void reminderNeedsDateAndTime() {
        final DebtDocument noDate = debt().firstPaymentDate(LocalDateTime.now().plusDays(1))
                .reminder(reminder(null, LocalTime.NOON)).build();
        assertThatThrownBy(() -> create(noDate)).hasMessage("error.reminder.date_or_time_missing");

        final DebtDocument noTime = debt().firstPaymentDate(LocalDateTime.now().plusDays(1))
                .reminder(reminder(LocalDate.now(), null)).build();
        assertThatThrownBy(() -> create(noTime)).hasMessage("error.reminder.date_or_time_missing");
    }

    @Test
    @DisplayName("valid premium reminder → next reminder date is date + time")
    void reminderSetsNextDate() {
        final LocalDate date = LocalDate.now().plusDays(3);
        final DebtDocument d = create(debt().firstPaymentDate(LocalDateTime.now().plusDays(1))
                .reminder(reminder(date, LocalTime.of(9, 30))).build());

        assertThat(d.getNextReminderDate()).isEqualTo(LocalDateTime.of(date, LocalTime.of(9, 30)));
    }

    @Test
    @DisplayName("disabled reminder is ignored even for a free user")
    void disabledReminderIgnored() {
        premium = false;
        final DebtDocument d = create(debt().firstPaymentDate(LocalDateTime.now().plusDays(1))
                .reminder(ReminderEmbedded.builder().enabled(false).build()).build());

        assertThat(d.getNextReminderDate()).isNull();
    }

    // ─── auto-payment backfill ────────────────────────────────────────────────

    @Test
    @DisplayName("auto-payment with a past first date → one paid installment per elapsed period")
    void backfillGeneratesElapsedPayments() {
        final LocalDateTime first = LocalDateTime.now(PaymentPeriodUtil.APP_ZONE).minusMonths(2).minusDays(1);
        final DebtDocument d = create(debt().autoPayment(true).firstPaymentDate(first).build());

        assertThat(d.getPayments()).hasSize(3).allMatch(p -> p.isPaid());
        assertThat(d.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(d.getNextAutoPaymentDate()).isAfter(LocalDateTime.now(PaymentPeriodUtil.APP_ZONE));
    }

    @Test
    @DisplayName("auto-payment reaching the installment count during backfill → COMPLETED, no next date")
    void backfillCompletesDebt() {
        final LocalDateTime first = LocalDateTime.now(PaymentPeriodUtil.APP_ZONE).minusMonths(5);
        final DebtDocument d = create(debt().autoPayment(true).installmentCount(2).firstPaymentDate(first).build());

        assertThat(d.getStatus()).isEqualTo(DebtStatus.COMPLETED);
        assertThat(d.getPayments()).hasSize(2);
        assertThat(d.getNextAutoPaymentDate()).isNull();
    }

    @Test
    @DisplayName("auto-payment with a future first date → no backfill, next date is the first date")
    void futureFirstDate() {
        final LocalDateTime first = LocalDateTime.now(PaymentPeriodUtil.APP_ZONE).plusDays(10);
        final DebtDocument d = create(debt().autoPayment(true).firstPaymentDate(first).build());

        assertThat(d.getPayments()).isEmpty();
        assertThat(d.getNextAutoPaymentDate()).isEqualTo(first);
    }

    @Test
    @DisplayName("auto-payment without a period type → nothing is generated")
    void noPeriodType() {
        final DebtDocument d = create(debt().autoPayment(true).paymentPeriodType(null)
                .firstPaymentDate(LocalDateTime.now().minusMonths(3)).build());

        assertThat(d.getPayments()).isEmpty();
    }

    @Test
    @DisplayName("existing next auto-payment date is preserved")
    void existingNextDatePreserved() {
        final LocalDateTime next = LocalDateTime.now(PaymentPeriodUtil.APP_ZONE).plusDays(20);
        final DebtDocument d = create(debt().autoPayment(true).nextAutoPaymentDate(next)
                .firstPaymentDate(LocalDateTime.now(PaymentPeriodUtil.APP_ZONE).plusDays(2)).build());

        assertThat(d.getNextAutoPaymentDate()).isEqualTo(next);
    }

    @Test
    @DisplayName("manual debt (no auto-payment) is left untouched")
    void manualDebtUntouched() {
        final DebtDocument d = create(debt().firstPaymentDate(LocalDateTime.now().minusMonths(3)).build());

        assertThat(d.getPayments()).isEmpty();
        assertThat(d.getNextAutoPaymentDate()).isNull();
    }
}
