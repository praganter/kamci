package dev.batuhanyetgin.notificationservice.repository;

public interface DeviceTokenRepositoryCustom {

    void upsertByUserId(String userId, String fcmToken, String locale, String email);
}
