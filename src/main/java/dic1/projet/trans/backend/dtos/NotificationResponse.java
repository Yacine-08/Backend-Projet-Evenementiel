package dic1.projet.trans.backend.dtos;

import dic1.projet.trans.backend.enums.NotificationStatus;
import dic1.projet.trans.backend.enums.NotificationType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class NotificationResponse {
    private String idNotif;
    private String title;
    private String content;
    private LocalDateTime dateEnvoi;
    private boolean isRead;
    private NotificationStatus status;
    private NotificationType type;
    private String groupId;
    private UserResponse recipient;
    private Map<String, String> metadata;
    private String actionUrl;
}