# rynek-notification-service

`notification-service` — powiadomienia

> Status: **szkielet**. Wszystkie endpointy z tabeli istnieją w kontrolerach (`src/main/java/.../api/`) i zwracają `501 Not Implemented` (RFC 7807). Ciała żądań to puste rekordy w `api/dto/`.

## Uruchomienie

```bash
./mvnw spring-boot:run   # port 8095, zmienna PORT
./mvnw verify            # testy
```

- Swagger UI: http://localhost:8095/swagger-ui.html
- Zdrowie: http://localhost:8095/actuator/health

## Zakres i endpointy

_Skopiowane z `rynek-docs/architektura/rynek-endpointy-mikroserwisy.md`. Od teraz to repozytorium jest właścicielem tej specyfikacji._

Wysyłka reaguje na zdarzenia Kafka (nowa wiadomość, zamówienie, rezerwacja, nowy film obserwowanego kanału). Endpointy tylko dla aplikacji klienckich.

| Metoda | Ścieżka | Opis | Dostęp |
|---|---|---|---|
| POST | `/api/v1/notifications/devices` | Rejestracja tokenu push (FCM/APNs) | user |
| DELETE | `/api/v1/notifications/devices/{deviceId}` | Wyrejestrowanie urządzenia | user |
| GET | `/api/v1/notifications?cursor=` | Lista powiadomień (dzwonek) | user |
| POST | `/api/v1/notifications/{notificationId}/read` | Oznaczenie jako przeczytane | user |
| POST | `/api/v1/notifications/read-all` | Oznacz wszystkie | user |
| GET | `/api/v1/notifications/settings` | Ustawienia powiadomień | user |
| PUT | `/api/v1/notifications/settings` | Zmiana ustawień (push, e-mail, kategorie) | user |

## Zdarzenia Kafka

Wiersze z §17, w których występuje `notification` (publikuje lub słucha).

| Zdarzenie | Publikuje | Słuchają |
|---|---|---|
| `user.registered`, `user.roles-changed` | identity | channel, notification |
| `channel.followed`, `channel.unfollowed` | channel | feed, notification, analytics |
| `question.created`, `question.answered` | engagement | notification |
| `review.created` | engagement | channel (ocena), search, notification |
| `chat.message-sent` | chat | notification |
| `order.created`, `order.paid`, `order.shipped`, `order.completed`, `order.cancelled` | order | catalog (zapas), notification, analytics, engagement |
| `payment.succeeded`, `payment.failed`, `payment.refunded` | payment | order, booking, notification |
| `booking.created`, `booking.confirmed`, `booking.cancelled`, `booking.completed` | booking | notification, analytics, engagement |
| `shipment.status-changed` | shipping | order, notification |

## Dane i zależności

- **Magazyn danych:** PostgreSQL
- **Zależności:** FCM/APNs, dostawca e-mail; zdarzenia Kafka
- **Kontrakty:** `rynek-contracts/openapi/` i `rynek-contracts/events/`
- **Lokalne uruchomienie:** `rynek-infra/local/`

## Kolejność budowy

Etap z §19: 7. `chat-service`, `engagement-service`, `notification-service`.
