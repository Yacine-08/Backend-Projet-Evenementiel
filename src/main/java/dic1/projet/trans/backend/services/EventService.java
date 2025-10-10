package dic1.projet.trans.backend.services;

import dic1.projet.trans.backend.dtos.CreateEventRequest;
import dic1.projet.trans.backend.entities.Event;
import dic1.projet.trans.backend.entities.User;
import dic1.projet.trans.backend.enums.EventStatus;
import dic1.projet.trans.backend.enums.Role;
import dic1.projet.trans.backend.repositories.EventRepository;
import dic1.projet.trans.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class EventService {

    // normalise une chaîne de caractères en enlevant les accents et en la mettant en minuscules

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase();
    }

    @Autowired
    private EventRepository eventRepository;
    
    @Autowired
    private UserRepository userRepository;

    public Event createEvent(CreateEventRequest request) {
        // Verify if the organizer exists
        User organizer = userRepository.findById(request.getOrganizerId())
                .orElseThrow(() -> new IllegalArgumentException("Organizer not found with id: " + request.getOrganizerId()));
        
        // verify the ORGANIZER role
        if (!organizer.hasRole(Role.ORGANIZER)) {
            throw new AccessDeniedException("Only users with ORGANIZER role can create events");
        }
        
        // create a new event
        Event event = new Event();
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setLocation(request.getLocation());
        event.setDateTimeStart(request.getDateTimeStart());
        event.setDateTimeEnd(request.getDateTimeEnd());
        event.setTypeEvent(request.getTypeEvent());
        event.setEventStatus(request.getEventStatus());
        event.setCapacityMaximal(request.getCapacityMaximal());
        event.setOrganizer(organizer);
        event.setImage(request.getImage());
        event.setRefundPolicy(request.getRefundPolicy());
        

        event.setCreationDateTime(LocalDateTime.now());
        
        // save event
        return eventRepository.save(event);
    }

    public Iterable<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    public Optional<Event> getEvent(final String id) {
        return eventRepository.findById(id);
    }

    public Event updateEvent(final String id, CreateEventRequest request) {
        Optional<Event> event = eventRepository.findById(id);
        if (!event.isPresent()) {
            throw new IllegalArgumentException("Event not found with id: " + id);
        }

        User organizer = userRepository.findById(request.getOrganizerId())
                .orElseThrow(() -> new IllegalArgumentException("Organizer not found with id: " + request.getOrganizerId()));

        if (!organizer.hasRole(Role.ORGANIZER)) {
            throw new AccessDeniedException("Only users with ORGANIZER role can update events");
        }

        Event updatedEvent = event.get();

        updatedEvent.setTitle(request.getTitle());
        updatedEvent.setDescription(request.getDescription());
        updatedEvent.setLocation(request.getLocation());
        updatedEvent.setDateTimeStart(request.getDateTimeStart());
        updatedEvent.setDateTimeEnd(request.getDateTimeEnd());
        updatedEvent.setTypeEvent(request.getTypeEvent());
        updatedEvent.setEventStatus(request.getEventStatus());
        updatedEvent.setCapacityMaximal(request.getCapacityMaximal());
        updatedEvent.setOrganizer(organizer);
        updatedEvent.setImage(request.getImage());
        updatedEvent.setRefundPolicy(request.getRefundPolicy());

        return eventRepository.save(updatedEvent);
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
