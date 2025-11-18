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
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
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

    private final EventRepository eventRepository;
    private final NotificationService notificationService;
    private final TicketService ticketService;
    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final MongoTemplate mongoTemplate;

    @Autowired
    public EventService(EventRepository eventRepository, 
                       NotificationService notificationService,
                       TicketService ticketService,
                       BookingRepository bookingRepository,
                       TicketRepository ticketRepository,
                       MongoTemplate mongoTemplate) {
        this.eventRepository = eventRepository;
        this.notificationService = notificationService;
        this.ticketService = ticketService;
        this.bookingRepository = bookingRepository;
        this.ticketRepository = ticketRepository;
        this.mongoTemplate = mongoTemplate;
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

    /**
     * Compte le nombre de réservations confirmées pour un événement
     * @param eventId ID de l'événement
     * @return Le nombre de réservations confirmées
     */
    public long getBookingCount(String eventId) {
        System.out.println("=== DEBUG: Getting tickets reserved (non-cancelled) for event: " + eventId);

        List<Booking> bookings = bookingRepository.findByEventId(eventId);

        long totalTickets = bookings.stream()
                .filter(b -> b.getBookingStatus() != BookingStatus.CANCELLED)
                .flatMap(booking -> booking.getTickets().stream())
                .mapToLong(Booking.ReservedTicket::getQuantity)
                .sum();

        System.out.println("=== DEBUG: Total reserved tickets (seats) non-cancelled: " + totalTickets);
        if (totalTickets == 0) {
            List<Ticket> tickets = ticketRepository.findByEventId(eventId);
            long sold = tickets.stream().mapToLong(Ticket::getSoldQuantity).sum();
            System.out.println("=== DEBUG: Fallback tickets.soldQuantity sum: " + sold);
            return sold;
        }
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
        System.out.println("=== DEBUG: Calculating revenue (non-cancelled bookings) for event: " + eventId);
        List<Booking> bookings = bookingRepository.findByEventId(eventId);

        double total = bookings.stream()
                .filter(b -> b.getBookingStatus() != BookingStatus.CANCELLED)
                .mapToDouble(Booking::getTotalAmount)
                .sum();

        System.out.println("=== DEBUG: Event " + eventId + " revenue from non-cancelled bookings: " + total);
        if (total == 0.0) {
            List<Ticket> tickets = ticketRepository.findByEventId(eventId);
            double alt = tickets.stream()
                    .mapToDouble(t -> t.getPrice() * t.getSoldQuantity())
                    .sum();
            System.out.println("=== DEBUG: Fallback revenue from tickets price*soldQuantity: " + alt);
            return alt;
        }
        return total;
    }

    /**
     * Récupérer tous les événements d'un organisateur
     */
    public List<Event> getEventsByOrganizer(String organizerId) {
        return eventRepository.findByOrganizerIdUser(organizerId);
    }

    public List<dic1.projet.trans.backend.dtos.RecentEventDTO> getRecentEventsByOrganizer(String organizerId, int limit) {
        List<Event> events = eventRepository.findByOrganizerIdUser(organizerId, Sort.by(Direction.DESC, "dateTimeStart"));
        if (events == null || events.isEmpty()) {
            return Collections.emptyList();
        }
        int n = Math.max(limit, 1);
        return events.stream()
                .limit(n)
                .map(ev -> {
                    String eventId = ev.getIdEvent();
                    long seats = getBookingCount(eventId);
                    double rev = calculateEventRevenue(eventId);
                    System.out.println("=== RECENT EVENTS: eventId=" + eventId + ", title='" + ev.getTitle() + "' seats=" + seats + ", revenue=" + rev);
                    return dic1.projet.trans.backend.dtos.RecentEventDTO.builder()
                            .idEvent(eventId)
                            .title(ev.getTitle())
                            .dateTimeStart(ev.getDateTimeStart())
                            .capacityMaximal(ev.getCapacityMaximal())
                            .eventStatus(ev.getEventStatus())
                            .reservationsSeats(seats)
                            .revenue(rev)
                            .build();
                })
                .toList();
    }

    public List<dic1.projet.trans.backend.dtos.RevenueByEventDTO> getEventRevenuesByOrganizer(String organizerId) {
        List<Event> events = eventRepository.findByOrganizerIdUser(organizerId);
        if (events == null || events.isEmpty()) {
            return Collections.emptyList();
        }
        return events.stream().map(ev -> {
            double revenue = calculateEventRevenue(ev.getIdEvent());
            System.out.println("=== ORGANIZER EVENT REVENUE: organizer=" + organizerId + ", eventId=" + ev.getIdEvent() + ", title='" + ev.getTitle() + "', revenue=" + revenue);
            return dic1.projet.trans.backend.dtos.RevenueByEventDTO.builder()
                    .eventId(ev.getIdEvent())
                    .title(ev.getTitle())
                    .totalRevenue(revenue)
                    .build();
        }).toList();
    }

    public List<dic1.projet.trans.backend.dtos.CategoryCountDTO> getEventCategoryCountsByOrganizer(String organizerId) {
        List<Event> events = eventRepository.findByOrganizerIdUser(organizerId);
        if (events == null || events.isEmpty()) {
            return Collections.emptyList();
        }
        java.util.Map<String, Long> counts = new java.util.HashMap<>();
        for (Event ev : events) {
            String cat = ev.getCategory() != null ? ev.getCategory().trim() : "";
            if (cat.isEmpty()) continue;
            counts.put(cat, counts.getOrDefault(cat, 0L) + 1);
        }
        List<dic1.projet.trans.backend.dtos.CategoryCountDTO> result = new java.util.ArrayList<>();
        counts.forEach((cat, count) -> {
            System.out.println("=== ORGANIZER CATEGORY COUNT: organizer=" + organizerId + ", category='" + cat + "', count=" + count);
            result.add(dic1.projet.trans.backend.dtos.CategoryCountDTO.builder()
                    .category(cat)
                    .count(count)
                    .build());
        });
        // Sort descending by count
        result.sort((a, b) -> Long.compare(b.getCount(), a.getCount()));
        return result;
    }

    public List<dic1.projet.trans.backend.dtos.FillRateByEventDTO> getEventFillRatesByOrganizer(String organizerId) {
        List<Event> events = eventRepository.findByOrganizerIdUser(organizerId);
        if (events == null || events.isEmpty()) {
            return Collections.emptyList();
        }
        List<dic1.projet.trans.backend.dtos.FillRateByEventDTO> result = new java.util.ArrayList<>();
        for (Event ev : events) {
            Integer capacityVal = ev.getCapacityMaximal();
            int capacity = capacityVal != null ? capacityVal : 0;
            long reservedSeats = 0L;
            List<Booking> bookings = bookingRepository.findByEventId(ev.getIdEvent());
            for (Booking b : bookings) {
                if (b.getBookingStatus() == dic1.projet.trans.backend.enums.BookingStatus.CANCELLED) continue;
                if (b.getTickets() != null) {
                    for (Booking.ReservedTicket rt : b.getTickets()) {
                        reservedSeats += rt.getQuantity();
                    }
                }
            }
            double fillRate = capacity > 0 ? Math.min(100.0, (reservedSeats * 100.0) / capacity) : 0.0;
            System.out.println("=== ORGANIZER FILL RATE: organizer=" + organizerId + ", eventId=" + ev.getIdEvent() + ", title='" + ev.getTitle() + "', fillRate=" + fillRate + ", reservedSeats=" + reservedSeats + "/" + capacity);
            result.add(dic1.projet.trans.backend.dtos.FillRateByEventDTO.builder()
                    .eventId(ev.getIdEvent())
                    .title(ev.getTitle())
                    .fillRate(fillRate)
                    .build());
        }
        // Optionally sort by fill rate descending
        result.sort((a, b) -> Double.compare(b.getFillRate(), a.getFillRate()));
        return result;
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



    public List<Event> getAllAvailableEvents() {
        return eventRepository.findAllAvailableEvents(LocalDateTime.now());
    }



    public void deleteEvent(final String id) {
        eventRepository.deleteById(id);
    }

    public List<Event> searchEvents(String title, LocalDate date, String location, String typeEvent, String eventStatus, String category) {
        try {
            // Créer un critère de recherche dynamique
            Criteria criteria = new Criteria();
            List<Criteria> criteriaList = new ArrayList<>();

            // Ajouter les critères uniquement si les paramètres ne sont pas nuls ni vides
            if (title != null && !title.trim().isEmpty()) {
                criteriaList.add(Criteria.where("title").regex(Pattern.quote(title), "i"));
            }
            
            if (date != null) {
                LocalDateTime startOfDay = date.atStartOfDay();
                LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
                criteriaList.add(Criteria.where("dateTimeStart").gte(startOfDay).lte(endOfDay));
            }
            
            if (location != null && !location.trim().isEmpty()) {
                criteriaList.add(Criteria.where("location").regex(Pattern.quote(location), "i"));
            }
            
            if (typeEvent != null && !typeEvent.trim().isEmpty()) {
                criteriaList.add(Criteria.where("typeEvent").is(typeEvent));
            }
            
            if (eventStatus != null && !eventStatus.trim().isEmpty()) {
                try {
                    EventStatus status = EventStatus.valueOf(eventStatus.trim().toUpperCase());
                    criteriaList.add(Criteria.where("eventStatus").is(status));
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Statut d'événement non valide: " + eventStatus +
                            ". Les valeurs possibles sont: " + Arrays.toString(EventStatus.values()));
                }
            }
            
            if (category != null && !category.trim().isEmpty()) {
                criteriaList.add(Criteria.where("category").regex("^" + Pattern.quote(category) + "$", "i"));
            }
            
            // Si aucun critère n'est spécifié, retourner tous les événements
            if (criteriaList.isEmpty()) {
                return eventRepository.findAll();
            }
            
            // Combiner les critères avec un ET logique
            criteria.andOperator(criteriaList.toArray(new Criteria[0]));
            
            // Exécuter la requête
            Query query = new Query(criteria);
            return mongoTemplate.find(query, Event.class);
        } catch (Exception e) {
            // En cas d'erreur, relancer l'exception pour une meilleure gestion des erreurs
            throw new RuntimeException("Erreur lors de la recherche d'événements", e);
        }
    }
}
