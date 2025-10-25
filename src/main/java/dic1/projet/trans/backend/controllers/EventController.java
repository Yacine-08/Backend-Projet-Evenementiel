package dic1.projet.trans.backend.controllers;

import dic1.projet.trans.backend.dtos.*;
import dic1.projet.trans.backend.entities.Event;
import dic1.projet.trans.backend.entities.User;
import dic1.projet.trans.backend.enums.Role;
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

@RestController
@RequestMapping("/api/events")
@Tag(name = "Events", description = "Gestion des événements")
public class EventController {

    @Autowired
    private EventService eventService;

    @Autowired
    private AuthenticationService authenticationService;

    @Operation(summary = "Créer un événement (Organisateur uniquement)")
    @PostMapping("/create")
    public ResponseEntity<?> createEvent(
            @Valid @RequestBody EventCreateDTO dto,
            Authentication authentication) {

        User currentUser = authenticationService.getCurrentUser(authentication);

        if (!currentUser.getRoles().contains(Role.ORGANIZER)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Seuls les organisateurs peuvent créer des événements"));
        }

        Event event = eventService.createEvent(dto, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(event);
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
        return ResponseEntity.ok(eventService.getAllEvents());
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

}