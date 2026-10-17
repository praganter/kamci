package dev.batuhanyetgin.notificationservice.service;

import dev.batuhanyetgin.commonmodule.exception.BaseException;
import dev.batuhanyetgin.notificationservice.document.NotificationDocument;
import dev.batuhanyetgin.notificationservice.document.NotificationType;
import dev.batuhanyetgin.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public void persist(final String userId, final NotificationType type, final String title,
                         final String body, final String entityId) {
        notificationRepository.save(NotificationDocument.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .body(body)
                .entityId(entityId)
                .read(false)
                .build());
    }

    public Page<NotificationDocument> list(final String userId, final Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedDateDesc(userId, pageable);
    }

    public void markAsRead(final String id, final String userId) {
        final NotificationDocument notification = notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> BaseException.notFound("error.notification.not_found", id));
        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    public void deleteAllForUser(final String userId) {
        notificationRepository.deleteByUserId(userId);
    }
}
