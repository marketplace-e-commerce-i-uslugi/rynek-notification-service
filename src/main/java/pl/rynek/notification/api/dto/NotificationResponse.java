package pl.rynek.notification.api.dto;

import java.time.Instant;
import java.util.UUID;

import pl.rynek.notification.notifications.Notification;

/** Kształt {@code AppNotification} z rynek-frontend. */
public record NotificationResponse(UUID id, String title, String body, Instant createdAt, boolean read, String link) {

    public static NotificationResponse of(Notification n) {
        return new NotificationResponse(n.getId(), n.getTitle(), n.getBody(), n.getCreatedAt(), n.isRead(), n.getLink());
    }
}
