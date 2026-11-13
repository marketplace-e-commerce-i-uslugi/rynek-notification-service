package pl.rynek.notification.api.dto;

import java.util.UUID;

import pl.rynek.notification.notifications.Device;

/** Kształt {@code DeviceRegistration} z rynek-frontend. */
public record DeviceResponse(UUID id, String platform) {

    public static DeviceResponse of(Device device) {
        return new DeviceResponse(device.getId(), device.getPlatform());
    }
}
