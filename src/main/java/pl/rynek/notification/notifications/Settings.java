package pl.rynek.notification.notifications;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Ustawienia powiadomień użytkownika. Bez wiersza obowiązują {@link #defaults}. */
@Entity
@Table(name = "notification_settings")
public class Settings {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    private boolean push;
    private boolean email;
    private boolean messages;
    private boolean orders;
    private boolean bookings;
    private boolean followed;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected Settings() {
    }

    public Settings(UUID userId, boolean push, boolean email, boolean messages, boolean orders, boolean bookings,
                    boolean followed, Instant now) {
        this.userId = userId;
        this.push = push;
        this.email = email;
        this.messages = messages;
        this.orders = orders;
        this.bookings = bookings;
        this.followed = followed;
        this.updatedAt = now;
    }

    /** Wszystko włączone poza powiadomieniami o filmach obserwowanych kanałów. */
    public static Settings defaults(UUID userId, Instant now) {
        return new Settings(userId, true, true, true, true, true, false, now);
    }

    public boolean allows(Category category) {
        return switch (category) {
            case MESSAGES -> messages;
            case ORDERS -> orders;
            case BOOKINGS -> bookings;
            case FOLLOWED -> followed;
        };
    }

    public boolean isPush() {
        return push;
    }

    public boolean isEmail() {
        return email;
    }

    public boolean isMessages() {
        return messages;
    }

    public boolean isOrders() {
        return orders;
    }

    public boolean isBookings() {
        return bookings;
    }

    public boolean isFollowed() {
        return followed;
    }
}
