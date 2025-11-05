package dic1.projet.trans.backend.services;

import dic1.projet.trans.backend.dtos.CreateEventRequest;
import dic1.projet.trans.backend.dtos.EventCreateDTO;
import dic1.projet.trans.backend.dtos.EventUpdateDTO;
import dic1.projet.trans.backend.entities.Event;
import dic1.projet.trans.backend.entities.User;
import dic1.projet.trans.backend.enums.EventStatus;
import dic1.projet.trans.backend.enums.Role;
import dic1.projet.trans.backend.repositories.EventRepository;
import dic1.projet.trans.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Pattern;
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
        event.setAddress(dto.getAddress());
        event.setDateTimeStart(dto.getDateTimeStart());
        event.setDateTimeEnd(dto.getDateTimeEnd());
        event.setTypeEvent(dto.getTypeEvent());
        event.setEventStatus(dto.getEventStatus());
        event.setCapacityMaximal(dto.getCapacityMaximal());
        event.setOrganizer(organizer);
        event.setImage(dto.getImage());
        event.setRefundPolicy(dto.getRefundPolicy());
        event.setCreationDateTime(LocalDateTime.now());

        Event savedEvent = eventRepository.save(event);
        
        // Envoyer une notification à l'organisateur en fonction du statut de l'événement
        boolean isDraft = savedEvent.getEventStatus() == EventStatus.DRAFT;
        notificationService.notifyEventCreated(savedEvent, isDraft);
        
        return savedEvent;
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
        if (dto.getAddress() != null) {
            event.setAddress(dto.getAddress());
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


    // normalise une chaîne de caractères en enlevant les accents et en la mettant en minuscules

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase();
    }



    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }



    public void deleteEvent(final String id) {
        eventRepository.deleteById(id);
    }

    public List<Event> searchEvents(String title, LocalDate date, String location, String typeEvent, String eventStatus) {
        try {
            // create a list to store results
            List<Event> results = new ArrayList<>();

            // tracks if any search criteria were provided
            boolean hasSearchCriteria = false;

            // search by title (insensible a la casse et aux accents)
            if (title != null && !title.trim().isEmpty()) {
                hasSearchCriteria = true;
                String normalizedTitle = normalizeText(title);

                // create a search pattern
                String searchPattern = ".*" + Pattern.quote(normalizedTitle) + ".*";
                results = eventRepository.findByTitle(searchPattern);
            }

            // search by location (insensible à la casse et aux accents)
//            if (location != null && !location.trim().isEmpty()) {
//                hasSearchCriteria = true;
//                String normalizedLocation = normalizeText(location);
//
//                // create a search pattern
//                String searchPattern = ".*" + Pattern.quote(normalizedLocation) + ".*";
//                List<Event> byLocation = eventRepository.findByLocation(searchPattern);
//                System.out.println("Résultats par localisation: " + byLocation.size() + " pour: " + location.trim());
//
//                if (results.isEmpty()) {
//                    results = byLocation;
//                } else {
//                    // Create a map of existing results by ID for faster lookup
//                    Map<String, Event> resultMap = new HashMap<>();
//                    for (Event event : results) {
//                        resultMap.put(event.getIdEvent(), event);
//                    }
//
//                    // Find common events by ID
//                    List<Event> intersection = new ArrayList<>();
//                    for (Event event : byLocation) {
//                        if (resultMap.containsKey(event.getIdEvent())) {
//                            intersection.add(event);
//                        }
//                    }
//                    results = intersection;
//                }
//            }

            // search by type (insensible à la casse et aux accents)
            if (typeEvent != null && !typeEvent.trim().isEmpty()) {
                hasSearchCriteria = true;
                String normalizedType = normalizeText(typeEvent);
                // Créer un motif de recherche insensible à la casse et aux accents
                String searchPattern = "^" + Pattern.quote(normalizedType) + "$";
                List<Event> byType = eventRepository.findByTypeEvent(searchPattern);

                if (results.isEmpty()) {
                    results = byType;
                } else {
                    // Create a map of existing results by ID for faster lookup
                    Map<String, Event> resultMap = new HashMap<>();
                    for (Event event : results) {
                        resultMap.put(event.getIdEvent(), event);
                    }

                    // Find common events by ID
                    List<Event> intersection = new ArrayList<>();
                    for (Event event : byType) {
                        if (resultMap.containsKey(event.getIdEvent())) {
                            intersection.add(event);
                        }
                    }
                    results = intersection;
                }
            }

            // search by status (insensible à la casse)
            if (eventStatus != null && !eventStatus.trim().isEmpty()) {
                hasSearchCriteria = true;
                String statusTerm = eventStatus.trim().toUpperCase();
                List<Event> byStatus;

                try {
                    // Convertir le statut en enum et effectuer la recherche
                    EventStatus status = EventStatus.valueOf(statusTerm);
                    byStatus = eventRepository.findByEventStatus(status);

                    // Mettre à jour les résultats
                    if (results.isEmpty()) {
                        results = byStatus;
                    } else {
                        results.retainAll(byStatus);
                    }
                } catch (IllegalArgumentException e) {
                    System.err.println("Statut d'événement non valide: " + statusTerm);
                    throw new IllegalArgumentException("Statut d'événement non valide: " + statusTerm +
                            ". Les valeurs possibles sont: " +
                            Arrays.toString(EventStatus.values()));
                }
            }

            // search by date
            if (date != null) {
                hasSearchCriteria = true;
                LocalDateTime startOfDay = date.atStartOfDay();
                LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
                List<Event> byDate = eventRepository.findByDateTimeStartBetween(startOfDay, endOfDay);

                if (results.isEmpty()) {
                    results = byDate;
                } else {
                    // Create a map of existing results by ID for faster lookup
                    Map<String, Event> resultMap = new HashMap<>();
                    for (Event event : results) {
                        resultMap.put(event.getIdEvent(), event);
                    }

                    // Find common events by ID
                    List<Event> intersection = new ArrayList<>();
                    for (Event event : byDate) {
                        if (resultMap.containsKey(event.getIdEvent())) {
                            intersection.add(event);
                        }
                    }
                    results = intersection;
                }
            }

            // Si aucun critère de recherche n'est fourni, retourner tous les événements
            if (!hasSearchCriteria) {
                return (List<Event>) eventRepository.findAll();
            }

            return results;
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la recherche d'événements", e);
        }
    }
}
