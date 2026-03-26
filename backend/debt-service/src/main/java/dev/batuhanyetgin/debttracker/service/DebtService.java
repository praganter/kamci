package dev.batuhanyetgin.debttracker.service;

import dev.batuhanyetgin.commonmodule.document.BaseDocument;
import dev.batuhanyetgin.commonmodule.exception.BaseException;
import dev.batuhanyetgin.commonmodule.util.RequestContextUtil;
import dev.batuhanyetgin.debttracker.document.CategoryDocument;
import dev.batuhanyetgin.debttracker.document.DebtDocument;
import dev.batuhanyetgin.debttracker.document.embedded.PaymentEmbedded;
import dev.batuhanyetgin.debttracker.document.embedded.ReminderEmbedded;
import dev.batuhanyetgin.debttracker.dto.request.CreateDebtRequest;
import dev.batuhanyetgin.debttracker.dto.request.UpdateDebtRequest;
import dev.batuhanyetgin.debttracker.dto.response.*;
import dev.batuhanyetgin.debttracker.enums.AccessLevelType;
import dev.batuhanyetgin.debttracker.enums.DebtStatus;
import dev.batuhanyetgin.debttracker.enums.PaymentPeriodType;
import dev.batuhanyetgin.debttracker.mapper.DebtMapper;
import dev.batuhanyetgin.debttracker.repositoryservice.CategoryRepositoryService;
import dev.batuhanyetgin.debttracker.repositoryservice.DebtRepositoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DebtService {

    private static final int MAX_DEBTS = 150;

    private final DebtRepositoryService debtRS;
    private final CategoryRepositoryService categoryRS;
    private final DebtMapper debtMapper;
    private final DebtEventAsyncPublisher debtEventAsyncPublisher;
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

        if (countFuture.join() >= MAX_DEBTS) {
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

        DebtDocument debtDocument = debtMapper.convert(request);
        debtDocument.setPayments(existing.getPayments());
        debtDocument.setNextAutoPaymentDate(existing.getNextAutoPaymentDate());
        setNextReminderDate(debtDocument);
        setNextAutoPaymentDate(debtDocument);
        debtDocument = debtRS.update(debtDocument);

        log.info("Debt updated with id: {}", debtDocument.getId());
        return DebtResponse.builder().debt(debtMapper.convert(debtDocument)).build();
    }

    public DebtResponse pay(final String id) {
        final String userId = RequestContextUtil.getUserId();
        final DebtDocument existing = debtRS.findById(id);

        if (existing.isAutoPayment()) {
            throw BaseException.conflict("error.debt.auto_payment_enabled");
        }

        final CategoryDocument category = categoryRS.findById(existing.getCategoryId());
        if (lacksWriteAccess(category, userId)) {
            throw BaseException.forbidden("error.debt.permission_denied");
        }

        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime periodStart = getCurrentPeriodStart(
                existing.getFirstPaymentDate(), existing.getPaymentPeriodType(), now);
        final boolean alreadyPaidThisPeriod = existing.getPayments().stream()
                .anyMatch(p -> !p.getPaidTime().isBefore(periodStart));
        if (alreadyPaidThisPeriod) {
            throw BaseException.conflict("error.debt.already_paid_this_period");
        }

        final List<PaymentEmbedded> payments = new ArrayList<>(existing.getPayments());
        payments.add(PaymentEmbedded.builder()
                .paid(true)
                .paidTime(now)
                .build());
        existing.setPayments(payments);

        if (existing.getLastPaymentDate() != null && !now.isBefore(existing.getLastPaymentDate())) {
            existing.setStatus(DebtStatus.PASSIVE);
            log.info("Debt {} marked as PASSIVE — lastPaymentDate reached", id);
        }

        final DebtDocument updated = debtRS.update(existing);
        log.info("Payment recorded for debt: {}", id);
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

        if (reminder == null || !reminder.isEnabled()
                || reminder.getReminderDate() == null || reminder.getReminderTime() == null) {
            return;
        }

        debtDocument.setNextReminderDate(
                LocalDateTime.of(reminder.getReminderDate(), reminder.getReminderTime())
        );
    }

    private LocalDateTime getCurrentPeriodStart(final LocalDateTime firstPaymentDate,
                                                final PaymentPeriodType periodType,
                                                final LocalDateTime now) {
        if (firstPaymentDate == null || firstPaymentDate.isAfter(now)) {
            return now;
        }
        return switch (periodType) {
            case DAILY -> {
                final long days = ChronoUnit.DAYS.between(firstPaymentDate, now);
                yield firstPaymentDate.plusDays(days);
            }
            case WEEKLY -> {
                final long weeks = ChronoUnit.WEEKS.between(firstPaymentDate, now);
                yield firstPaymentDate.plusWeeks(weeks);
            }
            case MONTHLY -> {
                final long months = ChronoUnit.MONTHS.between(firstPaymentDate, now);
                yield firstPaymentDate.plusMonths(months);
            }
            case HALF_YEARLY -> {
                final long halfYears = ChronoUnit.MONTHS.between(firstPaymentDate, now) / 6;
                yield firstPaymentDate.plusMonths(halfYears * 6);
            }
            case YEARLY -> {
                final long years = ChronoUnit.YEARS.between(firstPaymentDate, now);
                yield firstPaymentDate.plusYears(years);
            }
        };
    }

    private boolean isPaidCurrentPeriod(final DebtDocument debt) {
        if (debt.getPayments() == null || debt.getPayments().isEmpty()
                || debt.getPaymentPeriodType() == null) {
            return false;
        }
        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime periodStart = getCurrentPeriodStart(
                debt.getFirstPaymentDate(), debt.getPaymentPeriodType(), now);
        return debt.getPayments().stream()
                .anyMatch(p -> p.isPaid() && p.getPaidTime() != null
                        && !p.getPaidTime().isBefore(periodStart));
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