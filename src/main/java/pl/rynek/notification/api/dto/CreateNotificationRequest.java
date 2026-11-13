package pl.rynek.notification.api.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Żądanie innego serwisu (§ {@code /internal}). {@code category} to {@code messages}, {@code orders}, {@code bookings} albo {@code followed}, {@code link} to ścieżka w aplikacji (zaczyna się od {@code /}),
 * {@code dedupeKey} wyróżnia zdarzenie, więc ponowienie nie tworzy drugiego powiadomienia.
 */
public record CreateNotificationRequest(
        @NotNull(message = "Brakuje adresata powiadomienia.") UUID userId,
        @NotNull(message = "Brakuje kategorii powiadomienia.")
        @Pattern(regexp = "messages|orders|bookings|followed", message = "Nieznana kategoria powiadomienia.") String category,
        @NotBlank(message = "Brakuje tytułu powiadomienia.") @Size(max = 120, message = "Tytuł jest za długi.") String title,
        @NotBlank(message = "Brakuje treści powiadomienia.") @Size(max = 500, message = "Treść jest za długa.") String body,
        @Size(max = 200, message = "Link jest za długi.") @Pattern(regexp = "^/\\S*$", message = "Link musi być ścieżką w aplikacji.") String link,
        @Size(max = 100, message = "Klucz zdarzenia jest za długi.") String dedupeKey) {
}
