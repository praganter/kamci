package dev.batuhanyetgin.authservice.repositoryservice;

import dev.batuhanyetgin.authservice.document.SubscriptionDocument;
import dev.batuhanyetgin.authservice.enums.SubscriptionStatus;
import dev.batuhanyetgin.authservice.repository.SubscriptionRepository;
import dev.batuhanyetgin.commonmodule.exception.BaseException;
import dev.batuhanyetgin.commonmodule.repository.BaseRepositoryService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SubscriptionRepositoryService extends BaseRepositoryService<SubscriptionDocument> {

    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionRepositoryService(final SubscriptionRepository subscriptionRepository) {
        super(subscriptionRepository);
        this.subscriptionRepository = subscriptionRepository;
    }

    public Optional<SubscriptionDocument> findByUserId(final String userId) {
        return subscriptionRepository.findByUserId(userId);
    }

    public SubscriptionDocument findByOriginalTransactionId(final String originalTransactionId) {
        return subscriptionRepository.findByOriginalTransactionId(originalTransactionId)
                .orElseThrow(() -> BaseException.notFound("error.subscription.not_found"));
    }

    public SubscriptionDocument findByPurchaseToken(final String purchaseToken) {
        return subscriptionRepository.findByPurchaseToken(purchaseToken)
                .orElseThrow(() -> BaseException.notFound("error.subscription.not_found"));
    }

    public List<SubscriptionDocument> findActiveExpired(final LocalDateTime now) {
        return subscriptionRepository.findByStatusAndExpiryDateBefore(SubscriptionStatus.ACTIVE, now);
    }
}
