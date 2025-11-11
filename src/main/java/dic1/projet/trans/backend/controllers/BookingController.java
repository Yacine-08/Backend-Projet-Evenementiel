package dic1.projet.trans.backend.controllers;

import dic1.projet.trans.backend.dtos.CreateBookingRequest;
import dic1.projet.trans.backend.dtos.CreateNotificationRequest;
import dic1.projet.trans.backend.entities.Booking;
import dic1.projet.trans.backend.entities.Event;
import dic1.projet.trans.backend.entities.User;
import dic1.projet.trans.backend.enums.NotificationType;
import dic1.projet.trans.backend.exceptions.BadRequestException;
import dic1.projet.trans.backend.exceptions.ResourceNotFoundException;
import dic1.projet.trans.backend.repositories.EventRepository;
import dic1.projet.trans.backend.repositories.UserRepository;
import dic1.projet.trans.backend.services.BookingService;
import dic1.projet.trans.backend.services.EmailService;
import dic1.projet.trans.backend.services.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Réservations", description = "Endpoints pour gérer les réservations de tickets")
public class BookingController {

    private final BookingService bookingService;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

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

        User currentUser = (User) userDetails;

        try {
            List<Booking> confirmedBookings;

            if (group) {
                // Confirmer tout le groupe de réservations
                confirmedBookings = bookingService.confirmBookingGroup(id);

                // Envoyer des notifications pour toutes les réservations confirmées
                if (!confirmedBookings.isEmpty()) {
                    Booking firstBooking = confirmedBookings.get(0);
                    String eventId = firstBooking.getEventId();
                    String groupId = firstBooking.getGroupId();
                    int bookingCount = confirmedBookings.size();

                    // Vérifier que l'utilisateur est l'organisateur de l'événement
                    Event event = eventRepository.findById(eventId)
                            .orElseThrow(() -> new ResourceNotFoundException("Événement non trouvé"));

                    if (!event.getOrganizer().getIdUser().equals(currentUser.getIdUser())) {
                        throw new BadRequestException("Seul l'organisateur peut confirmer les réservations groupées");
                    }

                    // Créer une notification de confirmation de réservation
                    CreateNotificationRequest notificationRequest = new CreateNotificationRequest();
                    notificationRequest.setType(NotificationType.BOOKING_CONFIRMATION);
                    notificationRequest.setRecipientIds(Collections.singletonList(currentUser.getIdUser()));

                    // Ajouter des métadonnées pour le rendu du message
                    Map<String, String> metadata = new HashMap<>();
                    metadata.put("eventId", eventId);
                    metadata.put("eventName", event.getTitle());
                    metadata.put("bookingCount", String.valueOf(bookingCount));
                    metadata.put("isGroup", "true");
                    metadata.put("groupId", groupId);
                    notificationRequest.setMetadata(metadata);

                    // Envoyer la notification
                    notificationService.createNotification(notificationRequest);

                    // Envoyer un email de confirmation
                    try {
                        emailService.sendBookingConfirmationEmail(
                                currentUser.getEmail(),
                                currentUser.getFirstName(),
                                event.getTitle(),
                                groupId,
                                true,
                                bookingCount
                        );
                    } catch (Exception e) {
                        System.err.println("Erreur lors de l'envoi de l'email de confirmation: " + e.getMessage());
                    }
                }
            } else {
                // Confirmer une seule réservation
                Booking confirmedBooking = bookingService.confirmSingleBooking(id);
                confirmedBookings = Collections.singletonList(confirmedBooking);

                // Vérifier que l'utilisateur est l'organisateur ou le propriétaire de la réservation
                Event event = eventRepository.findById(confirmedBooking.getEventId())
                        .orElseThrow(() -> new ResourceNotFoundException("Événement non trouvé"));

                if (!event.getOrganizer().getIdUser().equals(currentUser.getIdUser()) &&
                        !confirmedBooking.getClientId().equals(currentUser.getIdUser())) {
                    throw new BadRequestException("Non autorisé à confirmer cette réservation");
                }

                // Récupérer l'utilisateur destinataire
                User recipient = userRepository.findById(confirmedBooking.getClientId())
                        .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

                // Créer une notification de confirmation de réservation
                CreateNotificationRequest notificationRequest = new CreateNotificationRequest();
                notificationRequest.setType(NotificationType.BOOKING_CONFIRMATION);
                notificationRequest.setRecipientIds(Collections.singletonList(recipient.getIdUser()));

                // Ajouter des métadonnées pour le rendu du message
                Map<String, String> metadata = new HashMap<>();
                metadata.put("eventId", confirmedBooking.getEventId());
                metadata.put("eventName", event.getTitle());
                metadata.put("bookingId", confirmedBooking.getBookingId());
                metadata.put("isGroup", "false");
                notificationRequest.setMetadata(metadata);

                // Envoyer la notification
                notificationService.createNotification(notificationRequest);

                // Envoyer un email de confirmation
                try {
                    emailService.sendBookingConfirmationEmail(
                            recipient.getEmail(),
                            recipient.getFirstName(),
                            event.getTitle(),
                            confirmedBooking.getBookingId(),
                            false,
                            1
                    );
                } catch (Exception e) {
                    System.err.println("Erreur lors de l'envoi de l'email de confirmation: " + e.getMessage());
                }
            }

            return ResponseEntity.ok(confirmedBookings);

        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de la confirmation de la réservation: " + e.getMessage()
            );
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détails d'une réservation", description = "Récupère une réservation si elle appartient à l'utilisateur connecté.")
    public ResponseEntity<?> getBooking(@AuthenticationPrincipal UserDetails userDetails,
                                        @PathVariable String id) {
        User user = (User) userDetails;
        return bookingService.getBookingByIdOwned(id, user.getIdUser())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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