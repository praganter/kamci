package dev.batuhanyetgin.notificationservice.repository;

import dev.batuhanyetgin.notificationservice.document.NotificationDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface NotificationRepository extends MongoRepository<NotificationDocument, String> {

    Page<NotificationDocument> findByUserIdOrderByCreatedDateDesc(String userId, Pageable pageable);

    Optional<NotificationDocument> findByIdAndUserId(String id, String userId);
}
