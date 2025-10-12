package dic1.projet.trans.backend.controllers;

import dic1.projet.trans.backend.dtos.CreateTicketRequest;
import dic1.projet.trans.backend.dtos.UpdateTicketRequest;
import dic1.projet.trans.backend.entities.Ticket;
import dic1.projet.trans.backend.entities.User;
import dic1.projet.trans.backend.services.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Tag(name = "Tickets", description = "Endpoints pour gérer les billets d'événements")
public class TicketController {

    private final TicketService ticketService;

    @PostMapping("/event/{eventId}")
    @Operation(summary = "Créer un billet", description = "Crée un billet pour un événement (organisateur requis)")
    public ResponseEntity<Ticket> createTicket(@AuthenticationPrincipal UserDetails userDetails,
                                               @PathVariable String eventId,
                                               @Valid @RequestBody CreateTicketRequest request) {
        User user = (User) userDetails;
        Ticket ticket = ticketService.createTicket(user.getIdUser(), eventId, request);
        return ResponseEntity.ok(ticket);
    }

    @GetMapping("/event/{eventId}")
    @Operation(summary = "Lister les billets d'un événement", description = "Retourne les billets disponibles pour un événement")
    public ResponseEntity<List<Ticket>> listTickets(@PathVariable String eventId) {
        return ResponseEntity.ok(ticketService.listTicketsByEvent(eventId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détails d'un billet", description = "Récupère les informations d'un billet")
    public ResponseEntity<Ticket> getTicket(@PathVariable String id) {
        return ResponseEntity.ok(ticketService.getTicketById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Mettre à jour un billet", description = "Modifie un billet (organisateur requis)")
    public ResponseEntity<Ticket> updateTicket(@AuthenticationPrincipal UserDetails userDetails,
                                               @PathVariable String id,
                                               @Valid @RequestBody UpdateTicketRequest request) {
        User user = (User) userDetails;
        return ResponseEntity.ok(ticketService.updateTicket(user.getIdUser(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un billet", description = "Supprime un billet (organisateur requis, aucune vente)")
    public ResponseEntity<Void> deleteTicket(@AuthenticationPrincipal UserDetails userDetails,
                                             @PathVariable String id) {
        User user = (User) userDetails;
        ticketService.deleteTicket(user.getIdUser(), id);
        return ResponseEntity.noContent().build();
    }
}