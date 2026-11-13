-- Powiadomienia z dzwonka. Tworzą je inne serwisy przez POST /internal/notifications (bez Kafki).
-- dedupe_key: to samo zdarzenie zgłoszone drugi raz (ponowienie) nie tworzy drugiego powiadomienia.
CREATE TABLE notifications (
    id         uuid PRIMARY KEY,
    user_id    uuid         NOT NULL,
    category   varchar(10)  NOT NULL CHECK (category IN ('messages', 'orders', 'bookings', 'followed')),
    title      varchar(120) NOT NULL,
    body       varchar(500) NOT NULL,
    link       varchar(200),
    dedupe_key varchar(100),
    created_at timestamptz  NOT NULL,
    read_at    timestamptz
);

CREATE INDEX notifications_user_idx ON notifications (user_id, created_at DESC, id DESC);
CREATE UNIQUE INDEX notifications_dedupe_idx ON notifications (user_id, dedupe_key) WHERE dedupe_key IS NOT NULL;

-- Tokeny push (FCM/APNs/web). Wysyłka push i e-mail nie jest jeszcze podłączona, więc to tylko rejestr urządzeń.
CREATE TABLE devices (
    id         uuid PRIMARY KEY,
    user_id    uuid         NOT NULL,
    token      varchar(500) NOT NULL UNIQUE,
    platform   varchar(10)  NOT NULL CHECK (platform IN ('WEB', 'ANDROID', 'IOS')),
    created_at timestamptz  NOT NULL
);

CREATE INDEX devices_user_idx ON devices (user_id);

-- Brak wiersza oznacza ustawienia domyślne (wszystko włączone poza „followed”).
CREATE TABLE notification_settings (
    user_id    uuid PRIMARY KEY,
    push       boolean     NOT NULL,
    email      boolean     NOT NULL,
    messages   boolean     NOT NULL,
    orders     boolean     NOT NULL,
    bookings   boolean     NOT NULL,
    followed   boolean     NOT NULL,
    updated_at timestamptz NOT NULL
);
