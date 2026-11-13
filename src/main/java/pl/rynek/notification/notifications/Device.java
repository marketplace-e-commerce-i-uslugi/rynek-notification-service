package pl.rynek.notification.notifications;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Token push zarejestrowany przez użytkownika. Ten sam token po zalogowaniu na inne konto przechodzi na to konto. */
@Entity
@Table(name = "devices")
public class Device {

    @Id
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    private String token;
    private String platform;

    @Column(name = "created_at")
    private Instant createdAt;

    protected Device() {
    }

    public Device(UUID userId, String token, String platform, Instant now) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.token = token;
        this.platform = platform;
        this.createdAt = now;
    }

    public void moveTo(UUID userId, String platform) {
        this.userId = userId;
        this.platform = platform;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getPlatform() {
        return platform;
    }
}
