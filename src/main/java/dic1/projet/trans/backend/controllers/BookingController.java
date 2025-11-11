package dic1.projet.trans.backend.controllers;

import dic1.projet.trans.backend.dtos.CreateBookingRequest;
import dic1.projet.trans.backend.dtos.CreateNotificationRequest;
import dic1.projet.trans.backend.entities.Booking;
import dic1.projet.trans.backend.entities.User;
import dic1.projet.trans.backend.enums.NotificationType;
import dic1.projet.trans.backend.services.BookingService;
import dic1.projet.trans.backend.services.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Réservations", description = "Endpoints pour gérer les réservations de tickets")
public class BookingController {

    private final BookingService bookingService;
    private final NotificationService notificationService;


    @PostMapping("/create")
    @Operation(summary = "Créer une réservation", description = "Crée une ou plusieurs réservations pour des tickets. Authentification requise.")
    public ResponseEntity<List<Booking>> createBooking(@AuthenticationPrincipal UserDetails userDetails,
                                                     @Valid @RequestBody CreateBookingRequest request) {
        User user = (User) userDetails;
        List<Booking> bookings = bookingService.createBooking(user.getIdUser(), request);
        return ResponseEntity.ok(bookings);

    }

    @PutMapping("/confirm/{id}")
    @Operation(summary = "Confirmer une réservation", description = "Confirme une réservation (organisateur requis). Si group=true, confirme toutes les réservations du même groupe.")
    public ResponseEntity<List<Booking>> confirmBooking(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "false") boolean group) {
        
        try {
            List<Booking> confirmedBookings;
            
            if (group) {
                // Confirmer tout le groupe de réservations
                confirmedBookings = bookingService.confirmBookingGroup(id);
                
                // Envoyer des notifications pour toutes les réservations confirmées
                if (!confirmedBookings.isEmpty()) {
                    Booking firstBooking = confirmedBookings.get(0);
                    String eventId = firstBooking.getEventId();
                    String userId = ((User) userDetails).getIdUser();
                    String groupId = firstBooking.getGroupId();
                    
                    // Créer une notification de confirmation de réservation
                    CreateNotificationRequest notificationRequest = new CreateNotificationRequest();
                    notificationRequest.setType(NotificationType.BOOKING_CONFIRMATION);
                    notificationRequest.setRecipientIds(Collections.singletonList(userId));
                    
                    // Ajouter des métadonnées pour le rendu du message
                    Map<String, String> metadata = new HashMap<>();
                    metadata.put("eventId", eventId);
                    metadata.put("bookingCount", String.valueOf(confirmedBookings.size()));
                    metadata.put("isGroup", "true");
                    metadata.put("groupId", groupId);
                    notificationRequest.setMetadata(metadata);
                    
                    // Envoyer la notification
                    notificationService.createNotification(notificationRequest);
                }
            } else {
                // Confirmer une seule réservation
                Booking confirmedBooking = bookingService.confirmSingleBooking(id);
                confirmedBookings = Collections.singletonList(confirmedBooking);
                
                // Créer une notification de confirmation de réservation
                CreateNotificationRequest notificationRequest = new CreateNotificationRequest();
                notificationRequest.setType(NotificationType.BOOKING_CONFIRMATION);
                notificationRequest.setRecipientIds(Collections.singletonList(confirmedBooking.getClientId()));
                
                // Ajouter des métadonnées pour le rendu du message
                Map<String, String> metadata = new HashMap<>();
                metadata.put("eventId", confirmedBooking.getEventId());
                metadata.put("bookingId", confirmedBooking.getBookingId());
                metadata.put("isGroup", "false");
                notificationRequest.setMetadata(metadata);
                
                // Envoyer la notification
                notificationService.createNotification(notificationRequest);
            }
            
            return ResponseEntity.ok(confirmedBookings);
            
        } catch (Exception e) {
            // Log l'erreur et relancer une exception appropriée
            // (vous pouvez personnaliser cette partie selon votre gestion d'erreurs)
            throw new RuntimeException("Erreur lors de la confirmation de la réservation", e);
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détails d'une réservation", description = "Récupère une réservation si elle appartient à l'utilisateur connecté.")
    public ResponseEntity<Booking> getBooking(@AuthenticationPrincipal UserDetails userDetails,
                                              @PathVariable String id) {
        User user = (User) userDetails;
        Booking booking = bookingService.getBookingByIdOwned(id, user.getIdUser());
        return ResponseEntity.ok(booking);
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Annuler une réservation", description = "Annule une réservation et réajuste les quantités des tickets.")
    public ResponseEntity<Booking> cancelBooking(@AuthenticationPrincipal UserDetails userDetails,
                                                 @PathVariable String id) {
        User user = (User) userDetails;
        Booking booking = bookingService.cancelBooking(id, user.getIdUser());
        return ResponseEntity.ok(booking);
    }
}