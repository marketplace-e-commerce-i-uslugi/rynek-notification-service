package pl.rynek.notification.api;

import java.util.UUID;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.rynek.notification.api.dto.CursorPage;
import pl.rynek.notification.api.dto.DeviceResponse;
import pl.rynek.notification.api.dto.NotificationResponse;
import pl.rynek.notification.api.dto.RegisterDeviceRequest;
import pl.rynek.notification.api.dto.SetSettingsRequest;
import pl.rynek.notification.api.dto.SettingsResponse;
import pl.rynek.notification.notifications.NotificationService;

/** Endpointy aplikacji klienckich: dzwonek, ustawienia i urządzenia push. Dostęp: user (każdy widzi tylko swoje). */
@RestController
public class NotificationsController {

    private final NotificationService notifications;

    public NotificationsController(NotificationService notifications) {
        this.notifications = notifications;
    }

    /** Rejestracja tokenu push (FCM/APNs). Ten sam token drugi raz zwraca to samo urządzenie. */
    @PostMapping("/api/v1/notifications/devices")
    @ResponseStatus(HttpStatus.CREATED)
    public DeviceResponse registerDevice(@AuthenticationPrincipal Jwt token, @Valid @RequestBody RegisterDeviceRequest request) {
        return DeviceResponse.of(notifications.registerDevice(user(token), request.token().trim(), request.platform()));
    }

    /** Wyrejestrowanie urządzenia; cudze albo nieistniejące daje 404. */
    @DeleteMapping("/api/v1/notifications/devices/{deviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unregisterDevice(@AuthenticationPrincipal Jwt token, @PathVariable UUID deviceId) {
        notifications.unregisterDevice(user(token), deviceId);
    }

    /** Lista powiadomień (dzwonek), od najnowszego. */
    @GetMapping("/api/v1/notifications")
    public CursorPage<NotificationResponse> listNotifications(@AuthenticationPrincipal Jwt token, @RequestParam(required = false) String cursor,
                                                              @RequestParam(required = false) Integer limit) {
        UUID user = user(token);
        return Cursors.page(cursor, limit, (offset, n) -> notifications.list(user, offset, n), NotificationResponse::of);
    }

    /** Oznaczenie jako przeczytane; cudze albo nieistniejące daje 404, powtórzenie jest bez skutku. */
    @PostMapping("/api/v1/notifications/{notificationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsRead(@AuthenticationPrincipal Jwt token, @PathVariable UUID notificationId) {
        notifications.markRead(user(token), notificationId);
    }

    /** Oznacza wszystkie powiadomienia jako przeczytane. */
    @PostMapping("/api/v1/notifications/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllAsRead(@AuthenticationPrincipal Jwt token) {
        notifications.markAllRead(user(token));
    }

    /** Ustawienia powiadomień; bez zapisanych obowiązują domyślne (wszystko włączone poza obserwowanymi kanałami). */
    @GetMapping("/api/v1/notifications/settings")
    public SettingsResponse getSettings(@AuthenticationPrincipal Jwt token) {
        return SettingsResponse.of(notifications.settings(user(token)));
    }

    /** Zmiana ustawień (push, e-mail, kategorie). Wyłączona kategoria wstrzymuje tworzenie nowych powiadomień. */
    @PutMapping("/api/v1/notifications/settings")
    public SettingsResponse setSettings(@AuthenticationPrincipal Jwt token, @Valid @RequestBody SetSettingsRequest request) {
        SetSettingsRequest.Categories c = request.categories();
        return SettingsResponse.of(notifications.changeSettings(user(token), request.push(), request.email(),
                c.messages(), c.orders(), c.bookings(), c.followed()));
    }

    private static UUID user(Jwt token) {
        return UUID.fromString(token.getSubject());
    }
}
