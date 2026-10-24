package pl.rynek.notification.api;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.rynek.notification.api.dto.RegisterDeviceRequest;
import pl.rynek.notification.api.dto.SetSettingsRequest;

import static pl.rynek.notification.common.NotImplemented.notImplemented;

@RestController
public class NotificationsController {

    /**
     * Rejestracja tokenu push (FCM/APNs). Dostęp: user.
     */
    @PostMapping("/api/v1/notifications/devices")
    public void registerDevice(@RequestBody RegisterDeviceRequest request) {
        throw notImplemented();
    }

    /**
     * Wyrejestrowanie urządzenia. Dostęp: user.
     */
    @DeleteMapping("/api/v1/notifications/devices/{deviceId}")
    public void unregisterDevice(@PathVariable String deviceId) {
        throw notImplemented();
    }

    /**
     * Lista powiadomień (dzwonek). Dostęp: user.
     */
    @GetMapping("/api/v1/notifications")
    public void listNotifications(@RequestParam(required = false) String cursor) {
        throw notImplemented();
    }

    /**
     * Oznaczenie jako przeczytane. Dostęp: user.
     */
    @PostMapping("/api/v1/notifications/{notificationId}/read")
    public void markAsRead(@PathVariable String notificationId) {
        throw notImplemented();
    }

    /**
     * Oznacz wszystkie. Dostęp: user.
     */
    @PostMapping("/api/v1/notifications/read-all")
    public void markAllAsRead() {
        throw notImplemented();
    }

    /**
     * Ustawienia powiadomień. Dostęp: user.
     */
    @GetMapping("/api/v1/notifications/settings")
    public void getSettings() {
        throw notImplemented();
    }

    /**
     * Zmiana ustawień (push, e-mail, kategorie). Dostęp: user.
     */
    @PutMapping("/api/v1/notifications/settings")
    public void setSettings(@RequestBody SetSettingsRequest request) {
        throw notImplemented();
    }
}
