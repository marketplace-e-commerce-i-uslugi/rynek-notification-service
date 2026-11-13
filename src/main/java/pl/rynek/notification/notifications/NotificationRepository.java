package pl.rynek.notification.notifications;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Query(value = "select * from notifications where user_id = :userId order by created_at desc, id desc limit :limit offset :offset", nativeQuery = true)
    List<Notification> page(UUID userId, int offset, int limit);

    /** 0: to samo zdarzenie (ten sam {@code dedupeKey}) było już zgłoszone. */
    @Modifying
    @Query(value = "insert into notifications (id, user_id, category, title, body, link, dedupe_key, created_at) "
            + "values (:id, :userId, :category, :title, :body, :link, :dedupeKey, :now) on conflict do nothing", nativeQuery = true)
    int add(UUID id, UUID userId, String category, String title, String body, String link, String dedupeKey, Instant now);

    @Query(value = "select count(*) > 0 from notifications where id = :id and user_id = :userId", nativeQuery = true)
    boolean owned(UUID id, UUID userId);

    @Modifying
    @Query(value = "update notifications set read_at = :now where id = :id and user_id = :userId and read_at is null", nativeQuery = true)
    int markRead(UUID id, UUID userId, Instant now);

    @Modifying
    @Query(value = "update notifications set read_at = :now where user_id = :userId and read_at is null", nativeQuery = true)
    int markAllRead(UUID userId, Instant now);
}
