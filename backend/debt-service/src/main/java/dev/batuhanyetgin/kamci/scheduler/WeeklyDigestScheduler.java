package dev.batuhanyetgin.kamci.scheduler;

import dev.batuhanyetgin.commonmodule.event.RabbitMQConstants;
import dev.batuhanyetgin.commonmodule.event.WeeklyDigestEvent;
import dev.batuhanyetgin.commonmodule.event.WeeklyDigestItem;
import dev.batuhanyetgin.kamci.document.DebtDocument;
import dev.batuhanyetgin.kamci.enums.PaymentPeriodType;
import dev.batuhanyetgin.kamci.producer.DebtEventProducer;
import dev.batuhanyetgin.kamci.repositoryservice.DebtRepositoryService;
import dev.batuhanyetgin.kamci.util.PaymentPeriodUtil;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Weekly "what's due" digest — Monday 09:00 local time.
 * Assumption: a single fixed weekly cadence is enough for v1 (no per-user schedule
 * preference exists yet); Monday morning was picked so the digest lands before the
 * work week's bill-paying rather than mid-week or on a weekend.
 */
@Slf4j
@Component
public class WeeklyDigestScheduler {

    private final DebtRepositoryService debtRS;
    private final DebtEventProducer debtEventProducer;
    private final Clock clock;

    @Autowired
    public WeeklyDigestScheduler(final DebtRepositoryService debtRS,
                                 final DebtEventProducer debtEventProducer) {
        this(debtRS, debtEventProducer, Clock.systemDefaultZone());
    }

    WeeklyDigestScheduler(final DebtRepositoryService debtRS,
                          final DebtEventProducer debtEventProducer,
                          final Clock clock) {
        this.debtRS = debtRS;
        this.debtEventProducer = debtEventProducer;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 9 * * MON")
    @SchedulerLock(name = "weeklyDigestScheduler", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    public void processWeeklyDigest() {
        final LocalDateTime now = LocalDateTime.now(clock);
        final LocalDateTime weekEnd = now.plusDays(7);
        log.info("Processing weekly digest at: {} for window up to {}", now, weekEnd);

        final Map<String, List<WeeklyDigestItem>> itemsByOwner = debtRS.findActiveDebts().stream()
                .map(debt -> toDigestItem(debt, now, weekEnd))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.groupingBy(
                        entry -> entry.ownerId,
                        Collectors.mapping(entry -> entry.item, Collectors.toList())));

        itemsByOwner.forEach((ownerId, items) -> produceDigestEvent(ownerId, items, now, weekEnd));
        log.info("Weekly digest complete — {} owner(s) notified", itemsByOwner.size());
    }

    private OwnerDigestItem toDigestItem(final DebtDocument debt, final LocalDateTime now, final LocalDateTime weekEnd) {
        final LocalDateTime dueDate = computeDueDate(debt, now);
        if (dueDate == null || dueDate.isBefore(now) || dueDate.isAfter(weekEnd)) {
            return null;
        }
        if (!isCurrentPeriodUnpaid(debt, now)) {
            return null;
        }
        return new OwnerDigestItem(debt.getOwnerId(), WeeklyDigestItem.builder()
                .debtId(debt.getId())
                .debtName(debt.getName())
                .amount(debt.getAmount())
                .dueDate(dueDate)
                .build());
    }

    private LocalDateTime computeDueDate(final DebtDocument debt, final LocalDateTime now) {
        if (debt.getFirstPaymentDate() == null) {
            return null;
        }
        if (debt.getPaymentPeriodType() == PaymentPeriodType.ONE_TIME) {
            return debt.getFirstPaymentDate();
        }
        final LocalDateTime periodStart = PaymentPeriodUtil.getPeriodStart(
                debt.getFirstPaymentDate(), debt.getPaymentPeriodType(), now);
        final int originalDay = debt.getFirstPaymentDate().getDayOfMonth();
        return PaymentPeriodUtil.getPeriodEnd(periodStart, debt.getPaymentPeriodType(), originalDay);
    }

    private boolean isCurrentPeriodUnpaid(final DebtDocument debt, final LocalDateTime now) {
        if (debt.getPayments() == null || debt.getPayments().isEmpty()) {
            return true;
        }
        final LocalDateTime periodStart = PaymentPeriodUtil.getPeriodStart(
                debt.getFirstPaymentDate(), debt.getPaymentPeriodType(), now);
        return debt.getPayments().stream()
                .noneMatch(p -> p.isPaid() && p.getPaidTime() != null && !p.getPaidTime().isBefore(periodStart));
    }

    private void produceDigestEvent(final String ownerId, final List<WeeklyDigestItem> items,
                                    final LocalDateTime weekStart, final LocalDateTime weekEnd) {
        final List<WeeklyDigestItem> sorted = items.stream()
                .sorted(Comparator.comparing(WeeklyDigestItem::getDueDate))
                .toList();
        final BigDecimal total = sorted.stream()
                .map(WeeklyDigestItem::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        try {
            debtEventProducer.produce(
                    RabbitMQConstants.NOTIFICATION_EXCHANGE,
                    RabbitMQConstants.WEEKLY_DIGEST_ROUTING_KEY,
                    WeeklyDigestEvent.builder()
                            .ownerId(ownerId)
                            .weekStart(weekStart)
                            .weekEnd(weekEnd)
                            .totalAmount(total)
                            .items(sorted)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to produce weekly digest event for ownerId {}: {}", ownerId, e.getMessage(), e);
        }
    }

    private record OwnerDigestItem(String ownerId, WeeklyDigestItem item) {
    }
}
