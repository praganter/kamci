package dev.batuhanyetgin.debttracker.service;

import dev.batuhanyetgin.commonmodule.document.BaseDocument;
import dev.batuhanyetgin.commonmodule.exception.BaseException;
import dev.batuhanyetgin.commonmodule.util.RequestContextUtil;
import dev.batuhanyetgin.debttracker.document.CategoryDocument;
import dev.batuhanyetgin.debttracker.document.DebtDocument;
import dev.batuhanyetgin.debttracker.document.embedded.ReminderEmbedded;
import dev.batuhanyetgin.debttracker.dto.request.CreateDebtRequest;
import dev.batuhanyetgin.debttracker.dto.request.PayDebtRequest;
import dev.batuhanyetgin.debttracker.dto.request.UpdateDebtRequest;
import dev.batuhanyetgin.debttracker.dto.response.*;
import dev.batuhanyetgin.debttracker.document.embedded.PaymentEmbedded;
import dev.batuhanyetgin.debttracker.enums.AccessLevelType;
import dev.batuhanyetgin.debttracker.enums.DebtStatus;
import dev.batuhanyetgin.debttracker.mapper.DebtMapper;
import dev.batuhanyetgin.debttracker.repositoryservice.CategoryRepositoryService;
import dev.batuhanyetgin.debttracker.repositoryservice.DebtRepositoryService;
import dev.batuhanyetgin.debttracker.repositoryservice.UserIncomeRepositoryService;
import dev.batuhanyetgin.debttracker.util.PaymentPeriodUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DebtService {

    private static final int FREE_MAX_DEBTS = 5;
    private static final int PREMIUM_MAX_DEBTS = 200;

    private final DebtRepositoryService debtRS;
    private final CategoryRepositoryService categoryRS;
    private final DebtMapper debtMapper;
    private final DebtEventAsyncPublisher debtEventAsyncPublisher;
    private final UserIncomeRepositoryService incomeRS;
    @Qualifier("taskExecutor")
    private final Executor taskExecutor;

    public DebtResponse create(final CreateDebtRequest request) {
        final String userId = RequestContextUtil.getUserId();
        final String locale = RequestContextUtil.getLocale();

        final CompletableFuture<Long> countFuture =
                CompletableFuture.supplyAsync(() -> debtRS.countByOwnerId(userId), taskExecutor);
        final CompletableFuture<CategoryDocument> categoryFuture =
                CompletableFuture.supplyAsync(() -> categoryRS.findById(request.getCategoryId()), taskExecutor);
        CompletableFuture.allOf(countFuture, categoryFuture).join();

        final int maxDebts = RequestContextUtil.isPremium() ? PREMIUM_MAX_DEBTS : FREE_MAX_DEBTS;
        if (countFuture.join() >= maxDebts) {
            throw BaseException.badRequest("error.debt.limit_exceeded");
        }

        final CategoryDocument category = categoryFuture.join();
        if (lacksWriteAccess(category, userId)) {
            throw BaseException.forbidden("error.debt.permission_denied");
        }

        request.setOwnerId(userId);

        DebtDocument debtDocument = debtMapper.convert(request);
        setNextReminderDate(debtDocument);
        setNextAutoPaymentDate(debtDocument);
        debtDocument = debtRS.save(debtDocument);

        log.info("Debt created with id: {} for user: {}", debtDocument.getId(), userId);
        debtEventAsyncPublisher.publishDebtAddedEvent(debtDocument, category, userId, locale);

        return DebtResponse.builder().debt(debtMapper.convert(debtDocument)).build();
    }

    public DebtResponse update(final UpdateDebtRequest request) {
        final String userId = RequestContextUtil.getUserId();
        final DebtDocument existing = debtRS.findById(request.getId());

        if (existing.getStatus() == DebtStatus.PAID) {
            throw BaseException.conflict("error.debt.already_completed");
        }

        final CategoryDocument currentCategory = categoryRS.findById(existing.getCategoryId());
        if (lacksWriteAccess(currentCategory, userId)) {
            throw BaseException.forbidden("error.debt.permission_denied");
        }

        if (!existing.getCategoryId().equals(request.getCategoryId())) {
            final CategoryDocument newCategory = categoryRS.findById(request.getCategoryId());
            if (lacksWriteAccess(newCategory, userId)) {
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

        if (existing.getStatus() == DebtStatus.PAID) {
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
        final LocalDateTime periodStart = PaymentPeriodUtil.getPeriodStart(
                existing.getFirstPaymentDate(), existing.getPaymentPeriodType(), now);
        final boolean alreadyPaidThisPeriod = existing.getPayments().stream()
                .anyMatch(p -> p.isPaid() && p.getPaidTime() != null
                        && !p.getPaidTime().isBefore(periodStart));
        if (alreadyPaidThisPeriod) {
            throw BaseException.conflict("error.debt.already_paid_this_period");
        }

        final int originalDay = existing.getFirstPaymentDate() != null
                ? existing.getFirstPaymentDate().getDayOfMonth() : 1;
        final LocalDateTime periodEnd = PaymentPeriodUtil.getPeriodEnd(
                periodStart, existing.getPaymentPeriodType(), originalDay);
        final BigDecimal amount = (request != null && request.getAmountPaid() != null)
                ? request.getAmountPaid()
                : existing.getAmount();
        existing.addPayment(now, periodStart, periodEnd, amount);

        if (existing.getInstallmentCount() != null
                && existing.getPayments().size() >= existing.getInstallmentCount()) {
            existing.setStatus(DebtStatus.PAID);
            log.info("Debt {} marked as PAID — all {} installments paid", id, existing.getInstallmentCount());
            } else if (existing.getStatus() == DebtStatus.OVERDUE) {
            existing.setStatus(DebtStatus.UNPAID);
            log.info("Debt {} restored to UNPAID after overdue payment", id);
            }

        final DebtDocument updated = debtRS.update(existing);
        log.info("Payment recorded for debt: {}", id);
        return DebtResponse.builder().debt(debtMapper.convert(updated)).build();
    }

    public DebtResponse skip(final String id) {
        final String userId = RequestContextUtil.getUserId();
        final DebtDocument existing = debtRS.findById(id);

        if (existing.getStatus() == DebtStatus.PAID) {
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
        final LocalDateTime periodStart = PaymentPeriodUtil.getPeriodStart(
                existing.getFirstPaymentDate(), existing.getPaymentPeriodType(), now);
        final boolean periodAlreadyRecorded = existing.getPayments().stream()
                .anyMatch(p -> p.getPaidTime() != null && !p.getPaidTime().isBefore(periodStart));
        if (periodAlreadyRecorded) {
            throw BaseException.conflict("error.debt.period_already_recorded");
        }

        final int originalDay = existing.getFirstPaymentDate() != null
                ? existing.getFirstPaymentDate().getDayOfMonth() : 1;
        final LocalDateTime periodEnd = PaymentPeriodUtil.getPeriodEnd(
                periodStart, existing.getPaymentPeriodType(), originalDay);
        existing.skipPayment(now, periodStart, periodEnd);

        if (existing.getStatus() == DebtStatus.OVERDUE) {
            existing.setStatus(DebtStatus.UNPAID);
            log.info("Debt {} restored to UNPAID after skip acknowledgement", id);
        }

        final DebtDocument updated = debtRS.update(existing);
        log.info("Payment skipped for debt: {}", id);
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
        if (lacksWriteAccess(category, userId)) {
            throw BaseException.forbidden("error.debt.permission_denied");
        }

        debtRS.delete(id);
        log.warn("Deleted debt: {}", id);
    }

    public GetDebtResponse get() {
        final String userId = RequestContextUtil.getUserId();

        log.info("Getting debts for user: {}", userId);

        final CompletableFuture<List<CategoryDocument>> ownedFuture =
                CompletableFuture.supplyAsync(() -> categoryRS.findByOwnerId(userId), taskExecutor);
        final CompletableFuture<List<CategoryDocument>> sharedFuture =
                CompletableFuture.supplyAsync(() -> categoryRS.findByMemberUserId(userId), taskExecutor);
        CompletableFuture.allOf(ownedFuture, sharedFuture).join();

        final List<CategoryDocument> owned = ownedFuture.join();
        final List<CategoryDocument> shared = sharedFuture.join();

        final List<CategoryDocument> all = new ArrayList<>();
        all.addAll(owned);
        all.addAll(shared);

        final List<String> categoryIds = all.stream().map(BaseDocument::getId).toList();
        final Map<String, List<DebtDocument>> byCategory = debtRS.findByCategoryIdIn(categoryIds)
                .stream().collect(Collectors.groupingBy(DebtDocument::getCategoryId));

        final Set<String> ownedIds = owned.stream()
                .map(BaseDocument::getId).collect(Collectors.toSet());

        final List<CategoryDebtResponse> categories = all.stream()
                .map(cat -> CategoryDebtResponse.builder()
                        .categoryId(cat.getId())
                        .categoryName(cat.getName())
                        .owned(ownedIds.contains(cat.getId()))
                        .ownerUserId(ownedIds.contains(cat.getId()) ? null : cat.getOwnerId())
                        .debts(byCategory.getOrDefault(cat.getId(), List.of())
                                .stream().map(debt -> {
                                    final DebtItemResponse item = debtMapper.convert(debt);
                                    item.setPaidCurrentPeriod(isPaidCurrentPeriod(debt));
                                    return item;
                                }).toList())
                        .build())
                .toList();

        return GetDebtResponse.builder().categories(categories).build();
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
                        && m.getAccessLevel() == AccessLevelType.WRITE);
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

    private boolean isPaidCurrentPeriod(final DebtDocument debt) {
        if (debt.getPayments() == null || debt.getPayments().isEmpty()
                || debt.getPaymentPeriodType() == null) {
            return false;
        }
        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime periodStart = PaymentPeriodUtil.getPeriodStart(
                debt.getFirstPaymentDate(), debt.getPaymentPeriodType(), now);
        return debt.getPayments().stream()
                .anyMatch(p -> p.isPaid() && p.getPaidTime() != null
                        && !p.getPaidTime().isBefore(periodStart));
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

            final PaymentEmbedded entry = monthPayments.get(0);
            final BigDecimal paidAmount = entry.isPaid() && entry.getAmountPaid() != null
                    ? entry.getAmountPaid() : BigDecimal.ZERO;
            if (entry.isPaid()) {
                totalPaid = totalPaid.add(paidAmount);
                paidCount++;
            } else {
                skippedCount++;
            }

            items.add(DebtMonthlySummaryItem.builder()
                    .debtId(debt.getId())
                    .debtName(debt.getName())
                    .amountPaid(entry.isPaid() ? paidAmount : null)
                    .paid(entry.isPaid())
                    .skipped(!entry.isPaid())
                    .paidTime(entry.getPaidTime())
                    .build());
        }

        final var incomeOpt = incomeRS.findByOwnerIdAsOf(userId, monthEnd);
        final BigDecimal income = incomeOpt.map(i -> i.getAmount()).orElse(null);
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

    private void setNextAutoPaymentDate(final DebtDocument debtDocument) {
        if (!debtDocument.isAutoPayment() || debtDocument.getFirstPaymentDate() == null) {
            return;
        }
        if (debtDocument.getNextAutoPaymentDate() == null) {
            debtDocument.setNextAutoPaymentDate(debtDocument.getFirstPaymentDate());
        }
    }
}