package dev.batuhanyetgin.kamci.service;

import dev.batuhanyetgin.commonmodule.util.RequestContextUtil;
import dev.batuhanyetgin.kamci.client.AuthClient;
import dev.batuhanyetgin.kamci.client.ExchangeRateClient;
import dev.batuhanyetgin.kamci.document.CategoryDocument;
import dev.batuhanyetgin.kamci.document.DebtDocument;
import dev.batuhanyetgin.kamci.document.embedded.PaymentEmbedded;
import dev.batuhanyetgin.kamci.dto.response.DebtSummaryResponse;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/** Debt list rules: which debts show up for a month and how the payment status of a period is derived. */
@ExtendWith(MockitoExtension.class)
class DebtServiceListRulesTest {

    private static final String USER = "u1";
    private static final String CAT = "cat1";
    private static final String MONTH = "2026-03";

    @Mock private DebtRepositoryService debtRS;
    @Mock private CategoryRepositoryService categoryRS;
    @Mock private DebtMapper debtMapper;
    @Mock private DebtEventAsyncPublisher publisher;
    @Mock private UserIncomeRepositoryService incomeRS;
    @Mock private ExchangeRateClient exchangeRateClient;
    @Mock private AuthClient authClient;
    @Mock private LockProvider lockProvider;

    private DebtService service;
    private MockedStatic<RequestContextUtil> ctx;

    @BeforeEach
    void setUp() {
        service = new DebtService(debtRS, categoryRS, debtMapper, publisher, incomeRS, exchangeRateClient,
                authClient, new DefaultLockingTaskExecutor(lockProvider));
        ctx = mockStatic(RequestContextUtil.class);
        ctx.when(RequestContextUtil::getUserId).thenReturn(USER);
    }

    @AfterEach
    void tearDown() {
        ctx.close();
    }

    private static DebtDocument.DebtDocumentBuilder debt(final String id) {
        return DebtDocument.builder().id(id).name(id).ownerId(USER).categoryId(CAT)
                .amount(BigDecimal.TEN).paymentPeriodType(PaymentPeriodType.MONTHLY);
    }

    private static PaymentEmbedded payment(final LocalDateTime paidTime, final boolean paid, final BigDecimal amount) {
        return PaymentEmbedded.builder().paid(paid).paidTime(paidTime).periodStart(paidTime).amountPaid(amount).build();
    }

    private List<DebtSummaryResponse> list(final String month, final boolean isOwner, final DebtDocument... debts) {
        final CategoryDocument cat = CategoryDocument.builder().id(CAT).ownerId(isOwner ? USER : "someone").name("c").build();
        lenient().when(categoryRS.findByOwnerId(USER)).thenReturn(isOwner ? List.of(cat) : List.of());
        lenient().when(categoryRS.findByMemberUserId(USER)).thenReturn(isOwner ? List.of() : List.of(cat));
        when(debtRS.findByCategoryIdIn(anyList())).thenReturn(List.of(debts));
        return service.get(month, isOwner).getCategories().stream()
                .flatMap(c -> c.getDebts().stream()).toList();
    }

    // ─── month filter ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("month filter keeps debts whose first payment is on or before the month end")
    void monthFilterByFirstPaymentDate() {
        final var started = debt("started").firstPaymentDate(LocalDateTime.of(2026, 1, 10, 9, 0)).build();
        final var future = debt("future").firstPaymentDate(LocalDateTime.of(2026, 5, 10, 9, 0)).build();
        final var undated = debt("undated").firstPaymentDate(null).build();

        assertThat(list(MONTH, true, started, future, undated)).extracting(DebtSummaryResponse::getId)
                .containsExactly("started");
    }

    @Test
    @DisplayName("one-time debts are listed for any month after their start")
    void oneTimeAndPeriodLess() {
        final var oneTime = debt("once").paymentPeriodType(PaymentPeriodType.ONE_TIME)
                .firstPaymentDate(LocalDateTime.of(2026, 1, 10, 9, 0)).build();

        assertThat(list(MONTH, true, oneTime)).extracting(DebtSummaryResponse::getId).containsExactly("once");
    }

    @Test
    @DisplayName("without a month filter every debt is listed")
    void noMonthFilter() {
        final var d = debt("a").firstPaymentDate(LocalDateTime.of(2030, 1, 1, 9, 0)).build();

        assertThat(list(null, true, d)).hasSize(1);
    }

    @Test
    @DisplayName("shared categories expose their owner, own categories do not")
    void ownerUserId() {
        final CategoryDocument shared = CategoryDocument.builder().id(CAT).ownerId("someone").name("c").build();
        when(categoryRS.findByMemberUserId(USER)).thenReturn(List.of(shared));
        when(debtRS.findByCategoryIdIn(anyList())).thenReturn(List.of());

        assertThat(service.get(null, false).getCategories().get(0).getOwnerUserId()).isEqualTo("someone");
        assertThat(service.get(null, false).getCategories().get(0).isOwned()).isFalse();
    }

    // ─── payment status of the displayed period ───────────────────────────────

    private DebtSummaryResponse statusOf(final DebtDocument debt) {
        return list(MONTH, true, debt).get(0);
    }

    @Test
    @DisplayName("COMPLETED debt is always PAID")
    void completedIsPaid() {
        final var d = debt("d").status(DebtStatus.COMPLETED).paymentStatus(PaymentStatus.UNPAID)
                .firstPaymentDate(LocalDateTime.of(2026, 1, 10, 9, 0)).build();

        assertThat(statusOf(d).getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(statusOf(d).isPaidCurrentPeriod()).isTrue();
    }

    @Test
    @DisplayName("no payments → status stored on the document")
    void noPayments() {
        final var d = debt("d").paymentStatus(PaymentStatus.OVERDUE)
                .firstPaymentDate(LocalDateTime.of(2026, 1, 10, 9, 0)).build();

        assertThat(statusOf(d).getPaymentStatus()).isEqualTo(PaymentStatus.OVERDUE);
    }

    @Test
    @DisplayName("payment of the displayed period → PAID")
    void paidInPeriod() {
        final var d = debt("d").firstPaymentDate(LocalDateTime.of(2026, 1, 10, 9, 0))
                .payments(List.of(payment(LocalDateTime.of(2026, 3, 11, 9, 0), true, BigDecimal.TEN))).build();

        assertThat(statusOf(d).getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(statusOf(d).getPaidCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("only an older period was paid → status stored on the document")
    void paidInEarlierPeriodOnly() {
        final var d = debt("d").paymentStatus(PaymentStatus.UNPAID).firstPaymentDate(LocalDateTime.of(2026, 1, 10, 9, 0))
                .payments(List.of(payment(LocalDateTime.of(2026, 1, 11, 9, 0), true, BigDecimal.TEN))).build();

        assertThat(statusOf(d).getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
    }

    @Test
    @DisplayName("partial payment in the period → PARTIALLY_PAID")
    void partialPayment() {
        final var d = debt("d").firstPaymentDate(LocalDateTime.of(2026, 1, 10, 9, 0))
                .payments(List.of(payment(LocalDateTime.of(2026, 3, 11, 9, 0), false, new BigDecimal("3")))).build();

        assertThat(statusOf(d).getPaymentStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);
    }

    @Test
    @DisplayName("unpaid zero-amount record in the period → status stored on the document")
    void zeroAmountRecord() {
        final var d = debt("d").paymentStatus(PaymentStatus.UNPAID).firstPaymentDate(LocalDateTime.of(2026, 1, 10, 9, 0))
                .payments(List.of(payment(LocalDateTime.of(2026, 3, 11, 9, 0), false, BigDecimal.ZERO),
                        payment(null, false, null))).build();

        assertThat(statusOf(d).getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
    }

    @Test
    @DisplayName("future reminder is reported, an elapsed one is not")
    void reminderFlag() {
        final LocalDateTime now = LocalDateTime.now(PaymentPeriodUtil.APP_ZONE);
        final var upcoming = debt("up").firstPaymentDate(now.minusMonths(1)).nextReminderDate(now.plusDays(1)).build();
        final var elapsed = debt("old").firstPaymentDate(now.minusMonths(1)).nextReminderDate(now.minusDays(1)).build();

        final List<DebtSummaryResponse> result = list(null, true, upcoming, elapsed);

        assertThat(result.get(0).isHasReminder()).isTrue();
        assertThat(result.get(1).isHasReminder()).isFalse();
    }
}
