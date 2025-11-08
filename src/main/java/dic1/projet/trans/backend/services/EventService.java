package dic1.projet.trans.backend.services;

import dic1.projet.trans.backend.dtos.CreateEventRequest;
import dic1.projet.trans.backend.dtos.EventCreateDTO;
import dic1.projet.trans.backend.dtos.EventUpdateDTO;
import dic1.projet.trans.backend.dtos.TicketCreateDTO;
import dic1.projet.trans.backend.entities.Booking;
import dic1.projet.trans.backend.entities.Event;
import dic1.projet.trans.backend.entities.Ticket;
import dic1.projet.trans.backend.entities.User;
import dic1.projet.trans.backend.enums.BookingStatus;
import dic1.projet.trans.backend.enums.EventStatus;
import dic1.projet.trans.backend.enums.EventType;
import dic1.projet.trans.backend.enums.Role;
import dic1.projet.trans.backend.exceptions.ResourceNotFoundException;
import dic1.projet.trans.backend.repositories.BookingRepository;
import dic1.projet.trans.backend.repositories.EventRepository;
import dic1.projet.trans.backend.repositories.TicketRepository;
import dic1.projet.trans.backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import dic1.projet.trans.backend.exceptions.ResourceNotFoundException;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final NotificationService notificationService;
    private final TicketService ticketService;
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;

    @Autowired
    public EventService(EventRepository eventRepository, 
                       NotificationService notificationService,
                       TicketService ticketService,
                       BookingRepository bookingRepository,
                       TicketRepository ticketRepository) {
        this.eventRepository = eventRepository;
        this.notificationService = notificationService;
        this.ticketService = ticketService;
        this.bookingRepository = bookingRepository;
        this.ticketRepository = ticketRepository;
    }

    /**
     * Créer un nouvel événement
     */
    @Transactional
    public Event createEvent(EventCreateDTO dto, User organizer) {
        try {
            Event event = new Event();
            event.setTitle(dto.getTitle());
            event.setDescription(dto.getDescription());
            event.setCategory(dto.getCategory()); // Ajout de la catégorie
            event.setLocation(dto.getLocation());
            event.setAddress(dto.getAddress());
            event.setDateTimeStart(dto.getDateTimeStart());
            event.setDateTimeEnd(dto.getDateTimeEnd());
            event.setTypeEvent(dto.getTypeEvent());
            event.setEventStatus(dto.getEventStatus());
            event.setCapacityMaximal(dto.getCapacityMaximal());
            event.setOrganizer(organizer);
            event.setImage(dto.getImage());
            // Gestion des remboursements
            event.setRefundEnabled(dto.isRefundEnabled());
            event.setRefundPolicy(dto.getRefundPolicy());
            event.setRefundConditions(dto.getRefundConditions());
            event.setRefundDeadlineDays(dto.getRefundDeadlineDays());
            
            event.setCreationDateTime(LocalDateTime.now());

            // Valider la configuration des remboursements
            if (!event.isRefundConfigValid()) {
                throw new IllegalArgumentException("Configuration de remboursement invalide");
            }

            // Sauvegarder d'abord l'événement pour obtenir son ID
            Event savedEvent = eventRepository.save(event);
            
            // Créer les billets associés à l'événement
            if (dto.getTickets() != null && !dto.getTickets().isEmpty()) {
                for (TicketCreateDTO ticketDto : dto.getTickets()) {
                    ticketService.createTicket(ticketDto, savedEvent);
                }
            }
            
            // Envoyer une notification à l'organisateur en fonction du statut de l'événement
            boolean isDraft = savedEvent.getEventStatus() == EventStatus.DRAFT;
            notificationService.notifyEventCreated(savedEvent, isDraft);
            
            return savedEvent;
        } catch (Exception e) {
            // Log l'erreur pour le débogage
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la création de l'événement: " + e.getMessage(), e);
        }
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
    /**
     * Récupérer un événement par ID avec calcul du revenu en temps réel
     */
    public Event getEventById(String eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Événement non trouvé avec l'ID: " + eventId));
        
        return event;
    }

    public double getRevenue(String eventId) {
        return calculateEventRevenue(eventId);
    }
    
    /**
     * Récupère le nombre de réservations pour un événement
     */

    public long getBookingCount(String eventId) {
        System.out.println("=== DEBUG: Getting booking count for event: " + eventId);

        // Compter uniquement les réservations confirmées
        List<Booking> bookings = bookingRepository.findByEventIdAndBookingStatus(
                eventId,
                BookingStatus.CONFIRMED
        );

        // Calculer le nombre total de billets
        long totalTickets = bookings.stream()
                .flatMap(booking -> booking.getTickets().stream())
                .mapToLong(Booking.ReservedTicket::getQuantity)
                .sum();

        System.out.println("=== DEBUG: Total tickets across all bookings: " + totalTickets);
        return totalTickets;
    }
    
    /**
     * Calcule le revenu potentiel d'un événement (somme des prix de tous les billets disponibles)
     * @param eventId ID de l'événement
     * @return Le revenu potentiel total
     */
    public double calculatePotentialRevenue(String eventId) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Événement non trouvé avec l'ID: " + eventId));
            
        if (event.getTypeEvent() != EventType.PAID) {
            return 0.0; // Les événements gratuits n'ont pas de revenu potentiel
        }
        
        // Récupérer tous les tickets pour cet événement
        List<Ticket> tickets = ticketRepository.findByEventId(eventId);
        
        return tickets.stream()
            .mapToDouble(ticket -> ticket.getPrice() * ticket.getInitialQuantity())
            .sum();
    }
    
    /**
     * Calcule le revenu total d'un événement en fonction des réservations
     */
    public double calculateEventRevenue(String eventId) {
        System.out.println("=== DEBUG: Calculating revenue for event: " + eventId);
        Double revenue = bookingRepository.calculateTotalRevenueByEventId(eventId);
        System.out.println("=== DEBUG: Raw revenue from repository: " + revenue);
        
        // If no revenue is found from the aggregation, calculate it manually
        if (revenue == null || revenue == 0.0) {
            List<Booking> bookings = bookingRepository.findByEventIdAndBookingStatus(eventId, BookingStatus.CONFIRMED);
            System.out.println("=== DEBUG: Found " + bookings.size() + " confirmed bookings for event " + eventId);
            
            if (!bookings.isEmpty()) {
                double calculatedRevenue = 0.0;
                
                for (Booking booking : bookings) {
                    System.out.println("=== DEBUG: Processing booking: " + booking.getBookingId());
                    
                    if (booking.getTickets() != null && !booking.getTickets().isEmpty()) {
                        System.out.println("=== DEBUG: Booking has " + booking.getTickets().size() + " tickets");
                        
                        for (Booking.ReservedTicket ticket : booking.getTickets()) {
                            System.out.println("=== DEBUG: Ticket ID: " + ticket.getTicketId() + ", Quantity: " + ticket.getQuantity());
                            
                            // Look up the ticket price
                            Optional<Ticket> ticketInfo = ticketRepository.findById(ticket.getTicketId());
                            if (ticketInfo.isPresent()) {
                                double ticketAmount = ticketInfo.get().getPrice() * ticket.getQuantity();
                                System.out.println("=== DEBUG: Ticket price: " + ticketInfo.get().getPrice());
                                System.out.println("=== DEBUG: Calculated amount: " + ticketAmount);
                                calculatedRevenue += ticketAmount;
                            } else {
                                System.out.println("=== DEBUG: Could not find ticket with ID: " + ticket.getTicketId());
                            }
                        }
                    }
                }
                
                System.out.println("=== DEBUG: Total calculated revenue: " + calculatedRevenue);
                return calculatedRevenue;
            }
            return 0.0;
        }
        
        return revenue;
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
