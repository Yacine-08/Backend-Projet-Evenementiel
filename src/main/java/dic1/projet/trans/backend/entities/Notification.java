package dic1.projet.trans.backend.entities;

import dic1.projet.trans.backend.enums.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notifications")
public class Notification {
    @Id
    private String idNotif;
    private String content;
    private LocalDateTime dateEnvoi;
    private NotificationStatus status;
    private String userId;
}