package dic1.projet.trans.backend.controllers;

import dic1.projet.trans.backend.dtos.*;
import dic1.projet.trans.backend.entities.Notification;
import dic1.projet.trans.backend.entities.User;
import dic1.projet.trans.backend.enums.Role;
import dic1.projet.trans.backend.services.AuthenticationService;
import dic1.projet.trans.backend.services.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Gestion des notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuthenticationService authenticationService;

    private NotificationResponse convertToResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setIdNotif(notification.getIdNotif());
        response.setTitle(notification.getTitle());
        response.setContent(notification.getContent());
        response.setDateEnvoi(notification.getDateEnvoi());
        response.setRead(notification.isRead());
        response.setStatus(notification.getStatus());
        response.setType(notification.getType());
        response.setGroupId(notification.getGroupId());
        response.setMetadata(notification.getMetadata());
        response.setActionUrl(notification.getActionUrl());

        if (notification.getRecipient() != null) {
            UserResponse recipientResponse = new UserResponse();
            recipientResponse.setIdUser(notification.getRecipient().getIdUser());
            recipientResponse.setFirstName(notification.getRecipient().getFirstName());
            recipientResponse.setLastName(notification.getRecipient().getLastName());
            recipientResponse.setEmail(notification.getRecipient().getEmail());
            response.setRecipient(recipientResponse);
        }

        return response;
    }

    @Operation(summary = "Créer des notifications")
    @PostMapping("/create")
    public ResponseEntity<?> createNotifications(
            @Valid @RequestBody CreateNotificationRequest request) {

        try {
            // Toute la logique est dans le service
            List<Notification> notifications = notificationService.createNotification(request);
            String groupId = notifications.isEmpty() ? null : notifications.get(0).getGroupId();

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "groupId", groupId,
                    "totalCreated", notifications.size(),
                    "message", "Notifications créées avec succès"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @Operation(summary = "Récupérer mes notifications")
    @GetMapping("/me")
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(Authentication authentication) {
        User currentUser = authenticationService.getCurrentUser(authentication);
        List<Notification> notifications = notificationService.getUserNotifications(currentUser.getIdUser());
        List<NotificationResponse> responses = notifications.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Compter mes notifications non lues")
    @GetMapping("/me/unread-count")
    public ResponseEntity<Map<String, Integer>> getUnreadCount(Authentication authentication) {
        User currentUser = authenticationService.getCurrentUser(authentication);
        int count = notificationService.getUnreadCount(currentUser.getIdUser());
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @Operation(summary = "Marquer une notification comme lue")
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<?> markAsRead(
            @PathVariable String notificationId,
            Authentication authentication) {

        User currentUser = authenticationService.getCurrentUser(authentication);

        try {
            Notification notification = notificationService.markAsRead(notificationId, currentUser.getIdUser());
            return ResponseEntity.ok(convertToResponse(notification));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
    
    @Operation(summary = "Marquer une notification comme non lue")
    @PutMapping("/{notificationId}/unread")
    public ResponseEntity<?> markAsUnread(
            @PathVariable String notificationId,
            Authentication authentication) {

        User currentUser = authenticationService.getCurrentUser(authentication);

        try {
            Notification notification = notificationService.markAsUnread(notificationId, currentUser.getIdUser());
            return ResponseEntity.ok(convertToResponse(notification));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Supprimer toutes mes notifications")
    @DeleteMapping("/me")
    public ResponseEntity<Map<String, Object>> deleteAllNotifications(Authentication authentication) {
        User currentUser = authenticationService.getCurrentUser(authentication);
        notificationService.deleteAllUserNotifications(currentUser.getIdUser());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Toutes vos notifications ont été supprimées"
        ));
    }

    @Operation(summary = "Statistiques d'un envoi groupé (Organisateurs)")
    @GetMapping("/group/{groupId}/stats")
    public ResponseEntity<?> getGroupStats(
            @PathVariable String groupId,
            Authentication authentication) {

        User currentUser = authenticationService.getCurrentUser(authentication);

        if (!currentUser.getRoles().contains(Role.ORGANIZER)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Accès réservé aux organisateurs"));
        }

        return ResponseEntity.ok(notificationService.getGroupStats(groupId));
    }

    @Operation(summary = "Marquer toutes mes notifications comme lues")
    @PutMapping("/me/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead(Authentication authentication) {
        try {
            User currentUser = authenticationService.getCurrentUser(authentication);
            int count = notificationService.markAllAsRead(currentUser.getIdUser());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "markedAsRead", count
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "markedAsRead", 0, "error", e.getMessage()));
        }
    }

    @Operation(summary = "Supprimer une notification")
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<?> deleteNotification(
            @PathVariable String notificationId,
            Authentication authentication) {

        User currentUser = authenticationService.getCurrentUser(authentication);

        try {
            notificationService.deleteNotification(notificationId, currentUser.getIdUser());
            return ResponseEntity.ok(Map.of("success", true, "message", "Notification supprimée"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}