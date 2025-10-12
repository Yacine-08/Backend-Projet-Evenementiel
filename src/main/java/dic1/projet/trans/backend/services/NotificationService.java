package dic1.projet.trans.backend.services;

import dic1.projet.trans.backend.dtos.CreateNotificationRequest;
import dic1.projet.trans.backend.entities.Event;
import dic1.projet.trans.backend.entities.Notification;
import dic1.projet.trans.backend.entities.User;
import dic1.projet.trans.backend.enums.EventType;
import dic1.projet.trans.backend.enums.NotificationStatus;
import dic1.projet.trans.backend.enums.NotificationType;
import dic1.projet.trans.backend.repositories.EventRepository;
import dic1.projet.trans.backend.repositories.NotificationRepository;
import dic1.projet.trans.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;


    // Créer des notifications en masse
    @Transactional
    public List<Notification> createNotification(CreateNotificationRequest request) {

        Map<String, String> enrichedMetadata = enrichMetadataIfEventRelated(request);

        // générer le titre et le contenu selon le type
        String title = generateTitle(request, enrichedMetadata);
        String content = generateContent(request, enrichedMetadata);

        return createBulkNotifications(
                request.getRecipientIds(),
                title,
                content,
                request.getType(),
                enrichedMetadata,
                request.getActionUrl()
        );
    }


    private String generateTitle(CreateNotificationRequest request, Map<String, String> metadata) {
        // si un titre custom est fourni pour CUSTOM, on l'utilise
        if (request.getType() == NotificationType.CUSTOM &&
                request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            return request.getTitle();
        }

        // Sinon, générer selon le type
        switch (request.getType()) {
            case EVENT_CANCELLED:
                return "Annulation d'événement";
            case EVENT_UPDATED:
                return "Modification d'événement";
            case EVENT_REMINDER:
                return "Rappel d'événement";
            case BOOKING_CONFIRMATION:
                return "Confirmation de réservation";
            case BOOKING_CANCELLED:
                return "Annulation de réservation";
            case BOOKING_REMINDER:
                return "Rappel de réservation";
            case PAYMENT_CONFIRMATION:
                return "Paiement confirmé";
            case PAYMENT_REFUND:
                return "Remboursement effectué";
            case ACCOUNT_VERIFIED:
                return "Compte vérifié";
            case SYSTEM_ALERT:
                return "Alerte système";
            case CUSTOM:
            default:
                return request.getTitle() != null ? request.getTitle() : "Notification";
        }
    }


    private String generateContent(CreateNotificationRequest request, Map<String, String> metadata) {
        // si un contenu custom est fourni pour CUSTOM, on l'utilise
        if (request.getType() == NotificationType.CUSTOM &&
                request.getContent() != null && !request.getContent().trim().isEmpty()) {
            return request.getContent();
        }

        String eventName = metadata.get("eventName");
        String reason = getReason(request, metadata);
        boolean isFree = Boolean.parseBoolean(metadata.getOrDefault(EventType.FREE.name(), EventType.PAID.name()));

        switch (request.getType()) {
            case EVENT_CANCELLED:
                return buildEventCancelledContent(eventName, reason, isFree);

            case EVENT_UPDATED:
                return buildEventUpdatedContent(eventName, reason);

            case EVENT_REMINDER:
                return buildEventReminderContent(eventName, metadata);

            case BOOKING_CONFIRMATION:
                return buildBookingConfirmationContent(eventName, metadata, isFree);

            case BOOKING_CANCELLED:
                return buildBookingCancelledContent(eventName, reason, isFree);

            case PAYMENT_CONFIRMATION:
                return buildPaymentConfirmationContent(eventName, metadata);

            case PAYMENT_REFUND:
                return buildPaymentRefundContent(eventName, metadata);

            case BOOKING_REMINDER:
                return buildBookingReminderContent(eventName, metadata);

            case ACCOUNT_VERIFIED:
                return "Félicitations ! Votre compte a été vérifié avec succès. Vous pouvez maintenant accéder à toutes les fonctionnalités.";

            case SYSTEM_ALERT:
                return request.getContent() != null ? request.getContent() :
                        "Vous avez une alerte système. " + (reason.isEmpty() ? "" : "Détails : " + reason);

            case CUSTOM:
            default:
                return request.getContent() != null ? request.getContent() :
                        "Vous avez reçu une notification.";
        }
    }


    private String buildEventCancelledContent(String eventName, String reason, boolean isFree) {
        StringBuilder content = new StringBuilder();
        content.append("Nous vous informons que l'événement");
        if (eventName != null && !eventName.isEmpty()) {
            content.append(" \"").append(eventName).append("\"");
        }
        content.append(" a été annulé");

        if (!reason.isEmpty()) {
            content.append(" en raison de ").append(reason);
        }
        content.append(".");

        if (!isFree) {
            content.append(" Le remboursement s'effectuera dans les 24h.");
        }

        content.append(" Nous nous excusons pour le désagrément causé.");
        return content.toString();
    }

    private String buildEventUpdatedContent(String eventName, String reason) {
        StringBuilder content = new StringBuilder();
        content.append("L'événement");
        if (eventName != null && !eventName.isEmpty()) {
            content.append(" \"").append(eventName).append("\"");
        }
        content.append(" a été modifié");

        if (!reason.isEmpty()) {
            content.append(" : ").append(reason);
        }
        content.append(".");

        return content.toString();
    }

    private String buildEventReminderContent(String eventName, Map<String, String> metadata) {
        StringBuilder content = new StringBuilder();
        content.append("Rappel : L'événement");
        if (eventName != null && !eventName.isEmpty()) {
            content.append(" \"").append(eventName).append("\"");
        }
        content.append(" aura lieu bientôt !");

        String eventDate = metadata.get("eventDateFormatted");
        if (eventDate != null && !eventDate.isEmpty()) {
            content.append(" Date : ").append(eventDate);
        }

        String eventLocation = metadata.get("eventLocation");
        if (eventLocation != null && !eventLocation.isEmpty()) {
            content.append(". Lieu : ").append(eventLocation);
        }

        content.append(".");
        return content.toString();
    }

    private String buildBookingConfirmationContent(String eventName, Map<String, String> metadata, boolean isFree) {
        StringBuilder content = new StringBuilder();
        content.append("Votre réservation pour l'événement");
        if (eventName != null && !eventName.isEmpty()) {
            content.append(" \"").append(eventName).append("\"");
        }
        content.append(" a été confirmée avec succès.");

        if (!isFree && metadata.containsKey("amount")) {
            content.append(" Montant payé : ").append(metadata.get("amount")).append(" FCFA.");
        }

        return content.toString();
    }

    private String buildBookingCancelledContent(String eventName, String reason, boolean isFree) {
        StringBuilder content = new StringBuilder();
        content.append("Votre réservation pour l'événement");
        if (eventName != null && !eventName.isEmpty()) {
            content.append(" \"").append(eventName).append("\"");
        }
        content.append(" a été annulée");

        if (!reason.isEmpty()) {
            content.append(" pour la raison suivante : ").append(reason);
        }
        content.append(".");

        if (!isFree) {
            content.append(" Le remboursement s'effectuera dans les 24h.");
        }

        return content.toString();
    }

    private String buildPaymentConfirmationContent(String eventName, Map<String, String> metadata) {
        StringBuilder content = new StringBuilder();
        content.append("Votre paiement");

        if (metadata.containsKey("amount")) {
            content.append(" d'un montant de ").append(metadata.get("amount")).append(" FCFA");
        }

        content.append(" a été confirmé avec succès.");

        if (eventName != null && !eventName.isEmpty()) {
            content.append(" Événement concerné : \"").append(eventName).append("\".");
        }

        return content.toString();
    }

    private String buildPaymentRefundContent(String eventName, Map<String, String> metadata) {
        StringBuilder content = new StringBuilder();
        content.append("Votre remboursement");

        if (metadata.containsKey("amount")) {
            content.append(" de ").append(metadata.get("amount")).append(" FCFA");
        }

        content.append(" a été effectué avec succès.");

        if (eventName != null && !eventName.isEmpty()) {
            content.append(" Concernant l'événement : \"").append(eventName).append("\".");
        }

        return content.toString();
    }

    private String buildBookingReminderContent(String eventName, Map<String, String> metadata) {
        StringBuilder content = new StringBuilder();
        content.append("Rappel : Vous avez une réservation pour l'événement");
        if (eventName != null && !eventName.isEmpty()) {
            content.append(" \"").append(eventName).append("\"");
        }

        String eventDate = metadata.get("eventDateFormatted");
        if (eventDate != null && !eventDate.isEmpty()) {
            content.append(" le ").append(eventDate);
        }

        content.append(".");
        return content.toString();
    }


    private Map<String, String> enrichMetadataIfEventRelated(CreateNotificationRequest request) {
        Map<String, String> metadata = request.getMetadata() != null ?
                new HashMap<>(request.getMetadata()) : new HashMap<>();

        // ajouter la raison si fournie
        if (request.getReason() != null && !request.getReason().trim().isEmpty()) {
            metadata.put("reason", request.getReason().trim());
        }

        // si c'est lié à un événement, enrichir avec les données de l'événement
        if (isEventRelatedNotification(request.getType()) && metadata.containsKey("eventId")) {
            String eventId = metadata.get("eventId");
            Event event = eventRepository.findById(eventId).orElse(null);

            if (event != null) {
                metadata.put("eventName", event.getTitle());
                metadata.put("eventLocation", event.getLocation());
                metadata.put("eventDate", event.getDateTimeStart().toString());
            }
        }

        return metadata;
    }

    private boolean isEventRelatedNotification(NotificationType type) {
        return type == NotificationType.EVENT_REMINDER ||
                type == NotificationType.EVENT_UPDATED ||
                type == NotificationType.EVENT_CANCELLED ||
                type == NotificationType.BOOKING_CONFIRMATION ||
                type == NotificationType.BOOKING_CANCELLED ||
                type == NotificationType.BOOKING_REMINDER ||
                type == NotificationType.PAYMENT_CONFIRMATION ||
                type == NotificationType.PAYMENT_REFUND;
    }

    private String getReason(CreateNotificationRequest request, Map<String, String> metadata) {
        if (request.getReason() != null && !request.getReason().trim().isEmpty()) {
            return request.getReason().trim();
        }
        if (metadata.containsKey("reason") && metadata.get("reason") != null) {
            return metadata.get("reason").trim();
        }
        return "";
    }

    @Transactional
    public List<Notification> notifyEventCancellation(
            String eventId,
            String eventTitle,
            List<String> participantIds,
            String reason) {

        Map<String, String> metadata = new HashMap<>();
        metadata.put("eventId", eventId);
        metadata.put("eventTitle", eventTitle);
        if (reason != null && !reason.isEmpty()) {
            metadata.put("reason", reason);
        }

        String content = String.format("L'événement '%s' a été annulé.", eventTitle);
        if (reason != null && !reason.isEmpty()) {
            content += String.format(" Raison : %s", reason);
        }
        content += " Nous nous excusons pour le désagrément.";

        return createBulkNotifications(
                participantIds,
                "Événement annulé",
                content,
                NotificationType.EVENT_CANCELLED,
                metadata,
                "/events/" + eventId
        );
    }

    @Transactional
    public List<Notification> notifyEventUpdate(
            String eventId,
            String eventTitle,
            List<String> participantIds,
            String updateDetails) {

        Map<String, String> metadata = new HashMap<>();
        metadata.put("eventId", eventId);
        metadata.put("eventTitle", eventTitle);
        if (updateDetails != null && !updateDetails.isEmpty()) {
            metadata.put("updateDetails", updateDetails);
        }

        String content = String.format("L'événement '%s' a été modifié.", eventTitle);
        if (updateDetails != null && !updateDetails.isEmpty()) {
            content += String.format(" %s", updateDetails);
        }

        return createBulkNotifications(
                participantIds,
                "Événement modifié",
                content,
                NotificationType.EVENT_UPDATED,
                metadata,
                "/events/" + eventId
        );
    }


    @Transactional
    public List<Notification> createBulkNotifications(
            List<String> recipientIds,
            String title,
            String content,
            NotificationType type,
            Map<String, String> metadata,
            String actionUrl) {

        String groupId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        List<User> recipients = userRepository.findAllById(recipientIds);

        List<Notification> notifications = recipients.stream()
                .map(recipient -> {
                    Notification notification = new Notification();
                    notification.setTitle(title);
                    notification.setContent(content);
                    notification.setDateEnvoi(now);
                    notification.setRecipient(recipient);
                    notification.setRead(false);
                    notification.setStatus(NotificationStatus.UNREAD);
                    notification.setType(type);
                    notification.setGroupId(groupId);
                    notification.setMetadata(metadata != null ? metadata : Map.of());
                    notification.setActionUrl(actionUrl);
                    return notification;
                })
                .collect(Collectors.toList());

        return notificationRepository.saveAll(notifications);
    }

//    @Transactional
//    public Notification createNotification(
//            String title,
//            String content,
//            User recipient,
//            NotificationType type,
//            Map<String, String> metadata,
//            String actionUrl) {
//
//        Notification notification = new Notification();
//        notification.setTitle(title);
//        notification.setContent(content);
//        notification.setDateEnvoi(LocalDateTime.now());
//        notification.setRecipient(recipient);
//        notification.setRead(false);
//        notification.setStatus(NotificationStatus.UNREAD);
//        notification.setType(type);
//        notification.setMetadata(metadata != null ? metadata : Map.of());
//        notification.setActionUrl(actionUrl);
//
//        return notificationRepository.save(notification);
//    }


    public List<Notification> getUserNotifications(String userId) {
        return notificationRepository.findByRecipientIdUserOrderByDateEnvoiDesc(userId);
    }

    public List<Notification> getNotificationsByType(String userId, NotificationType type) {
        return notificationRepository.findByRecipientIdUserAndType(userId, type);
    }

    public List<Notification> getNotificationsByGroup(String groupId) {
        return notificationRepository.findByGroupId(groupId);
    }

    public Map<String, Object> getGroupStats(String groupId) {
        List<Notification> notifications = notificationRepository.findByGroupId(groupId);
        long totalCount = notifications.size();
        long readCount = notifications.stream().filter(Notification::isRead).count();
        long unreadCount = totalCount - readCount;

        return Map.of(
                "groupId", groupId,
                "totalSent", totalCount,
                "readCount", readCount,
                "unreadCount", unreadCount,
                "readPercentage", totalCount > 0 ? (readCount * 100.0 / totalCount) : 0
        );
    }

    public int getUnreadCount(String userId) {
        return notificationRepository.countByRecipientIdUserAndIsRead(userId, false);
    }


    @Transactional
    public Notification markAsRead(String notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification non trouvée"));

        if (!notification.getRecipient().getIdUser().equals(userId)) {
            throw new SecurityException("Non autorisé");
        }

        notification.setRead(true);
        notification.setStatus(NotificationStatus.READ);
        return notificationRepository.save(notification);
    }

    @Transactional
    public int markAllAsRead(String userId) {
        List<Notification> unreadNotifications = notificationRepository
                .findByRecipientIdUserAndIsRead(userId, false);

        unreadNotifications.forEach(notification -> {
            notification.setRead(true);
            notification.setStatus(NotificationStatus.READ);
        });

        notificationRepository.saveAll(unreadNotifications);
        return unreadNotifications.size();
    }

    @Transactional
    public void deleteNotification(String notificationId, String userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification non trouvée"));

        if (!notification.getRecipient().getIdUser().equals(userId)) {
            throw new SecurityException("Non autorisé");
        }

        notificationRepository.delete(notification);
    }

    @Transactional
    public void deleteAllUserNotifications(String userId) {
        notificationRepository.deleteByRecipientIdUser(userId);
    }
}