package dic1.projet.trans.backend.services;

import dic1.projet.trans.backend.dtos.CreateTicketRequest;
import dic1.projet.trans.backend.dtos.UpdateTicketRequest;
import dic1.projet.trans.backend.entities.Event;
import dic1.projet.trans.backend.entities.Ticket;
import dic1.projet.trans.backend.entities.User;
import dic1.projet.trans.backend.enums.Role;
import dic1.projet.trans.backend.exceptions.BadRequestException;
import dic1.projet.trans.backend.exceptions.ResourceNotFoundException;
import dic1.projet.trans.backend.exceptions.ValidationException;
import dic1.projet.trans.backend.repositories.EventRepository;
import dic1.projet.trans.backend.repositories.TicketRepository;
import dic1.projet.trans.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final TicketValidationService ticketValidationService;

    public Ticket createTicket(String userId, String eventId, CreateTicketRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Événement non trouvé"));

        // Vérifier que l'utilisateur est l'organisateur de l'événement
        if (event.getOrganizer() == null || !event.getOrganizer().getIdUser().equals(userId)) {
            throw new AccessDeniedException("Seul l'organisateur de l'événement peut créer des billets");
        }

        // Vérification de rôle (défense en profondeur)
        User organizer = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        if (!organizer.hasRole(Role.ORGANIZER)) {
            throw new AccessDeniedException("Rôle ORGANIZER requis");
        }

        Ticket ticket = new Ticket();
        ticket.setTicketType(request.getTicketType());
        ticket.setPrice(request.getPrice());
        ticket.setInitialQuantity(request.getInitialQuantity());
        ticket.setSoldQuantity(0);
        ticket.setEventId(eventId);
        
        // Valider le ticket en fonction du type d'événement
        try {
            ticketValidationService.validateTicket(ticket);
        } catch (ValidationException e) {
            throw new BadRequestException(e.getMessage());
        }

        return ticketRepository.save(ticket);
    }

    public List<Ticket> listTicketsByEvent(String eventId) {
        return ticketRepository.findByEventId(eventId);
    }

    public Ticket getTicketById(String ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Billet non trouvé"));
    }

    public Ticket updateTicket(String userId, String ticketId, UpdateTicketRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Billet non trouvé"));

        Event event = eventRepository.findById(ticket.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Événement lié au billet non trouvé"));

        if (event.getOrganizer() == null || !event.getOrganizer().getIdUser().equals(userId)) {
            throw new AccessDeniedException("Seul l'organisateur de l'événement peut modifier des billets");
        }

        // Appliquer mises à jour
        if (request.getTicketType() != null && !request.getTicketType().isBlank()) {
            ticket.setTicketType(request.getTicketType());
        }
        if (request.getPrice() != null) {
            ticket.setPrice(request.getPrice());
        }
        if (request.getInitialQuantity() != null) {
            if (request.getInitialQuantity() < ticket.getSoldQuantity()) {
                throw new BadRequestException("La quantité initiale ne peut pas être inférieure au nombre déjà vendu");
            }
            ticket.setInitialQuantity(request.getInitialQuantity());
        }

        return ticketRepository.save(ticket);
    }

    public void deleteTicket(String userId, String ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Billet non trouvé"));

        Event event = eventRepository.findById(ticket.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Événement lié au billet non trouvé"));

        if (event.getOrganizer() == null || !event.getOrganizer().getIdUser().equals(userId)) {
            throw new AccessDeniedException("Seul l'organisateur de l'événement peut supprimer des billets");
        }

        if (ticket.getSoldQuantity() > 0) {
            throw new BadRequestException("Impossible de supprimer un billet ayant des ventes");
        }

        ticketRepository.delete(ticket);
    }
}