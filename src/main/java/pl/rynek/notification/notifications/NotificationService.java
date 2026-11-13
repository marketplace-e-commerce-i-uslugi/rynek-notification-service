package pl.rynek.notification.notifications;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotificationService {

    private final NotificationRepository notifications;
    private final DeviceRepository devices;
    private final SettingsRepository settings;
    private final Clock clock;

    public NotificationService(NotificationRepository notifications, DeviceRepository devices, SettingsRepository settings, Clock clock) {
        this.notifications = notifications;
        this.devices = devices;
        this.settings = settings;
        this.clock = clock;
    }

    /**
     * Tworzy powiadomienie dla użytkownika. {@code false}: nie utworzono, bo użytkownik wyłączył kategorię albo to samo
     * zdarzenie ({@code dedupeKey}) już zgłoszono. Wywołują inne serwisy przez {@code /internal/notifications}.
     */
    @Transactional
    public boolean create(UUID userId, Category category, String title, String body, String link, String dedupeKey) {
        if (!settings(userId).allows(category)) {
            return false;
        }
        String key = dedupeKey == null || dedupeKey.isBlank() ? null : dedupeKey;
        return notifications.add(UUID.randomUUID(), userId, category.key(), title.trim(), body.trim(), link, key, clock.instant()) == 1;
    }

    @Transactional(readOnly = true)
    public List<Notification> list(UUID userId, int offset, int limit) {
        return notifications.page(userId, offset, limit);
    }

    /** 404 także dla cudzego powiadomienia; ponowne przeczytanie niczego nie zmienia. */
    @Transactional
    public void markRead(UUID userId, UUID id) {
        if (!notifications.owned(id, userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma takiego powiadomienia.");
        }
        notifications.markRead(id, userId, clock.instant());
    }

    @Transactional
    public void markAllRead(UUID userId) {
        notifications.markAllRead(userId, clock.instant());
    }

    @Transactional(readOnly = true)
    public Settings settings(UUID userId) {
        return settings.findById(userId).orElseGet(() -> Settings.defaults(userId, clock.instant()));
    }

    @Transactional
    public Settings changeSettings(UUID userId, boolean push, boolean email, boolean messages, boolean orders, boolean bookings,
                                   boolean followed) {
        return settings.saveAndFlush(new Settings(userId, push, email, messages, orders, bookings, followed, clock.instant()));
    }

    /** Ten sam token drugi raz to to samo urządzenie; token z innego konta przechodzi na to konto. */
    @Transactional
    public Device registerDevice(UUID userId, String token, String platform) {
        String name = platform == null || platform.isBlank() ? "WEB" : platform;
        Device device = devices.findByToken(token).orElse(null);
        if (device == null) {
            return devices.saveAndFlush(new Device(userId, token, name, clock.instant()));
        }
        device.moveTo(userId, name);
        return devices.saveAndFlush(device);
    }

    @Transactional
    public void unregisterDevice(UUID userId, UUID deviceId) {
        if (devices.remove(deviceId, userId) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Nie ma takiego urządzenia.");
        }
    }
}
