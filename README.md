# rynek-notification-service

`notification-service` — powiadomienia

> Status: **dzwonek, ustawienia i rejestr urządzeń działają** (PostgreSQL + Flyway, JWT z JWKS identity). Powiadomienia tworzą inne serwisy przez `POST /internal/notifications` (order, booking i engagement), bo Kafki jeszcze nie ma. **Wysyłka push i e-mail nie jest podłączona**: tokeny urządzeń są tylko zapisywane (brak FCM/APNs i dostawcy e-mail), a pola `push` i `email` w ustawieniach są zapamiętywane na przyszłość.

## Uruchomienie

```bash
./mvnw spring-boot:run   # port 8095, zmienna PORT
./mvnw verify            # testy
```

- Swagger UI: http://localhost:8095/swagger-ui.html
- Zdrowie: http://localhost:8095/actuator/health

## Zakres i endpointy

_Skopiowane z `rynek-docs/architektura/rynek-endpointy-mikroserwisy.md`. Od teraz to repozytorium jest właścicielem tej specyfikacji._

Docelowo wysyłka reaguje na zdarzenia Kafka (nowa wiadomość, zamówienie, rezerwacja, nowy film obserwowanego kanału). Do czasu Kafki serwisy wołają `POST /internal/notifications`. Brakuje producentów: chat (wiadomości), channel/feed (nowy film obserwowanego kanału) i payment (np. zwrot po anulowaniu); kategorie `messages` i `followed` są więc w tej chwili zasilane tylko pytaniami z engagement.

| Metoda | Ścieżka | Opis | Dostęp |
|---|---|---|---|
| POST | `/api/v1/notifications/devices` | Rejestracja tokenu push (FCM/APNs) | user |
| DELETE | `/api/v1/notifications/devices/{deviceId}` | Wyrejestrowanie urządzenia | user |
| GET | `/api/v1/notifications?cursor=` | Lista powiadomień (dzwonek) | user |
| POST | `/api/v1/notifications/{notificationId}/read` | Oznaczenie jako przeczytane | user |
| POST | `/api/v1/notifications/read-all` | Oznacz wszystkie | user |
| GET | `/api/v1/notifications/settings` | Ustawienia powiadomień | user |
| PUT | `/api/v1/notifications/settings` | Zmiana ustawień (push, e-mail, kategorie) | user |
| POST | `/internal/notifications` | Utworzenie powiadomienia: `{userId, category, title, body, link?, dedupeKey?}` | int |

## Działanie

- **Dzwonek:** `GET /notifications` zwraca `{items, nextCursor}` od najnowszego (`id, title, body, createdAt, read, link`). Odczyt i oznaczanie (`/{id}/read`, `/read-all`, oba 204) dotyczą tylko własnych powiadomień; cudze lub nieistniejące daje 404.
- **Tworzenie:** `POST /internal/notifications` (bez tokenu, gateway go nie wystawia) odpowiada 201 po utworzeniu albo 204, gdy pominięto, bo użytkownik wyłączył kategorię albo `dedupeKey` został już użyty u tego użytkownika (ponowienie producenta nie dubluje powiadomienia). `category`: `messages`, `orders`, `bookings`, `followed`. `link` to ścieżka w aplikacji (zaczyna się od `/`), inne adresy dają 400.
- **Ustawienia:** bez zapisu obowiązują domyślne: push, e-mail, wiadomości, zamówienia i rezerwacje włączone, obserwowane kanały wyłączone. Wyłączona kategoria wstrzymuje tworzenie nowych powiadomień tej kategorii (istniejące zostają).
- **Urządzenia:** `POST /devices {token, platform?}` (`WEB` domyślnie, `ANDROID`, `IOS`) zwraca 201 z `{id, platform}`; ten sam token drugi raz to to samo urządzenie, a token z innego konta przechodzi na zalogowane konto. `DELETE /devices/{id}` usuwa tylko własne urządzenie (inaczej 404).
- **Producenci** wysyłają w tle i bez blokowania własnej operacji (trzy próby co 2 s); jeśli notification-service nie działa, powiadomienie przepada. Trwałe ponawianie wymaga Kafki albo kolejki zdarzeń.
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

- **Magazyn danych:** PostgreSQL (baza `notifications`, tabele `notifications`, `devices`, `notification_settings`)
- **Zależności:** identity-service (klucze JWKS); docelowo FCM/APNs, dostawca e-mail i zdarzenia Kafka
- **Kontrakty:** `rynek-contracts/openapi/` i `rynek-contracts/events/`
- **Lokalne uruchomienie:** `rynek-infra/local/`

## Kolejność budowy

Etap z §19: 7. `chat-service`, `engagement-service`, `notification-service`.
