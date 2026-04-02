package dev.batuhanyetgin.authservice.scheduler;

import dev.batuhanyetgin.authservice.document.SubscriptionDocument;
import dev.batuhanyetgin.authservice.enums.SubscriptionStatus;
import dev.batuhanyetgin.authservice.repositoryservice.AuthRepositoryService;
import dev.batuhanyetgin.authservice.repositoryservice.SubscriptionRepositoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionExpiryScheduler {

    private final SubscriptionRepositoryService subscriptionRS;
    private final AuthRepositoryService authRS;

    @Scheduled(cron = "0 0 2 * * *")
    @SchedulerLock(
            name = "subscriptionExpiryScheduler",
            lockAtMostFor = "PT30M",
            lockAtLeastFor = "PT5M"
    )
    public void expireSubscriptions() {
        final LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        log.info("Running subscription expiry check at: {}", now);

        final List<SubscriptionDocument> expired = subscriptionRS.findActiveExpired(now);
        if (expired.isEmpty()) {
            log.info("No expired subscriptions found");
            return;
        }

        log.info("Found {} subscription(s) to expire", expired.size());

        int success = 0;
        int failure = 0;

        for (final SubscriptionDocument sub : expired) {
            try {
                sub.setStatus(SubscriptionStatus.EXPIRED);
                subscriptionRS.save(sub);

                final var auth = authRS.findById(sub.getUserId());
                auth.setPremium(false);
                authRS.save(auth);

                log.info("Expired subscription for userId: {}", sub.getUserId());
                success++;
            } catch (Exception e) {
                log.error("Failed to expire subscription for userId: {}, error: {}",
                        sub.getUserId(), e.getMessage(), e);
                failure++;
            }
        }

        log.info("Subscription expiry batch complete — success: {}, failed: {}", success, failure);
    }
}
