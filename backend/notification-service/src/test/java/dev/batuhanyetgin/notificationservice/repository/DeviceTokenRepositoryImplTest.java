package dev.batuhanyetgin.notificationservice.repository;

import dev.batuhanyetgin.notificationservice.document.DeviceTokenDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeviceTokenRepositoryImplTest {

    @Mock private MongoTemplate mongoTemplate;
    @InjectMocks private DeviceTokenRepositoryImpl repository;

    private org.bson.Document upsertedSet(final String email) {
        repository.upsertByUserId("u1", "fcm", "tr", email);
        final ArgumentCaptor<UpdateDefinition> update = ArgumentCaptor.forClass(UpdateDefinition.class);
        verify(mongoTemplate).upsert(any(Query.class), update.capture(), eq(DeviceTokenDocument.class));
        return (org.bson.Document) update.getValue().getUpdateObject().get("$set");
    }

    @Test
    @DisplayName("upsert without email → email field is left untouched")
    void withoutEmail_doesNotSetEmail() {
        assertThat(upsertedSet(null)).containsEntry("fcmToken", "fcm").doesNotContainKey("email");
    }

    @Test
    @DisplayName("upsert with email → email is written too")
    void withEmail_setsEmail() {
        assertThat(upsertedSet("a@b.co")).containsEntry("email", "a@b.co");
    }
}
