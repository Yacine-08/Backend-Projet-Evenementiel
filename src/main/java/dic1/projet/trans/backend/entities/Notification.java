package dic1.projet.trans.backend.entities;

import dic1.projet.trans.backend.enums.NotificationStatus;
import dic1.projet.trans.backend.enums.NotificationType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notifications")
@ToString(exclude = {"recipient"})
@EqualsAndHashCode(exclude = {"recipient"})
public class Notification implements Serializable {

    @Id
    private String idNotif;

    private String title;
    private String content;
    private LocalDateTime dateEnvoi;

    @Indexed
    private boolean isRead = false;

    private NotificationStatus status;
    private NotificationType type;

    @DBRef(lazy = true)
    @Indexed
    private User recipient;

    @Indexed
    private String groupId;

    private Map<String, String> metadata = new HashMap<>();

    private String actionUrl;
}