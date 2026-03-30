package dev.batuhanyetgin.authservice.repository;

import dev.batuhanyetgin.authservice.document.SubscriptionDocument;
import dev.batuhanyetgin.commonmodule.repository.BaseRepository;

import java.util.Optional;

public interface SubscriptionRepository extends BaseRepository<SubscriptionDocument, String> {

    Optional<SubscriptionDocument> findByUserId(String userId);

    Optional<SubscriptionDocument> findByOriginalTransactionId(String originalTransactionId);

    Optional<SubscriptionDocument> findByPurchaseToken(String purchaseToken);
}
