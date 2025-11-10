package dic1.projet.trans.backend.controllers;

import dic1.projet.trans.backend.dtos.*;
import dic1.projet.trans.backend.entities.Event;
import dic1.projet.trans.backend.entities.User;
import dic1.projet.trans.backend.repositories.EventRepository;
import dic1.projet.trans.backend.enums.EventType;
import dic1.projet.trans.backend.enums.Role;
import dic1.projet.trans.backend.repositories.EventRepository;
import dic1.projet.trans.backend.services.AuthenticationService;
import dic1.projet.trans.backend.services.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/events")
@Tag(name = "Events", description = "Gestion des événements")
public class EventController {

    @Autowired
    private EventService eventService;

    @Autowired
    private AuthenticationService authenticationService;
    
    @Autowired
    private EventRepository eventRepository;

    @Operation(summary = "Créer un événement (Organisateur uniquement)")
    @PostMapping("/create")
    public ResponseEntity<?> createEvent(
            @Valid @RequestBody EventCreateDTO dto,
            Authentication authentication) {
        try {
            User currentUser = authenticationService.getCurrentUser(authentication);

            if (!currentUser.getRoles().contains(Role.ORGANIZER)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "error", "Seuls les organisateurs peuvent créer des événements"));
            }

            // Validation des dates
            if (dto.getDateTimeStart() != null && dto.getDateTimeEnd() != null && 
                dto.getDateTimeEnd().isBefore(dto.getDateTimeStart())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "error", "La date de fin doit être postérieure à la date de début"));
            }

            // Validation des billets pour un événement payant
            if (dto.getTypeEvent() != null && dto.getTypeEvent() == EventType.PAID &&
                (dto.getTickets() == null || dto.getTickets().isEmpty())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "error", "Un événement payant doit avoir au moins un type de billet"));
            }

            Event event = eventService.createEvent(dto, currentUser);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                        "success", true, 
                        "event", event,
                        "eventId", event.getIdEvent()
                    ));
                    
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Une erreur est survenue lors de la création de l'événement: " + e.getMessage()));
        }
    }

    @Operation(summary = "Mettre à jour un événement")
    @PutMapping("/{eventId}")
    public ResponseEntity<?> updateEvent(
            @PathVariable String eventId,
            @Valid @RequestBody EventUpdateDTO dto,
            Authentication authentication) {

        User currentUser = authenticationService.getCurrentUser(authentication);

        try {
            Event updatedEvent = eventService.updateEvent(eventId, dto, currentUser);
            return ResponseEntity.ok(updatedEvent);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Annuler un événement")
    @PutMapping("/{eventId}/cancel")
    public ResponseEntity<?> cancelEvent(
            @PathVariable String eventId,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {

        User currentUser = authenticationService.getCurrentUser(authentication);
        String reason = body != null ? body.get("reason") : "Non spécifiée";

        try {
            Event cancelledEvent = eventService.cancelEvent(eventId, currentUser, reason);
            return ResponseEntity.ok(Map.of(
                    "message", "Événement annulé avec succès",
                    "event", cancelledEvent
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Récupérer un événement par ID")
    @GetMapping("/{eventId}")
    public ResponseEntity<?> getEvent(@PathVariable String eventId) {
        try {
            Event event = eventService.getEventById(eventId);
            return ResponseEntity.ok(event);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Récupérer les événements (Organisateur)")
    @GetMapping("/my-events")
    public ResponseEntity<?> getMyEvents(Authentication authentication) {
        User currentUser = authenticationService.getCurrentUser(authentication);

        if (!currentUser.getRoles().contains(Role.ORGANIZER)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Accessible uniquement aux organisateurs"));
        }

        return ResponseEntity.ok(eventService.getEventsByOrganizer(currentUser.getIdUser()));
    }

    @GetMapping("/events")
    public ResponseEntity<List<Event>> getAllEvents() {
        return ResponseEntity.ok(eventService.getAllAvailableEvents());
    }

    @GetMapping("/search")
    public List<Event> searchEvents(
            @RequestParam(required = false, name = "title") String title,
            @RequestParam(required = false, name = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false, name = "location") String location,
            @RequestParam(required = false, name = "typeEvent") String typeEvent,
            @RequestParam(required = false, name = "eventStatus") String eventStatus) {

        List<Event> events = eventService.searchEvents(title, date, location, typeEvent, eventStatus);
        return events;
    }
    @GetMapping("/{eventId}/booking-count")
    @Operation(summary = "Get event booking count", description = "Get the number of bookings for a specific event")
    public ResponseEntity<Map<String, Object>> getEventBookingCount(
            @PathVariable String eventId,
            Authentication authentication) {
                
        if (authentication != null) {
            System.out.println("=== DEBUG [booking-count]: Authentication authorities: " + authentication.getAuthorities());
        }

        // Vérifier l'authentification
        if (authentication == null || !authentication.isAuthenticated()) {
            System.out.println("=== DEBUG [booking-count]: Authentication required");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("success", false, "error", "Authentification requise"));
        }
        
        // Get current user
        User currentUser = authenticationService.getCurrentUser(authentication);
        if (currentUser == null) {
            System.out.println("=== DEBUG [booking-count]: Current user is null");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("success", false, "error", "Utilisateur non trouvé"));
        }
        
        System.out.println("=== DEBUG [booking-count]: Current user ID: " + currentUser.getIdUser());
        System.out.println("=== DEBUG [booking-count]: Current user roles: " + currentUser.getRoles());
        
        // Vérifier si l'événement existe
        Optional<Event> eventOpt = eventRepository.findById(eventId);
        if (eventOpt.isEmpty()) {
            System.out.println("=== DEBUG [booking-count]: Event not found with ID: " + eventId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("success", false, "error", "Événement non trouvé"));
        }
        
        Event event = eventOpt.get();
        System.out.println("=== DEBUG [booking-count]: Event found. Organizer ID: " + event.getOrganizer().getIdUser());
        
        boolean isOrganizer = event.getOrganizer().getIdUser().equals(currentUser.getIdUser());
        System.out.println("=== DEBUG [booking-count]: Is organizer: " + isOrganizer);
        
        boolean isAdmin = currentUser.getAuthorities().stream()
            .anyMatch(auth -> {
                boolean matches = auth.getAuthority().equals("ROLE_ADMINISTRATOR");
                System.out.println("=== DEBUG [booking-count]: Checking authority: " + auth.getAuthority() + " matches ADMINISTRATOR: " + matches);
                return matches;
            });
            
        System.out.println("=== DEBUG [booking-count]: Is admin: " + isAdmin);
            
        if (!isOrganizer && !isAdmin) {
            System.out.println("=== DEBUG [booking-count]: Access denied - User is neither organizer nor admin");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("success", false, "error", "Non autorisé à accéder à ces informations"));
        }
        
        System.out.println("=== DEBUG [booking-count]: Access granted - User is authorized");
        try {
            long bookingCount = eventService.getBookingCount(eventId);
            System.out.println("=== DEBUG [booking-count]: Booking count: " + bookingCount);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "bookingCount", bookingCount
            ));
        } catch (Exception e) {
            System.out.println("=== DEBUG [booking-count]: Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Erreur lors du calcul du nombre de réservations: " + e.getMessage()
                ));
        }
    }
    
    @GetMapping("/{eventId}/revenue")
    @Operation(summary = "Get event revenue", description = "Calculate and return the total revenue for a specific event")
    public ResponseEntity<Map<String, Object>> getEventRevenue(
            @PathVariable String eventId,
            Authentication authentication) {

        if (authentication != null) {
            System.out.println("=== DEBUG: Authentication authorities: " + authentication.getAuthorities());
        }

        // Vérifier l'authentification
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                    "success", false,
                    "error", "Authentification requise"
                ));
        }
        
        // Get current user
        User currentUser = authenticationService.getCurrentUser(authentication);
        if (currentUser == null) {
            System.out.println("=== DEBUG: Current user is null");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                    "success", false,
                    "error", "Utilisateur non trouvé"
                ));
        }
        
        System.out.println("=== DEBUG: Current user ID: " + currentUser.getIdUser());
        System.out.println("=== DEBUG: Current user roles: " + currentUser.getRoles());
        
        // Vérifier si l'utilisateur est l'organisateur de l'événement ou un administrateur
        Optional<Event> eventOpt = eventRepository.findById(eventId);
        if (eventOpt.isEmpty()) {
            System.out.println("=== DEBUG: Event not found with ID: " + eventId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                    "success", false,
                    "error", "Événement non trouvé"
                ));
        }
        
        Event event = eventOpt.get();
        System.out.println("=== DEBUG: Event found. Organizer ID: " + event.getOrganizer().getIdUser());
        
        boolean isOrganizer = event.getOrganizer().getIdUser().equals(currentUser.getIdUser());
        System.out.println("=== DEBUG: Is organizer: " + isOrganizer);
        
        boolean isAdmin = currentUser.getAuthorities().stream()
            .anyMatch(auth -> {
                boolean matches = auth.getAuthority().equals("ROLE_ADMINISTRATOR");
                System.out.println("=== DEBUG: Checking authority: " + auth.getAuthority() + " matches ADMINISTRATOR: " + matches);
                return matches;
            });
            
        System.out.println("=== DEBUG: Is admin: " + isAdmin);
            
        if (!isOrganizer && !isAdmin) {
            System.out.println("=== DEBUG: Access denied - User is neither organizer nor admin");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                    "success", false,
                    "error", "Vous n'êtes pas autorisé à accéder aux revenus de cet événement"
                ));
        }
        
        System.out.println("=== DEBUG: Access granted - User is authorized");
        try {
            double revenue = eventService.getRevenue(eventId);
            long bookingCount = eventService.getBookingCount(eventId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "revenue", revenue,
                "bookingCount", bookingCount
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "success", false,
                    "error", "Failed to calculate event revenue: " + e.getMessage()
                ));
        }
    }

    @GetMapping("/{eventId}/potential-revenue")
    @Operation(summary = "Get potential event revenue",
            description = "Calculate and return the potential revenue for a specific event (sum of all ticket prices * quantities)")
    public ResponseEntity<Map<String, Object>> getPotentialRevenue(
            @PathVariable String eventId,
            Authentication authentication) {

        // Check authentication
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "error", "Authentication required"
                    ));
        }

        // Get current user
        User currentUser = authenticationService.getCurrentUser(authentication);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "error", "User not found"
                    ));
        }

        // Check if user is the event organizer or an administrator
        Optional<Event> eventOpt = eventRepository.findById(eventId);
        if (eventOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "success", false,
                            "error", "Event not found"
                    ));
        }

        Event event = eventOpt.get();
        boolean isOrganizer = event.getOrganizer().getIdUser().equals(currentUser.getIdUser());
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role == Role.ADMINISTRATOR);

        if (!isOrganizer && !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "success", false,
                            "error", "You are not authorized to access potential revenue for this event"
                    ));
        }

        try {
            double potentialRevenue = eventService.calculatePotentialRevenue(eventId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "potentialRevenue", potentialRevenue
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "success", false,
                            "error", "Failed to calculate potential event revenue: " + e.getMessage()
                    ));
        }
    }
}