package dev.batuhanyetgin.kamci.service;

import dev.batuhanyetgin.commonmodule.document.BaseDocument;
import dev.batuhanyetgin.commonmodule.exception.BaseException;
import dev.batuhanyetgin.commonmodule.util.RequestContextUtil;
import dev.batuhanyetgin.kamci.document.CategoryDocument;
import dev.batuhanyetgin.kamci.document.DebtDocument;
import dev.batuhanyetgin.kamci.document.UserIncomeDocument;
import dev.batuhanyetgin.kamci.document.embedded.PaymentEmbedded;
import dev.batuhanyetgin.kamci.document.embedded.ReminderEmbedded;
import dev.batuhanyetgin.kamci.dto.request.*;
import dev.batuhanyetgin.kamci.dto.response.*;
import dev.batuhanyetgin.kamci.enums.AccessLevelType;
import dev.batuhanyetgin.kamci.enums.DebtStatus;
import dev.batuhanyetgin.kamci.enums.PaymentPeriodType;
import dev.batuhanyetgin.kamci.enums.PaymentStatus;
import dev.batuhanyetgin.kamci.mapper.DebtMapper;
import dev.batuhanyetgin.kamci.repositoryservice.CategoryRepositoryService;
import dev.batuhanyetgin.kamci.repositoryservice.DebtRepositoryService;
import dev.batuhanyetgin.kamci.repositoryservice.UserIncomeRepositoryService;
import dev.batuhanyetgin.kamci.util.PaymentPeriodUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DebtService {

    private static final int FREE_MAX_DEBTS = 5;
    private static final int PREMIUM_MAX_DEBTS = 500;

    private final DebtRepositoryService debtRS;
    private final CategoryRepositoryService categoryRS;
    private final DebtMapper debtMapper;
    private final DebtEventAsyncPublisher debtEventAsyncPublisher;
    private final UserIncomeRepositoryService incomeRS;
    private final LockingTaskExecutor lockingTaskExecutor;
    @Qualifier("taskExecutor")
    private final Executor taskExecutor;

    public DebtResponse create(final CreateDebtRequest request) {
        final String userId = RequestContextUtil.getUserId();
        final String locale = RequestContextUtil.getLocale();
        final boolean isPremium = RequestContextUtil.isPremium();

        final CategoryDocument category = CompletableFuture
                .supplyAsync(() -> categoryRS.findById(request.getCategoryId()), taskExecutor)
                .join();

        if (lacksWriteAccess(category, userId)) {
            throw BaseException.forbidden("error.debt.permission_denied");
        }

        request.setOwnerId(userId);

        DebtDocument debtDocument = debtMapper.convert(request);
        setNextReminderDate(debtDocument);
        backfillAutoPayments(debtDocument);
        setNextAutoPaymentDate(debtDocument);

        final DebtDocument preparedDebt = debtDocument;
        if (category.getOwnerId().equals(userId)) {
            final int maxDebts = isPremium ? PREMIUM_MAX_DEBTS : FREE_MAX_DEBTS;
            final LockConfiguration lockConfig = new LockConfiguration(
                    Instant.now(), "debt-create:" + userId, Duration.ofSeconds(10), Duration.ZERO);
            try {
                final LockingTaskExecutor.TaskResult<DebtDocument> result = lockingTaskExecutor.executeWithLock(
                        () -> {
                            final List<String> nonSharedCategoryIds = categoryRS.findByOwnerId(userId).stream()
                                    .filter(cat -> cat.getMembers().isEmpty())
                                    .map(BaseDocument::getId).toList();
                            if (debtRS.countByOwnerIdAndCategoryIdIn(userId, nonSharedCategoryIds) >= maxDebts) {
                                throw BaseException.badRequest("error.debt.limit_exceeded");
                            }
                            return debtRS.save(preparedDebt);
                        }, lockConfig);
                if (!result.wasExecuted()) {
                    throw BaseException.conflict("error.debt.concurrent_request");
                }
                debtDocument = result.getResult();
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        } else {
            debtDocument = debtRS.save(preparedDebt);
        }

        log.info("Debt created with id: {} for user: {}", debtDocument.getId(), userId);
        debtEventAsyncPublisher.publishDebtAddedEvent(debtDocument, category, userId, locale);

        return DebtResponse.builder().debt(debtMapper.convert(debtDocument)).build();
    }

    public DebtResponse update(final UpdateDebtRequest request) {
        final String userId = RequestContextUtil.getUserId();
        final DebtDocument existing = debtRS.findById(request.getId());

        if (existing.getStatus() == DebtStatus.COMPLETED) {
            throw BaseException.conflict("error.debt.already_completed");
        }

        final CategoryDocument currentCategory = categoryRS.findById(existing.getCategoryId());
        if (lacksEditAccess(currentCategory, userId)) {
            throw BaseException.forbidden("error.debt.permission_denied");
        }

        if (!existing.getCategoryId().equals(request.getCategoryId())) {
            final CategoryDocument newCategory = categoryRS.findById(request.getCategoryId());
            if (lacksEditAccess(newCategory, userId)) {
                throw BaseException.forbidden("error.debt.permission_denied");
            }
        }

        request.setOwnerId(existing.getOwnerId());

        if (!Objects.equals(request.getAmount(), existing.getAmount())) {
            existing.updateAmount(request.getAmount(), LocalDateTime.now());
        }

        DebtDocument debtDocument = debtMapper.convert(request);
        debtDocument.setPayments(existing.getPayments());
        debtDocument.setAmountHistory(existing.getAmountHistory());
        debtDocument.setNextAutoPaymentDate(existing.getNextAutoPaymentDate());
        setNextReminderDate(debtDocument);
        setNextAutoPaymentDate(debtDocument);
        debtDocument = debtRS.update(debtDocument);

        log.info("Debt updated with id: {}", debtDocument.getId());
        return DebtResponse.builder().debt(debtMapper.convert(debtDocument)).build();
    }

    public DebtResponse pay(final String id, final PayDebtRequest request) {
        final String userId = RequestContextUtil.getUserId();
        final DebtDocument existing = debtRS.findById(id);

        if (existing.getStatus() == DebtStatus.COMPLETED) {
            throw BaseException.conflict("error.debt.already_completed");
        }

        if (existing.isAutoPayment()) {
            throw BaseException.conflict("error.debt.auto_payment_enabled");
        }

        final CategoryDocument category = categoryRS.findById(existing.getCategoryId());
        if (lacksWriteAccess(category, userId)) {
            throw BaseException.forbidden("error.debt.permission_denied");
        }

        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime effectiveTime = (request != null && request.getPaidTime() != null)
                ? request.getPaidTime() : now;
        final boolean isHistorical = effectiveTime.isBefore(now.minusSeconds(1));

        if (existing.getFirstPaymentDate() != null
                && effectiveTime.isBefore(existing.getFirstPaymentDate())) {
            throw BaseException.badRequest("error.debt.payment_before_start");
        }

        final LocalDateTime periodStart = PaymentPeriodUtil.getPeriodStart(
                existing.getFirstPaymentDate(), existing.getPaymentPeriodType(), effectiveTime);
        final boolean alreadyPaidThisPeriod = existing.getPayments().stream()
                .anyMatch(p -> p.isPaid() && p.getPaidTime() != null
                        && !p.getPaidTime().isBefore(periodStart));
        if (alreadyPaidThisPeriod) {
            throw BaseException.conflict("error.debt.already_paid_this_period");
        }

        final int originalDay = existing.getFirstPaymentDate() != null
                ? existing.getFirstPaymentDate().getDayOfMonth() : 1;
        final LocalDateTime periodEnd = PaymentPeriodUtil.getPeriodEnd(
                periodStart, existing.getPaymentPeriodType(), originalDay).minusDays(1);
        final BigDecimal amount = (request != null && request.getAmountPaid() != null)
                ? request.getAmountPaid()
                : existing.getAmount();
        existing.addPayment(effectiveTime, periodStart, periodEnd, amount);

        final boolean isFullPayment = amount.compareTo(existing.getAmount()) >= 0;
        if (!isHistorical) {
            existing.setPaymentStatus(isFullPayment ? PaymentStatus.PAID : PaymentStatus.PARTIALLY_PAID);
        }

        if (existing.getInstallmentCount() != null
                && existing.getPayments().size() >= existing.getInstallmentCount()) {
            existing.setStatus(DebtStatus.COMPLETED);
            log.info("Debt {} marked as COMPLETED — all {} installments paid", id, existing.getInstallmentCount());
        } else {
            log.info("Payment recorded for debt {}. Payment status: {}", id, existing.getPaymentStatus());
        }

        final DebtDocument updated = debtRS.update(existing);
        log.info("Payment recorded for debt: {}", id);
        return DebtResponse.builder().debt(debtMapper.convert(updated)).build();
    }

    public DebtResponse skip(final String id, final SkipDebtRequest request) {
        final String userId = RequestContextUtil.getUserId();
        final DebtDocument existing = debtRS.findById(id);

        if (existing.getStatus() == DebtStatus.COMPLETED) {
            throw BaseException.conflict("error.debt.already_completed");
        }

        if (existing.isAutoPayment()) {
            throw BaseException.conflict("error.debt.auto_payment_enabled");
        }

        final CategoryDocument category = categoryRS.findById(existing.getCategoryId());
        if (lacksWriteAccess(category, userId)) {
            throw BaseException.forbidden("error.debt.permission_denied");
        }

        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime effectiveTime = (request != null && request.getSkippedAt() != null)
                ? request.getSkippedAt() : now;
        final boolean isHistorical = effectiveTime.isBefore(now.minusSeconds(1));

        if (existing.getFirstPaymentDate() != null
                && effectiveTime.isBefore(existing.getFirstPaymentDate())) {
            throw BaseException.badRequest("error.debt.payment_before_start");
        }

        final LocalDateTime periodStart = PaymentPeriodUtil.getPeriodStart(
                existing.getFirstPaymentDate(), existing.getPaymentPeriodType(), effectiveTime);
        final boolean periodAlreadyRecorded = existing.getPayments().stream()
                .anyMatch(p -> p.getPaidTime() != null && !p.getPaidTime().isBefore(periodStart));
        if (periodAlreadyRecorded) {
            throw BaseException.conflict("error.debt.period_already_recorded");
        }

        final int originalDay = existing.getFirstPaymentDate() != null
                ? existing.getFirstPaymentDate().getDayOfMonth() : 1;
        final LocalDateTime periodEnd = PaymentPeriodUtil.getPeriodEnd(
                periodStart, existing.getPaymentPeriodType(), originalDay).minusDays(1);
        existing.skipPayment(effectiveTime, periodStart, periodEnd);

        if (!isHistorical && existing.getPaymentStatus() == PaymentStatus.OVERDUE) {
            existing.setPaymentStatus(PaymentStatus.UNPAID);
            log.info("Debt {} payment status restored to UNPAID after skip acknowledgement", id);
        }

        final DebtDocument updated = debtRS.update(existing);
        log.info("Payment skipped for debt: {}", id);
        return DebtResponse.builder().debt(debtMapper.convert(updated)).build();
    }

    public DebtResponse bulkPay(final String id, final List<PayDebtRequest> requests) {
        final String userId = RequestContextUtil.getUserId();
        final DebtDocument existing = debtRS.findById(id);

        if (existing.getStatus() == DebtStatus.COMPLETED) {
            throw BaseException.conflict("error.debt.already_completed");
        }
        if (existing.isAutoPayment()) {
            throw BaseException.conflict("error.debt.auto_payment_enabled");
        }

        final CategoryDocument category = categoryRS.findById(existing.getCategoryId());
        if (lacksWriteAccess(category, userId)) {
            throw BaseException.forbidden("error.debt.permission_denied");
        }

        final LocalDateTime now = LocalDateTime.now();
        final int originalDay = existing.getFirstPaymentDate() != null
                ? existing.getFirstPaymentDate().getDayOfMonth() : 1;

        final List<PayDebtRequest> sorted = requests.stream()
                .sorted(Comparator.comparing(r -> r.getPaidTime() != null ? r.getPaidTime() : now))
                .toList();

        for (final PayDebtRequest req : sorted) {
            final LocalDateTime effectiveTime = req.getPaidTime() != null ? req.getPaidTime() : now;

            if (effectiveTime.isAfter(now)) {
                throw BaseException.badRequest("error.validation.past_or_present");
            }
            if (existing.getFirstPaymentDate() != null
                    && effectiveTime.isBefore(existing.getFirstPaymentDate())) {
                throw BaseException.badRequest("error.debt.payment_before_start");
            }

            final LocalDateTime periodStart = PaymentPeriodUtil.getPeriodStart(
                    existing.getFirstPaymentDate(), existing.getPaymentPeriodType(), effectiveTime);
            final boolean alreadyPaid = existing.getPayments().stream()
                    .anyMatch(p -> p.isPaid() && p.getPaidTime() != null
                            && !p.getPaidTime().isBefore(periodStart));
            if (alreadyPaid) {
                continue;
            }

            final LocalDateTime periodEnd = PaymentPeriodUtil.getPeriodEnd(
                    periodStart, existing.getPaymentPeriodType(), originalDay).minusDays(1);
            final BigDecimal amount = req.getAmountPaid() != null
                    ? req.getAmountPaid() : existing.getAmount();
            existing.addPayment(effectiveTime, periodStart, periodEnd, amount);
        }

        if (existing.getInstallmentCount() != null
                && existing.getPayments().size() >= existing.getInstallmentCount()) {
            existing.setStatus(DebtStatus.COMPLETED);
            log.info("Debt {} marked as COMPLETED after bulk payment", id);
        } else {
            final LocalDateTime latestEffective = sorted.stream()
                    .map(r -> r.getPaidTime() != null ? r.getPaidTime() : now)
                    .max(Comparator.naturalOrder()).orElse(now);
            final boolean isHistorical = latestEffective.isBefore(now.minusSeconds(1));
            if (!isHistorical) {
                final boolean anyPartial = existing.getPayments().stream()
                        .anyMatch(p -> p.isPaid() && p.getAmountPaid() != null
                                && p.getAmountPaid().compareTo(existing.getAmount()) < 0);
                existing.setPaymentStatus(anyPartial ? PaymentStatus.PARTIALLY_PAID : PaymentStatus.PAID);
            }
        }

        final DebtDocument updated = debtRS.update(existing);
        log.info("Bulk payment ({} entries) recorded for debt: {}", sorted.size(), id);
        return DebtResponse.builder().debt(debtMapper.convert(updated)).build();
    }

    public DebtResponse setPaidInstallments(final String id, final SetPaidInstallmentsRequest request) {
        final String userId = RequestContextUtil.getUserId();
        final DebtDocument existing = debtRS.findById(id);

        if (existing.getStatus() == DebtStatus.COMPLETED) {
            throw BaseException.conflict("error.debt.already_completed");
        }
        if (existing.isAutoPayment()) {
            throw BaseException.conflict("error.debt.auto_payment_enabled");
        }
        if (existing.getFirstPaymentDate() == null || existing.getPaymentPeriodType() == null) {
            throw BaseException.badRequest("error.debt.missing_period_config");
        }
        if (existing.getInstallmentCount() != null && request.getCount() > existing.getInstallmentCount()) {
            throw BaseException.badRequest("error.debt.installment_count_exceeded");
        }

        final CategoryDocument category = categoryRS.findById(existing.getCategoryId());
        if (lacksWriteAccess(category, userId)) {
            throw BaseException.forbidden("error.debt.permission_denied");
        }

        final int originalDay = existing.getFirstPaymentDate().getDayOfMonth();
        LocalDateTime periodStart = existing.getFirstPaymentDate();

        for (int i = 0; i < request.getCount(); i++) {
            final LocalDateTime ps = periodStart;
            final boolean alreadyRecorded = existing.getPayments().stream()
                    .anyMatch(p -> p.getPeriodStart() != null && p.getPeriodStart().equals(ps));

            if (!alreadyRecorded) {
                final LocalDateTime rawEnd = PaymentPeriodUtil.getPeriodEnd(
                        ps, existing.getPaymentPeriodType(), originalDay);
                final LocalDateTime pe = rawEnd != null ? rawEnd.minusDays(1) : null;
                existing.addPayment(ps, ps, pe, existing.getAmount());
            }
            periodStart = PaymentPeriodUtil.advanceDate(periodStart, existing.getPaymentPeriodType(), originalDay);
            if (periodStart == null) break;
        }

        if (existing.getInstallmentCount() != null
                && existing.getPayments().size() >= existing.getInstallmentCount()) {
            existing.setStatus(DebtStatus.COMPLETED);
            log.info("Debt {} marked as COMPLETED after setting {} paid installments", id, request.getCount());
        }

        final DebtDocument updated = debtRS.update(existing);
        log.info("Set {} paid installments for debt: {}", request.getCount(), id);
        return DebtResponse.builder().debt(debtMapper.convert(updated)).build();
    }

    public DebtDetailResponse getById(final String id) {
        final String userId = RequestContextUtil.getUserId();
        final DebtDocument debt = debtRS.findById(id);

        final CategoryDocument category = categoryRS.findById(debt.getCategoryId());
        if (lacksReadAccess(category, userId)) {
            throw BaseException.forbidden("error.debt.permission_denied");
        }

        log.info("Debt detail fetched for id: {} by user: {}", id, userId);
        return debtMapper.convertToDetail(debt);
    }

    public void delete(final String id) {
        final String userId = RequestContextUtil.getUserId();
        final DebtDocument debtDocument = debtRS.findById(id);

        final CategoryDocument category = categoryRS.findById(debtDocument.getCategoryId());
        if (lacksDeleteAccess(category, userId)) {
            throw BaseException.forbidden("error.debt.permission_denied");
        }

        debtRS.delete(id);
        log.warn("Deleted debt: {}", id);
    }

    public GetDebtResponse get(final String month, final boolean isOwner) {
        final String userId = RequestContextUtil.getUserId();

        log.info("Getting debts for user: {} isOwner: {}", userId, isOwner);

        final List<CategoryDocument> cats = isOwner
                ? categoryRS.findByOwnerId(userId)
                : categoryRS.findByMemberUserId(userId);

        final List<String> categoryIds = cats.stream().map(BaseDocument::getId).toList();
        final Map<String, List<DebtDocument>> byCategory = debtRS.findByCategoryIdIn(categoryIds)
                .stream().collect(Collectors.groupingBy(DebtDocument::getCategoryId));

        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime monthStart;
        final LocalDateTime monthEnd;
        final LocalDateTime referenceTime;
        if (month != null) {
            final YearMonth ym = YearMonth.parse(month);
            monthStart = ym.atDay(1).atStartOfDay();
            monthEnd = ym.atEndOfMonth().atTime(23, 59, 59);
            referenceTime = monthEnd;
        } else {
            monthStart = null;
            monthEnd = null;
            referenceTime = now;
        }

        final List<CategoryDebtResponse> categories = cats.stream()
                .map(cat -> CategoryDebtResponse.builder()
                        .categoryId(cat.getId())
                        .categoryName(cat.getName())
                        .owned(isOwner)
                        .ownerUserId(isOwner ? null : cat.getOwnerId())
                        .debts(byCategory.getOrDefault(cat.getId(), List.of())
                                .stream()
                                .filter(debt -> monthEnd == null
                                        || (debt.getFirstPaymentDate() != null
                                        && !debt.getFirstPaymentDate().isAfter(monthEnd)
                                        && isPaymentDueInMonth(debt, monthStart, monthEnd)))
                                .map(debt -> {
                                    final PaymentStatus status = calculatePaymentStatusAt(debt, referenceTime);
                                    final int paidCount = (int) debt.getPayments().stream()
                                            .filter(PaymentEmbedded::isPaid).count();
                                    final boolean hasReminder = debt.getNextReminderDate() != null
                                            && debt.getNextReminderDate().isAfter(now);
                                    return DebtSummaryResponse.builder()
                                            .id(debt.getId())
                                            .name(debt.getName())
                                            .categoryId(debt.getCategoryId())
                                            .installmentCount(debt.getInstallmentCount())
                                            .paidCount(paidCount)
                                            .paymentPeriodType(debt.getPaymentPeriodType())
                                            .amount(debt.getAmount())
                                            .currency(debt.getCurrency())
                                            .status(debt.getStatus())
                                            .paymentStatus(status)
                                            .autoPayment(debt.isAutoPayment())
                                            .paidCurrentPeriod(status == PaymentStatus.PAID)
                                            .hasReminder(hasReminder)
                                            .build();
                                }).toList())
                        .build())
                .toList();

        return GetDebtResponse.builder().categories(categories).build();
    }

    private boolean isPaymentDueInMonth(final DebtDocument debt,
                                        final LocalDateTime monthStart,
                                        final LocalDateTime monthEnd) {
        if (debt.getPaymentPeriodType() == null
                || debt.getPaymentPeriodType() == PaymentPeriodType.ONE_TIME) {
            return true;
        }
        final LocalDateTime periodStart = PaymentPeriodUtil.getPeriodStart(
                debt.getFirstPaymentDate(), debt.getPaymentPeriodType(), monthEnd);
        return !periodStart.isBefore(monthStart) && !periodStart.isAfter(monthEnd);
    }

    private boolean lacksReadAccess(final CategoryDocument category, final String userId) {
        if (category.getOwnerId().equals(userId)) {
            return false;
        }
        return category.getMembers().stream()
                .noneMatch(m -> m.getUserId().equals(userId));
    }

    private boolean lacksWriteAccess(final CategoryDocument category, final String userId) {
        if (category.getOwnerId().equals(userId)) {
            return false;
        }
        return category.getMembers().stream()
                .noneMatch(m -> m.getUserId().equals(userId)
                        && (m.getAccessLevel() == AccessLevelType.WRITE
                        || m.getAccessLevel() == AccessLevelType.EDIT
                        || m.getAccessLevel() == AccessLevelType.DELETE));
    }

    private boolean lacksEditAccess(final CategoryDocument category, final String userId) {
        if (category.getOwnerId().equals(userId)) {
            return false;
        }
        return category.getMembers().stream()
                .noneMatch(m -> m.getUserId().equals(userId)
                        && (m.getAccessLevel() == AccessLevelType.EDIT
                        || m.getAccessLevel() == AccessLevelType.DELETE));
    }

    private boolean lacksDeleteAccess(final CategoryDocument category, final String userId) {
        if (category.getOwnerId().equals(userId)) {
            return false;
        }
        return category.getMembers().stream()
                .noneMatch(m -> m.getUserId().equals(userId)
                        && m.getAccessLevel() == AccessLevelType.DELETE);
    }

    private void setNextReminderDate(final DebtDocument debtDocument) {
        final ReminderEmbedded reminder = debtDocument.getReminder();

        if (reminder == null || !reminder.isEnabled()) {
            return;
        }

        if (!RequestContextUtil.isPremium()) {
            throw BaseException.forbidden("error.debt.premium_required");
        }

        if (reminder.getReminderDate() == null || reminder.getReminderTime() == null) {
            throw BaseException.badRequest("error.reminder.date_or_time_missing");
        }

        debtDocument.setNextReminderDate(
                LocalDateTime.of(reminder.getReminderDate(), reminder.getReminderTime())
        );
    }

    private PaymentStatus calculatePaymentStatusAt(final DebtDocument debt, final LocalDateTime referenceTime) {
        if (debt.getStatus() == DebtStatus.COMPLETED) {
            return PaymentStatus.PAID;
        }

        if (debt.getPayments() == null || debt.getPayments().isEmpty()
                || debt.getPaymentPeriodType() == null) {
            return debt.getPaymentStatus();
        }

        final LocalDateTime periodStart = PaymentPeriodUtil.getPeriodStart(
                debt.getFirstPaymentDate(), debt.getPaymentPeriodType(), referenceTime);

        final List<PaymentEmbedded> currentPeriodPayments = debt.getPayments().stream()
                .filter(p -> p.getPaidTime() != null && !p.getPaidTime().isBefore(periodStart))
                .toList();

        if (currentPeriodPayments.isEmpty()) {
            return debt.getPaymentStatus(); // Returns UNPAID or OVERDUE from document
        }

        final boolean hasFullPayment = currentPeriodPayments.stream().anyMatch(PaymentEmbedded::isPaid);
        if (hasFullPayment) {
            return PaymentStatus.PAID;
        }

        final boolean hasPartialPayment = currentPeriodPayments.stream()
                .anyMatch(p -> p.getAmountPaid() != null && p.getAmountPaid().compareTo(BigDecimal.ZERO) > 0);

        return hasPartialPayment ? PaymentStatus.PARTIALLY_PAID : debt.getPaymentStatus();
    }

    public List<PaymentHistoryResponse> getPaymentHistory(final String id,
                                                          final LocalDateTime from,
                                                          final LocalDateTime to) {
        final String userId = RequestContextUtil.getUserId();
        final DebtDocument debt = debtRS.findById(id);
        final CategoryDocument category = categoryRS.findById(debt.getCategoryId());
        if (lacksReadAccess(category, userId)) {
            throw BaseException.forbidden("error.debt.permission_denied");
        }

        return debt.getPayments().stream()
                .filter(p -> {
                    final LocalDateTime ts = p.getPeriodStart() != null ? p.getPeriodStart() : p.getPaidTime();
                    return ts != null && !ts.isBefore(from) && !ts.isAfter(to);
                })
                .map(p -> PaymentHistoryResponse.builder()
                        .paid(p.isPaid())
                        .paidTime(p.getPaidTime())
                        .periodStart(p.getPeriodStart())
                        .periodEnd(p.getPeriodEnd())
                        .amountPaid(p.getAmountPaid())
                        .build())
                .toList();
    }

    public MonthlySummaryResponse getMonthlySummary(final String month) {
        final String userId = RequestContextUtil.getUserId();
        final YearMonth yearMonth = YearMonth.parse(month);
        final LocalDateTime monthStart = yearMonth.atDay(1).atStartOfDay();
        final LocalDateTime monthEnd = yearMonth.atEndOfMonth().atTime(23, 59, 59);

        final List<CategoryDocument> owned = categoryRS.findByOwnerId(userId);
        final List<CategoryDocument> shared = categoryRS.findByMemberUserId(userId);
        final List<CategoryDocument> all = new ArrayList<>();
        all.addAll(owned);
        all.addAll(shared);

        final List<String> categoryIds = all.stream().map(BaseDocument::getId).toList();
        final List<DebtDocument> debts = debtRS.findByCategoryIdIn(categoryIds);

        BigDecimal totalPaid = BigDecimal.ZERO;
        int paidCount = 0;
        int skippedCount = 0;
        final List<DebtMonthlySummaryItem> items = new ArrayList<>();

        for (final DebtDocument debt : debts) {
            final List<PaymentEmbedded> monthPayments = debt.getPayments().stream()
                    .filter(p -> {
                        final LocalDateTime ts = p.getPeriodStart() != null ? p.getPeriodStart() : p.getPaidTime();
                        return ts != null && !ts.isBefore(monthStart) && !ts.isAfter(monthEnd);
                    })
                    .toList();

            if (monthPayments.isEmpty()) {
                continue;
            }

            final boolean isPaid = monthPayments.stream().anyMatch(PaymentEmbedded::isPaid);
            final BigDecimal paidAmount = monthPayments.stream()
                    .filter(PaymentEmbedded::isPaid)
                    .map(p -> p.getAmountPaid() != null ? p.getAmountPaid() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            final LocalDateTime lastPaidTime = monthPayments.stream()
                    .filter(PaymentEmbedded::isPaid)
                    .map(PaymentEmbedded::getPaidTime)
                    .filter(Objects::nonNull)
                    .max(Comparator.naturalOrder()).orElse(null);

            if (isPaid) {
                totalPaid = totalPaid.add(paidAmount);
                paidCount++;
            } else {
                skippedCount++;
            }

            items.add(DebtMonthlySummaryItem.builder()
                    .debtId(debt.getId())
                    .debtName(debt.getName())
                    .amountPaid(isPaid ? paidAmount : null)
                    .paid(isPaid)
                    .skipped(!isPaid)
                    .paidTime(lastPaidTime)
                    .build());
        }

        final var incomeOpt = incomeRS.findByOwnerIdAsOf(userId, monthEnd);
        final BigDecimal income = incomeOpt.map(UserIncomeDocument::getAmount).orElse(null);
        BigDecimal ratio = null;
        if (income != null && income.compareTo(BigDecimal.ZERO) > 0) {
            ratio = totalPaid.divide(income, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
        }

        return MonthlySummaryResponse.builder()
                .month(month)
                .totalPaid(totalPaid)
                .paidCount(paidCount)
                .skippedCount(skippedCount)
                .incomeAtMonth(income)
                .debtToIncomeRatio(ratio)
                .debts(items)
                .build();
    }

    private void backfillAutoPayments(final DebtDocument debt) {
        if (!debt.isAutoPayment() || debt.getFirstPaymentDate() == null
                || debt.getPaymentPeriodType() == null) {
            return;
        }

        final LocalDateTime now = LocalDateTime.now();
        if (debt.getFirstPaymentDate().isAfter(now)) {
            return;
        }

        final int originalDay = debt.getFirstPaymentDate().getDayOfMonth();
        LocalDateTime periodStart = debt.getFirstPaymentDate();

        while (!periodStart.isAfter(now)) {
            final LocalDateTime rawEnd = PaymentPeriodUtil.getPeriodEnd(
                    periodStart, debt.getPaymentPeriodType(), originalDay);
            final LocalDateTime periodEnd = rawEnd != null ? rawEnd.minusDays(1) : null;
            debt.addPayment(periodStart, periodStart, periodEnd, debt.getAmount());

            if (debt.getInstallmentCount() != null
                    && (long) debt.getPayments().stream().filter(PaymentEmbedded::isPaid).count()
                            >= debt.getInstallmentCount()) {
                debt.setStatus(DebtStatus.COMPLETED);
                debt.setNextAutoPaymentDate(null);
                log.info("Debt backfill: marked COMPLETED after {} installments",
                        debt.getInstallmentCount());
                return;
            }

            periodStart = PaymentPeriodUtil.advanceDate(
                    periodStart, debt.getPaymentPeriodType(), originalDay);
            if (periodStart == null) break;
        }

        debt.setNextAutoPaymentDate(periodStart);
        debt.setPaymentStatus(PaymentStatus.PAID);
        log.info("Debt backfill: {} payments generated, nextAutoPaymentDate={}",
                debt.getPayments().size(), periodStart);
    }

    private void setNextAutoPaymentDate(final DebtDocument debtDocument) {
        if (!debtDocument.isAutoPayment() || debtDocument.getFirstPaymentDate() == null) {
            return;
        }
        if (debtDocument.getNextAutoPaymentDate() == null) {
            debtDocument.setNextAutoPaymentDate(debtDocument.getFirstPaymentDate());
        }
    }
}