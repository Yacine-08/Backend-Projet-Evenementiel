package dic1.projet.trans.backend.services;

import dic1.projet.trans.backend.dtos.EventCreateDTO;
import dic1.projet.trans.backend.dtos.EventUpdateDTO;
import dic1.projet.trans.backend.entities.Event;
import dic1.projet.trans.backend.entities.User;
import dic1.projet.trans.backend.enums.EventStatus;
import dic1.projet.trans.backend.repositories.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private NotificationService notificationService;

    /**
     * Créer un nouvel événement
     */
    @Transactional
    public Event createEvent(EventCreateDTO dto, User organizer) {
        Event event = new Event();
        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setLocation(dto.getLocation());
        event.setDateTimeStart(dto.getDateTimeStart());
        event.setDateTimeEnd(dto.getDateTimeEnd());
        event.setTypeEvent(dto.getTypeEvent());
        event.setEventStatus(dto.getEventStatus());
        event.setCapacityMaximal(dto.getCapacityMaximal());
        event.setOrganizer(organizer);
        event.setImage(dto.getImage());
        event.setRefundPolicy(dto.getRefundPolicy());
        event.setCreationDateTime(LocalDateTime.now());

        return eventRepository.save(event);
    }

    /**
     * Mettre à jour un événement et notifier les participants
     */
    @Transactional
    public Event updateEvent(String eventId, EventUpdateDTO dto, User currentUser) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé"));

        // Vérifier que l'utilisateur est bien l'organisateur
        if (!event.getOrganizer().getIdUser().equals(currentUser.getIdUser())) {
            throw new SecurityException("Seul l'organisateur peut modifier cet événement");
        }

        // Sauvegarder l'ancien état pour détecter les changements
        Event oldEvent = cloneEvent(event);

        // Mettre à jour les champs (seulement ceux qui ne sont pas null)
        if (dto.getTitle() != null) {
            event.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            event.setDescription(dto.getDescription());
        }
        if (dto.getLocation() != null) {
            event.setLocation(dto.getLocation());
        }
        if (dto.getDateTimeStart() != null) {
            event.setDateTimeStart(dto.getDateTimeStart());
        }
        if (dto.getDateTimeEnd() != null) {
            event.setDateTimeEnd(dto.getDateTimeEnd());
        }
        if (dto.getTypeEvent() != null) {
            event.setTypeEvent(dto.getTypeEvent());
        }
        if (dto.getEventStatus() != null) {
            event.setEventStatus(dto.getEventStatus());
        }
        if (dto.getCapacityMaximal() != null) {
            event.setCapacityMaximal(dto.getCapacityMaximal());
        }
        if (dto.getImage() != null) {
            event.setImage(dto.getImage());
        }
        if (dto.getRefundPolicy() != null) {
            event.setRefundPolicy(dto.getRefundPolicy());
        }

        Event updatedEvent = eventRepository.save(event);

        // Notifier les participants si il y en a
        if (event.getParticipants() != null && !event.getParticipants().isEmpty()) {
            List<String> participantIds = event.getParticipants().stream()
                    .map(User::getIdUser)
                    .collect(Collectors.toList());

            String changesSummary = dto.getChangesSummary(oldEvent);

            notificationService.notifyEventUpdate(
                    eventId,
                    event.getTitle(),
                    participantIds,
                    changesSummary
            );
        }

        return updatedEvent;
    }

    /**
     * Annuler un événement
     */
    @Transactional
    public Event cancelEvent(String eventId, User currentUser, String reason) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé"));

        if (!event.getOrganizer().getIdUser().equals(currentUser.getIdUser())) {
            throw new SecurityException("Seul l'organisateur peut annuler cet événement");
        }

        event.setEventStatus(EventStatus.CANCELLED);
        Event cancelledEvent = eventRepository.save(event);

        // Notifier tous les participants
        if (event.getParticipants() != null && !event.getParticipants().isEmpty()) {
            List<String> participantIds = event.getParticipants().stream()
                    .map(User::getIdUser)
                    .collect(Collectors.toList());

            notificationService.notifyEventCancellation(
                    eventId,
                    event.getTitle(),
                    participantIds,
                    reason
            );
        }

        return cancelledEvent;
    }

    /**
     * Ajouter un participant à un événement
     */
    @Transactional
    public void addParticipant(String eventId, User participant) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé"));

        if (event.isFull()) {
            throw new RuntimeException("L'événement est complet");
        }

        event.addParticipant(participant);
        eventRepository.save(event);
    }

    /**
     * Récupérer un événement par ID
     */
    public Event getEventById(String eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Événement non trouvé"));
    }

    /**
     * Récupérer tous les événements d'un organisateur
     */
    public List<Event> getEventsByOrganizer(String organizerId) {
        return eventRepository.findByOrganizerIdUser(organizerId);
    }

    /**
     * Cloner un événement pour comparer les changements
     */
    private Event cloneEvent(Event event) {
        Event clone = new Event();
        clone.setTitle(event.getTitle());
        clone.setDescription(event.getDescription());
        clone.setLocation(event.getLocation());
        clone.setDateTimeStart(event.getDateTimeStart());
        clone.setDateTimeEnd(event.getDateTimeEnd());
        clone.setCapacityMaximal(event.getCapacityMaximal());
        return clone;
    }
}
