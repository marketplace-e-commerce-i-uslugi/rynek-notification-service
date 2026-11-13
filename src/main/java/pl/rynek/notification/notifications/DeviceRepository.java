package pl.rynek.notification.notifications;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface DeviceRepository extends JpaRepository<Device, UUID> {

    Optional<Device> findByToken(String token);

    @Modifying
    @Query(value = "delete from devices where id = :id and user_id = :userId", nativeQuery = true)
    int remove(UUID id, UUID userId);
}
