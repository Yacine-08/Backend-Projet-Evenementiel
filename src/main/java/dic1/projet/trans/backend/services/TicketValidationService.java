package dic1.projet.trans.backend.services;

import dic1.projet.trans.backend.entities.Event;
import dic1.projet.trans.backend.entities.Ticket;
import dic1.projet.trans.backend.exceptions.ValidationException;
import dic1.projet.trans.backend.repositories.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketValidationService {

    private final EventRepository eventRepository;

    /**
     * Valide un ticket en fonction du type d'événement
     * @param ticket Le ticket à valider
     * @throws ValidationException si la validation échoue
     */
    public void validateTicket(Ticket ticket) {
        if (ticket == null) {
            throw new ValidationException("Le ticket ne peut pas être nul");
        }

        // Récupérer l'événement associé
        Event event = eventRepository.findById(ticket.getEventId())
                .orElseThrow(() -> new ValidationException("Événement non trouvé avec l'ID: " + ticket.getEventId()));

        // Vérifier le prix en fonction du type d'événement
        if (event.isPaidEvent()) {
            // Pour un événement payant, le prix doit être strictement supérieur à 0
            if (ticket.getPrice() <= 0) {
                throw new ValidationException("Le prix doit être supérieur à 0 pour un événement payant");
            }
        } else {
            // Pour un événement gratuit, le prix doit être de 0
            if (ticket.getPrice() < 0) {
                throw new ValidationException("Le prix ne peut pas être négatif");
            }
            // On peut forcer le prix à 0 pour les événements gratuits
            ticket.setPrice(0);
        }

        // Autres validations si nécessaire...
        if (ticket.getInitialQuantity() <= 0) {
            throw new ValidationException("La quantité initiale doit être supérieure à 0");
        }
    }
}
