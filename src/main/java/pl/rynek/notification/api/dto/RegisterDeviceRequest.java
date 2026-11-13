package pl.rynek.notification.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** {@code platform} pominięta oznacza {@code WEB}. */
public record RegisterDeviceRequest(
        @NotBlank(message = "Brakuje tokenu urządzenia.") @Size(max = 500, message = "Token urządzenia jest za długi.") String token,
        @Pattern(regexp = "WEB|ANDROID|IOS", message = "Platforma musi mieć wartość WEB, ANDROID albo IOS.") String platform) {
}
