package pl.rynek.notification.api;

import java.util.Locale;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pl.rynek.notification.api.dto.CreateNotificationRequest;
import pl.rynek.notification.notifications.Category;
import pl.rynek.notification.notifications.NotificationService;

/** Tylko dla innych serwisów (gateway tego nie wystawia). Zastępuje zdarzenia Kafka, dopóki ich nie ma. */
@RestController
public class InternalNotificationsController {

    private final NotificationService notifications;

    public InternalNotificationsController(NotificationService notifications) {
        this.notifications = notifications;
    }

    /**
     * Tworzy powiadomienie w dzwonku użytkownika. 201: utworzono. 204: pominięto, bo użytkownik wyłączył kategorię
     * albo to samo zdarzenie ({@code dedupeKey}) już było zgłoszone. Wywołujący traktuje oba jako sukces.
     */
    @PostMapping("/internal/notifications")
    public ResponseEntity<Void> create(@Valid @RequestBody CreateNotificationRequest request) {
        boolean created = notifications.create(request.userId(), Category.valueOf(request.category().toUpperCase(Locale.ROOT)), request.title(), request.body(),
                request.link(), request.dedupeKey());
        return ResponseEntity.status(created ? HttpStatus.CREATED : HttpStatus.NO_CONTENT).build();
    }
}
