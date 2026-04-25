package dev.batuhanyetgin.notificationservice.repository;

import dev.batuhanyetgin.notificationservice.document.DeviceTokenDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends MongoRepository<DeviceTokenDocument, String>, DeviceTokenRepositoryCustom {

    Optional<DeviceTokenDocument> findByUserId(String userId);

    List<DeviceTokenDocument> findByUserIdIn(List<String> userIds);

    void deleteByUserId(String userId);
}
