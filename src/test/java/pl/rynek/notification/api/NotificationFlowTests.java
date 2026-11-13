package pl.rynek.notification.api;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import pl.rynek.notification.TestcontainersConfig;

/** Dzwonek, ustawienia i urządzenia na prawdziwym PostgreSQL. Tokeny testowe mają postać {@code user-<uuid>}. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class NotificationFlowTests {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @BeforeEach
    void stubs() {
        given(jwtDecoder.decode(anyString())).willAnswer(call -> {
            String token = call.getArgument(0);
            if (!token.startsWith("user-")) {
                throw new BadJwtException("Nieważny token");
            }
            return Jwt.withTokenValue(token).header("alg", "RS256").subject(token.substring(5))
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(900)).build();
        });
    }

    @Test
    void appEndpointsNeedLoginButInternalOnesDoNot() throws Exception {
        mvc.perform(get("/api/v1/notifications")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/notifications/settings")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/notifications/read-all")).andExpect(status().isUnauthorized());
        create(UUID.randomUUID(), "orders", "Nowe zamówienie", "Zamówienie #1 czeka na wysyłkę.", "/sprzedawca/zamowienia", "t-1")
                .andExpect(status().isCreated());
    }

    @Test
    void createdNotificationAppearsOnlyForItsOwnerAndIsDeduplicated() throws Exception {
        UUID user = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        create(user, "orders", "Zamówienie #1", "Nowe zamówienie do wysłania.", "/sprzedawca/zamowienia", "order-1-created").andExpect(status().isCreated());
        // To samo zdarzenie zgłoszone drugi raz (ponowienie) nie tworzy drugiego powiadomienia.
        create(user, "orders", "Zamówienie #1", "Nowe zamówienie do wysłania.", "/sprzedawca/zamowienia", "order-1-created").andExpect(status().isNoContent());
        // Ten sam klucz u innego użytkownika to inne zdarzenie.
        create(other, "orders", "Zamówienie #1", "Nowe zamówienie do wysłania.", null, "order-1-created").andExpect(status().isCreated());

        list(user, "").andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].title").value("Zamówienie #1"))
                .andExpect(jsonPath("$.items[0].body").value("Nowe zamówienie do wysłania."))
                .andExpect(jsonPath("$.items[0].read").value(false))
                .andExpect(jsonPath("$.items[0].link").value("/sprzedawca/zamowienia"))
                .andExpect(jsonPath("$.items[0].createdAt").exists())
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
        list(other, "").andExpect(jsonPath("$.items", hasSize(1))).andExpect(jsonPath("$.items[0].link").doesNotExist());
        list(UUID.randomUUID(), "").andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void internalRequestIsValidated() throws Exception {
        UUID user = UUID.randomUUID();
        create(user, "orders", "", "Treść", null, null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Brakuje tytułu powiadomienia."));
        create(user, "orders", "Tytuł", "Treść", "https://example.com/phishing", null).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Link musi być ścieżką w aplikacji."));
        create(user, "nieznana", "Tytuł", "Treść", null, null).andExpect(status().isBadRequest());
        create(user, "orders", "x".repeat(121), "Treść", null, null).andExpect(status().isBadRequest());
        mvc.perform(post("/internal/notifications").contentType(MediaType.APPLICATION_JSON)
                .content("{\"category\":\"orders\",\"title\":\"T\",\"body\":\"B\"}")).andExpect(status().isBadRequest());
        list(user, "").andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void listIsPagedFromNewestWithCursor() throws Exception {
        UUID user = UUID.randomUUID();
        for (int i = 1; i <= 3; i++) {
            create(user, "messages", "Wiadomość " + i, "Treść " + i, "/wiadomosci", "m-" + i).andExpect(status().isCreated());
        }
        String first = list(user, "?limit=2").andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].title").value("Wiadomość 3"))
                .andExpect(jsonPath("$.items[1].title").value("Wiadomość 2"))
                .andReturn().getResponse().getContentAsString();
        String cursor = JsonPath.read(first, "$.nextCursor");
        list(user, "?limit=2&cursor=" + cursor).andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].title").value("Wiadomość 1"))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
        list(user, "?cursor=zly").andExpect(status().isBadRequest());
    }

    @Test
    void readingOneOrAllAndOwnership() throws Exception {
        UUID user = UUID.randomUUID();
        UUID stranger = UUID.randomUUID();
        for (int i = 1; i <= 3; i++) {
            create(user, "bookings", "Rezerwacja " + i, "Treść", "/profil/rezerwacje", "b-" + i).andExpect(status().isCreated());
        }
        String id = JsonPath.read(list(user, "").andReturn().getResponse().getContentAsString(), "$.items[0].id");

        mvc.perform(post("/api/v1/notifications/" + id + "/read").header(HttpHeaders.AUTHORIZATION, bearer(stranger))).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/notifications/" + UUID.randomUUID() + "/read").header(HttpHeaders.AUTHORIZATION, bearer(user))).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/notifications/" + id + "/read").header(HttpHeaders.AUTHORIZATION, bearer(user))).andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/notifications/" + id + "/read").header(HttpHeaders.AUTHORIZATION, bearer(user))).andExpect(status().isNoContent());
        list(user, "").andExpect(jsonPath("$.items[0].read").value(true))
                .andExpect(jsonPath("$.items[1].read").value(false)).andExpect(jsonPath("$.items[2].read").value(false));

        mvc.perform(post("/api/v1/notifications/read-all").header(HttpHeaders.AUTHORIZATION, bearer(user))).andExpect(status().isNoContent());
        list(user, "").andExpect(jsonPath("$.items[?(@.read == false)]", hasSize(0)));
        // Cudze powiadomienia zostają nietknięte.
        create(stranger, "bookings", "Rezerwacja", "Treść", null, "s-1").andExpect(status().isCreated());
        mvc.perform(post("/api/v1/notifications/read-all").header(HttpHeaders.AUTHORIZATION, bearer(user))).andExpect(status().isNoContent());
        list(stranger, "").andExpect(jsonPath("$.items[0].read").value(false));
    }

    @Test
    void settingsHaveDefaultsAndDisabledCategoryStopsNewNotifications() throws Exception {
        UUID user = UUID.randomUUID();
        settings(user).andExpect(status().isOk())
                .andExpect(jsonPath("$.push").value(true)).andExpect(jsonPath("$.email").value(true))
                .andExpect(jsonPath("$.categories.messages").value(true)).andExpect(jsonPath("$.categories.orders").value(true))
                .andExpect(jsonPath("$.categories.bookings").value(true)).andExpect(jsonPath("$.categories.followed").value(false));
        // Domyślnie obserwowane kanały są wyłączone.
        create(user, "followed", "Nowy film", "Latte z Magnifica S", "/kanal/domikawa", "v-1").andExpect(status().isNoContent());

        putSettings(user, "{\"push\":false,\"email\":true,\"categories\":{\"messages\":true,\"orders\":false,\"bookings\":true,\"followed\":true}}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.push").value(false)).andExpect(jsonPath("$.categories.orders").value(false));
        settings(user).andExpect(jsonPath("$.push").value(false)).andExpect(jsonPath("$.categories.followed").value(true));
        create(user, "followed", "Nowy film", "Latte z Magnifica S", "/kanal/domikawa", "v-1").andExpect(status().isCreated());
        create(user, "orders", "Zamówienie #2", "Treść", null, "o-2").andExpect(status().isNoContent());
        list(user, "").andExpect(jsonPath("$.items", hasSize(1)));

        putSettings(user, "{\"push\":true}").andExpect(status().isBadRequest());
        putSettings(user, "{\"push\":true,\"email\":true,\"categories\":{\"messages\":true}}").andExpect(status().isBadRequest());
    }

    @Test
    void devicesAreRegisteredOncePerTokenAndRemovedOnlyByOwner() throws Exception {
        UUID user = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        String token = "fcm-" + UUID.randomUUID();
        String first = device(user, "{\"token\":\"" + token + "\",\"platform\":\"ANDROID\"}").andExpect(status().isCreated())
                .andExpect(jsonPath("$.platform").value("ANDROID")).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(first, "$.id");
        // Ten sam token drugi raz to to samo urządzenie; brak platformy to WEB tylko przy pierwszej rejestracji.
        device(user, "{\"token\":\"" + token + "\",\"platform\":\"ANDROID\"}").andExpect(jsonPath("$.id").value(id));
        device(user, "{\"token\":\"  \"}").andExpect(status().isBadRequest());
        device(user, "{\"token\":\"abc\",\"platform\":\"WINDOWS\"}").andExpect(status().isBadRequest());
        String web = device(user, "{\"token\":\"web-" + UUID.randomUUID() + "\"}").andExpect(status().isCreated())
                .andExpect(jsonPath("$.platform").value("WEB")).andReturn().getResponse().getContentAsString();

        mvc.perform(delete("/api/v1/notifications/devices/" + id).header(HttpHeaders.AUTHORIZATION, bearer(other))).andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/notifications/devices/" + id).header(HttpHeaders.AUTHORIZATION, bearer(user))).andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/notifications/devices/" + id).header(HttpHeaders.AUTHORIZATION, bearer(user))).andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/notifications/devices/" + (String) JsonPath.read(web, "$.id")).header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .andExpect(status().isNoContent());

        // Token po zalogowaniu na inne konto przechodzi na to konto: poprzedni właściciel go nie usunie.
        String shared = "shared-" + UUID.randomUUID();
        String sharedId = JsonPath.read(device(user, "{\"token\":\"" + shared + "\"}").andReturn().getResponse().getContentAsString(), "$.id");
        device(other, "{\"token\":\"" + shared + "\"}").andExpect(jsonPath("$.id").value(sharedId));
        mvc.perform(delete("/api/v1/notifications/devices/" + sharedId).header(HttpHeaders.AUTHORIZATION, bearer(user))).andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/notifications/devices/" + sharedId).header(HttpHeaders.AUTHORIZATION, bearer(other))).andExpect(status().isNoContent());
    }

    private ResultActions create(UUID user, String category, String title, String body, String link, String dedupeKey) throws Exception {
        StringBuilder json = new StringBuilder("{\"userId\":\"" + user + "\",\"category\":\"" + category + "\",\"title\":\"" + title
                + "\",\"body\":\"" + body + "\"");
        if (link != null) {
            json.append(",\"link\":\"").append(link).append('"');
        }
        if (dedupeKey != null) {
            json.append(",\"dedupeKey\":\"").append(dedupeKey).append('"');
        }
        return mvc.perform(post("/internal/notifications").contentType(MediaType.APPLICATION_JSON).content(json.append('}').toString()));
    }

    private ResultActions list(UUID user, String query) throws Exception {
        return mvc.perform(get("/api/v1/notifications" + query).header(HttpHeaders.AUTHORIZATION, bearer(user)));
    }

    private ResultActions settings(UUID user) throws Exception {
        return mvc.perform(get("/api/v1/notifications/settings").header(HttpHeaders.AUTHORIZATION, bearer(user)));
    }

    private ResultActions putSettings(UUID user, String body) throws Exception {
        return mvc.perform(put("/api/v1/notifications/settings").header(HttpHeaders.AUTHORIZATION, bearer(user))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions device(UUID user, String body) throws Exception {
        return mvc.perform(post("/api/v1/notifications/devices").header(HttpHeaders.AUTHORIZATION, bearer(user))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String bearer(UUID userId) {
        return "Bearer user-" + userId;
    }
}
