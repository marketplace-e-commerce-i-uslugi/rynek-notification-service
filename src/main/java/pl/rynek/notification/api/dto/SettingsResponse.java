package pl.rynek.notification.api.dto;

import pl.rynek.notification.notifications.Settings;

/** Kształt {@code NotificationSettings} z rynek-frontend. */
public record SettingsResponse(boolean push, boolean email, Categories categories) {

    public record Categories(boolean messages, boolean orders, boolean bookings, boolean followed) {
    }

    public static SettingsResponse of(Settings s) {
        return new SettingsResponse(s.isPush(), s.isEmail(),
                new Categories(s.isMessages(), s.isOrders(), s.isBookings(), s.isFollowed()));
    }
}
