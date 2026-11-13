package pl.rynek.notification.notifications;

import java.util.Locale;

/** Kategorie z ustawień powiadomień; w bazie i w API zapisywane małymi literami. */
public enum Category {
    MESSAGES, ORDERS, BOOKINGS, FOLLOWED;

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
