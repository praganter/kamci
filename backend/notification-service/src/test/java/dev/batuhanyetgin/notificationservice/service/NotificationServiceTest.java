package dev.batuhanyetgin.notificationservice.service;

import dev.batuhanyetgin.commonmodule.exception.BaseException;
import dev.batuhanyetgin.notificationservice.document.NotificationDocument;
import dev.batuhanyetgin.notificationservice.document.NotificationType;
import dev.batuhanyetgin.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private static final String USER_ID = "user123";
    private static final String NOTIFICATION_ID = "notif123";

    @Nested
    @DisplayName("persist")
    class Persist {

        @Test
        @DisplayName("saves a new unread notification")
        void savesNewUnreadNotification() {
            notificationService.persist(USER_ID, NotificationType.DEBT_REMINDER, "Title", "Body", "entity1");

            final ArgumentCaptor<NotificationDocument> captor = ArgumentCaptor.forClass(NotificationDocument.class);
            verify(notificationRepository).save(captor.capture());

            final NotificationDocument saved = captor.getValue();
            assertThat(saved.getUserId()).isEqualTo(USER_ID);
            assertThat(saved.getType()).isEqualTo(NotificationType.DEBT_REMINDER);
            assertThat(saved.getTitle()).isEqualTo("Title");
            assertThat(saved.getBody()).isEqualTo("Body");
            assertThat(saved.getEntityId()).isEqualTo("entity1");
            assertThat(saved.isRead()).isFalse();
        }
    }

    @Nested
    @DisplayName("list")
    class ListNotifications {

        @Test
        @DisplayName("delegates to repository with pageable")
        void delegatesToRepository() {
            final var pageable = PageRequest.of(0, 20);
            when(notificationRepository.findByUserIdOrderByCreatedDateDesc(USER_ID, pageable))
                    .thenReturn(new PageImpl<>(java.util.List.of()));

            final var result = notificationService.list(USER_ID, pageable);

            assertThat(result.getContent()).isEmpty();
            verify(notificationRepository).findByUserIdOrderByCreatedDateDesc(USER_ID, pageable);
        }
    }

    @Nested
    @DisplayName("markAsRead")
    class MarkAsRead {

        @Test
        @DisplayName("unread notification → sets read=true and saves")
        void unreadNotification_setsReadAndSaves() {
            final NotificationDocument doc = NotificationDocument.builder()
                    .userId(USER_ID).read(false).build();
            when(notificationRepository.findByIdAndUserId(NOTIFICATION_ID, USER_ID))
                    .thenReturn(Optional.of(doc));

            notificationService.markAsRead(NOTIFICATION_ID, USER_ID);

            assertThat(doc.isRead()).isTrue();
            verify(notificationRepository).save(doc);
        }

        @Test
        @DisplayName("already read notification → no-op save")
        void alreadyRead_noSave() {
            final NotificationDocument doc = NotificationDocument.builder()
                    .userId(USER_ID).read(true).build();
            when(notificationRepository.findByIdAndUserId(NOTIFICATION_ID, USER_ID))
                    .thenReturn(Optional.of(doc));

            notificationService.markAsRead(NOTIFICATION_ID, USER_ID);

            verify(notificationRepository, never()).save(any());
        }

        @Test
        @DisplayName("not found → throws BaseException")
        void notFound_throwsBaseException() {
            when(notificationRepository.findByIdAndUserId(NOTIFICATION_ID, USER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.markAsRead(NOTIFICATION_ID, USER_ID))
                    .isInstanceOf(BaseException.class);
        }
    }

    @Nested
    @DisplayName("deleteAllForUser")
    class DeleteAllForUser {

        @Test
        @DisplayName("delegates to repository deleteByUserId")
        void delegatesToRepository() {
            notificationService.deleteAllForUser(USER_ID);

            verify(notificationRepository).deleteByUserId(USER_ID);
        }
    }
}
