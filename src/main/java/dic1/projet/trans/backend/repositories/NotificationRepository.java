package dic1.projet.trans.backend.repositories;

import dic1.projet.trans.backend.entities.Notification;
import dic1.projet.trans.backend.enums.NotificationType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    List<Notification> findByRecipientIdUserOrderByDateEnvoiDesc(String recipientId);


    List<Notification> findByRecipientIdUserAndIsRead(String recipientId, boolean isRead);

    List<Notification> findByGroupId(String groupId);

    int countByRecipientIdUserAndIsRead(String recipientId, boolean isRead);

    void deleteByRecipientIdUser(String recipientId);

    void deleteByGroupId(String groupId);

    void deleteByDateEnvoiBefore(LocalDateTime date);

    @Query("{ 'recipient.$id': ?0, 'type': ?1 }")
    List<Notification> findByRecipientIdUserAndType(String recipientId, NotificationType type);
}
