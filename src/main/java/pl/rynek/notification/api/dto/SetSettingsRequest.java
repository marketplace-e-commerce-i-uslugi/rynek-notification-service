package pl.rynek.notification.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Kształt {@code NotificationSettings} z rynek-frontend: push, e-mail i cztery kategorie. */
public record SetSettingsRequest(
        @NotNull(message = "Wybierz, czy chcesz powiadomienia push.") Boolean push,
        @NotNull(message = "Wybierz, czy chcesz powiadomienia e-mail.") Boolean email,
        @NotNull(message = "Wybierz kategorie powiadomień.") @Valid Categories categories) {

    public record Categories(
            @NotNull(message = "Wybierz kategorię wiadomości.") Boolean messages,
            @NotNull(message = "Wybierz kategorię zamówień.") Boolean orders,
            @NotNull(message = "Wybierz kategorię rezerwacji.") Boolean bookings,
            @NotNull(message = "Wybierz kategorię obserwowanych kanałów.") Boolean followed) {
    }
}
