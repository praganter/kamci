package dev.batuhanyetgin.notificationservice.repository;

import dev.batuhanyetgin.notificationservice.document.DeviceTokenDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@RequiredArgsConstructor
public class DeviceTokenRepositoryImpl implements DeviceTokenRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public void upsertByUserId(String userId, String fcmToken, String locale, String email) {
        Query query = Query.query(Criteria.where("userId").is(userId));
        Update update = new Update()
                .set("userId", userId)
                .set("fcmToken", fcmToken)
                .set("locale", locale);

        if (email != null) {
            update.set("email", email);
        }

        mongoTemplate.upsert(query, update, DeviceTokenDocument.class);
    }
}
