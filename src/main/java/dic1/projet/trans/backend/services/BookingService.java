package dic1.projet.trans.backend.services;

import dic1.projet.trans.backend.dtos.BookingDetailsDTO;
import dic1.projet.trans.backend.dtos.CreateBookingRequest;
import dic1.projet.trans.backend.dtos.ReservedTicketRequest;
import dic1.projet.trans.backend.entities.Booking;
import dic1.projet.trans.backend.entities.Event;
import dic1.projet.trans.backend.entities.Ticket;
import dic1.projet.trans.backend.enums.BookingStatus;
import dic1.projet.trans.backend.exceptions.BadRequestException;
import dic1.projet.trans.backend.exceptions.ResourceNotFoundException;
import dic1.projet.trans.backend.repositories.BookingRepository;
import dic1.projet.trans.backend.repositories.EventRepository;
import dic1.projet.trans.backend.repositories.TicketRepository;
import dic1.projet.trans.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    /**
     * Récupère toutes les réservations d'un utilisateur
     * @param userId ID de l'utilisateur
     * @return Liste des réservations de l'utilisateur
     */
    public List<BookingDetailsDTO> getUserBookings(String userId) {
        System.out.println("Recherche des réservations pour l'utilisateur ID: " + userId);
        List<Booking> bookings = bookingRepository.findByClientId(userId);
        System.out.println("Nombre de réservations trouvées : " + bookings.size());
        
        // Récupérer tous les IDs d'événements uniques
        List<String> eventIds = bookings.stream()
                .map(Booking::getEventId)
                .distinct()
                .collect(Collectors.toList());
                
        // Récupérer tous les événements en une seule requête
        Map<String, Event> events = eventRepository.findByIdIn(eventIds).stream()
                .collect(Collectors.toMap(Event::getIdEvent, e -> e));
        
        // Récupérer tous les IDs de tickets uniques
        List<String> ticketIds = bookings.stream()
                .flatMap(booking -> booking.getTickets().stream())
                .map(Booking.ReservedTicket::getTicketId)
                .distinct()
                .collect(Collectors.toList());
                
        // Récupérer tous les tickets en une seule requête
        Map<String, Ticket> tickets = ticketRepository.findAllById(ticketIds).stream()
                .collect(Collectors.toMap(Ticket::getTicketId, t -> t));
        
        // Construire la réponse avec les détails complets
        return bookings.stream()
                .map(booking -> {
                    Event event = events.get(booking.getEventId());
                    if (event == null) {
                        return null; // ou gérer le cas où l'événement n'existe plus
                    }
                    
                    List<Ticket> bookingTickets = booking.getTickets().stream()
                            .map(bt -> tickets.get(bt.getTicketId()))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                            
                    return BookingDetailsDTO.fromBookingAndEvent(booking, event, bookingTickets);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<Booking> createBooking(String userId, CreateBookingRequest request) {
        if (request.getTickets() == null || request.getTickets().isEmpty()) {
            throw new BadRequestException("Aucun ticket fourni pour la réservation");
        }

        // Vérifier que l'événement existe
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Événement non trouvé: " + request.getEventId()));

        // Idempotence: fusionner avec une réservation PENDING existante de même utilisateur+événement
        List<Booking> existingPendings = bookingRepository.findByClientIdAndEventIdAndBookingStatus(
                userId, request.getEventId(), BookingStatus.PENDING);
        Booking booking;
        if (!existingPendings.isEmpty()) {
            booking = existingPendings.get(0);
        } else {
            booking = new Booking();
            booking.setGroupId(UUID.randomUUID().toString());
            booking.setBookingDate(LocalDateTime.now());
            booking.setPaymentMethod(request.getPaymentMethod());
            booking.setBookingStatus(BookingStatus.PENDING);
            booking.setClientId(userId);
            booking.setEventId(request.getEventId());
            booking.setTickets(new ArrayList<>());
            booking.setTotalAmount(0.0);
        }

        List<Booking.ReservedTicket> reservedTickets = booking.getTickets() != null
                ? new ArrayList<>(booking.getTickets())
                : new ArrayList<>();
        double totalAmount = booking.getTotalAmount();

        for (ReservedTicketRequest rt : request.getTickets()) {
            Ticket ticket = ticketRepository.findById(rt.getTicketId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket non trouvé: " + rt.getTicketId()));

            if (!ticket.getEventId().equals(request.getEventId())) {
                throw new BadRequestException("Le ticket " + rt.getTicketId() + " n'appartient pas à l'événement " + request.getEventId());
            }

            int available = ticket.getInitialQuantity() - ticket.getSoldQuantity();
            if (rt.getQuantity() > available) {
                throw new BadRequestException("Quantité demandée supérieure au stock disponible pour le ticket: " + rt.getTicketId());
            }

            reservedTickets.add(new Booking.ReservedTicket(ticket.getTicketId(), rt.getQuantity()));
            totalAmount += (ticket.getPrice() * rt.getQuantity());
        }

        booking.setTickets(reservedTickets);
        booking.setTotalAmount(totalAmount);
        Booking saved = bookingRepository.save(booking);
        return Collections.singletonList(saved);
    }
    
    /**
     * Confirme une réservation et met à jour les quantités vendues
     * @param bookingId ID de la réservation à confirmer
     * @return La réservation confirmée
     */
    @Transactional
    public Booking confirmSingleBooking(String bookingId) {
        System.out.println("=== DEBUG: Confirming single booking: " + bookingId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation non trouvée"));

        if (booking.getBookingStatus() == BookingStatus.CONFIRMED) {
            return booking;
        }

        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Impossible de confirmer une réservation annulée");
        }

        // Vérifier la disponibilité des billets pour cette réservation uniquement
        for (Booking.ReservedTicket rt : booking.getTickets()) {
            Ticket ticket = ticketRepository.findById(rt.getTicketId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket non trouvé: " + rt.getTicketId()));

            // Somme des quantités déjà vendues (réservations confirmées) pour ce ticket
            int alreadySold = bookingRepository.countSoldTicketsByTicketId(rt.getTicketId()).orElse(0);

            // Vérifier la disponibilité en tenant compte de la quantité actuelle
            int available = ticket.getInitialQuantity() - alreadySold;
            if (rt.getQuantity() > available) {
                throw new BadRequestException("Quantité insuffisante pour le ticket " + ticket.getTicketType() +
                    ". Disponible: " + available + ", Demandé: " + rt.getQuantity());
            }
        }

        // Mettre à jour le statut de la réservation
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        
        // Mettre à jour les quantités vendues pour chaque ticket
        for (Booking.ReservedTicket rt : booking.getTickets()) {
            ticketRepository.findById(rt.getTicketId()).ifPresent(ticket -> {
                // Somme des quantités confirmées existantes
                int totalSold = bookingRepository.countSoldTicketsByTicketId(rt.getTicketId()).orElse(0);

                // Ajouter la quantité de la réservation actuelle
                totalSold += rt.getQuantity();

                // Ne pas dépasser la quantité initiale
                totalSold = Math.min(totalSold, ticket.getInitialQuantity());

                // Mettre à jour le nombre de billets vendus
                ticket.setSoldQuantity(totalSold);
                ticketRepository.save(ticket);
            });
        }
        
        booking = bookingRepository.save(booking);
        System.out.println("=== DEBUG: La réservation a été confirmée avec succès");
        return booking;
    }
    
    // Méthode de compatibilité pour l'ancien code
    @Deprecated
    @Transactional
    public Booking confirmBooking(String bookingId) {
        return confirmSingleBooking(bookingId);
    }

    @Transactional
    public List<Booking> confirmBookingGroup(String bookingId) {
        System.out.println("=== DEBUG: Confirming booking group for booking: " + bookingId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation non trouvée"));

        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Impossible de confirmer une réservation annulée");
        }

        // Récupérer toutes les réservations du même groupe non annulées
        List<Booking> groupBookings = bookingRepository.findByGroupIdAndBookingStatusNot(
                booking.getGroupId(), BookingStatus.CANCELLED);

        // Confirmer chaque réservation du groupe
        List<Booking> confirmedBookings = new ArrayList<>();
        for (Booking groupBooking : groupBookings) {
            if (groupBooking.getBookingStatus() != BookingStatus.CONFIRMED) {
                Booking confirmed = confirmSingleBooking(groupBooking.getBookingId());
                confirmedBookings.add(confirmed);
            } else {
                confirmedBookings.add(groupBooking);
            }
        }

        System.out.println("=== DEBUG: Toutes les réservations du groupe ont été confirmées avec succès");
        return confirmedBookings;
    }
    public List<Booking> getBookingsForUser(String userId) {
        // Retourne directement la liste des réservations triées par date décroissante
        return bookingRepository.findByClientIdOrderByBookingDateDesc(userId);
    }

    public Optional<Booking> getBookingByIdOwned(String bookingId, String userId) {
        return bookingRepository.findById(bookingId)
                .filter(booking -> booking.getClientId().equals(userId));
    }

    public List<BookingDetailsDTO> getRecentBookingsForOrganizer(String organizerId, Integer limit, BookingStatus status) {
        List<Event> events = eventRepository.findByOrganizerIdUser(organizerId);
        List<String> eventIds = events.stream().map(Event::getIdEvent).toList();
        if (eventIds.isEmpty()) return Collections.emptyList();

        List<Booking> bookings = (status != null)
                ? bookingRepository.findByEventIdInAndBookingStatusOrderByBookingDateDesc(eventIds, status)
                : bookingRepository.findByEventIdInOrderByBookingDateDesc(eventIds);

        int n = (limit != null && limit > 0) ? limit : 5;
        List<Booking> limited = bookings.stream().limit(n).toList();

        Map<String, Event> eventMap = events.stream().collect(Collectors.toMap(Event::getIdEvent, e -> e));

        List<String> ticketIds = limited.stream()
                .flatMap(b -> b.getTickets().stream())
                .map(Booking.ReservedTicket::getTicketId)
                .distinct()
                .toList();
        Map<String, Ticket> tickets = ticketRepository.findAllById(ticketIds).stream()
                .collect(Collectors.toMap(Ticket::getTicketId, t -> t));

        return limited.stream().map(b -> {
            Event ev = eventMap.get(b.getEventId());
            if (ev == null) return null;
            List<Ticket> bookedTickets = b.getTickets().stream()
                    .map(rt -> tickets.get(rt.getTicketId()))
                    .filter(Objects::nonNull)
                    .toList();
            BookingDetailsDTO dto = BookingDetailsDTO.fromBookingAndEvent(b, ev, bookedTickets);
            userRepository.findById(b.getClientId()).ifPresent(u -> {
                String full = ((u.getFirstName() != null ? u.getFirstName() : "") + " " + (u.getLastName() != null ? u.getLastName() : "")).trim();
                dto.setClientName(full.isEmpty() ? (u.getUsername() != null ? u.getUsername() : u.getEmail()) : full);
            });
            return dto;
        }).filter(Objects::nonNull).toList();
    }

    public List<dic1.projet.trans.backend.dtos.MonthlySalesDTO> getMonthlySalesForOrganizer(String organizerId, int months) {
        List<Event> events = eventRepository.findByOrganizerIdUser(organizerId);
        List<String> eventIds = events.stream().map(Event::getIdEvent).toList();
        if (eventIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        int m = months > 0 ? months : 6;
        java.time.LocalDateTime start = now.minusMonths(m - 1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        List<Booking> bookings = bookingRepository.findByEventIdInOrderByBookingDateDesc(eventIds);

        java.util.Map<java.time.YearMonth, Long> counts = new java.util.LinkedHashMap<>();
        for (int i = m - 1; i >= 0; i--) {
            java.time.YearMonth ym = java.time.YearMonth.from(now.minusMonths(i));
            counts.put(ym, 0L);
        }

        for (Booking b : bookings) {
            if (b.getBookingDate() == null) continue;
            if (b.getBookingStatus() != BookingStatus.CONFIRMED) continue;
            if (b.getBookingDate().isBefore(start)) continue;
            java.time.YearMonth ym = java.time.YearMonth.from(b.getBookingDate());
            if (counts.containsKey(ym)) {
                counts.put(ym, counts.get(ym) + 1);
            }
        }

        List<dic1.projet.trans.backend.dtos.MonthlySalesDTO> result = new java.util.ArrayList<>();
        counts.forEach((ym, count) -> {
            String label = ym.getYear() + "-" + String.format("%02d", ym.getMonthValue());
            System.out.println("=== MONTHLY SALES: organizer=" + organizerId + ", month=" + label + ", salesCount=" + count);
            result.add(dic1.projet.trans.backend.dtos.MonthlySalesDTO.builder()
                    .month(label)
                    .salesCount(count)
                    .build());
        });

        return result;
    }
    
    /**
     * Récupère la première réservation d'un groupe
     * @param groupId L'ID du groupe
     * @return La première réservation du groupe si elle existe
     */
    public Optional<Booking> getFirstBookingByGroupId(String groupId) {
        return bookingRepository.findFirstByGroupId(groupId);
    }
    
    @Transactional
    public Booking cancelBooking(String bookingId, String userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation non trouvée"));

        if (!booking.getClientId().equals(userId)) {
            throw new BadRequestException("Vous n'êtes pas autorisé à annuler cette réservation");
        }

        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("La réservation est déjà annulée");
        }

        // Mettre à jour les quantités vendues si la réservation était confirmée
        if (booking.getBookingStatus() == BookingStatus.CONFIRMED) {
            for (Booking.ReservedTicket rt : booking.getTickets()) {
                ticketRepository.findById(rt.getTicketId()).ifPresent(ticket -> {
                    int newSoldQuantity = Math.max(0, ticket.getSoldQuantity() - rt.getQuantity());
                    ticket.setSoldQuantity(newSoldQuantity);
                    ticketRepository.save(ticket);
                    System.out.println("=== DEBUG: Annulation - Mise à jour de soldQuantity pour le ticket " + 
                                     rt.getTicketId() + ". Nouvelle valeur: " + newSoldQuantity);
                });
            }
        }

        // Mettre à jour le statut de la réservation
        booking.setBookingStatus(BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }

    @Transactional
    public void cancelBookingGroup(String groupId, String userId) {
        List<Booking> bookings = bookingRepository.findByGroupId(groupId);

        if (bookings.isEmpty()) {
            throw new ResourceNotFoundException("Aucune réservation trouvée pour ce groupe");
        }

        // Vérifier que l'utilisateur est bien le propriétaire des réservations
        if (!bookings.get(0).getClientId().equals(userId)) {
            throw new BadRequestException("Vous n'êtes pas autorisé à annuler ces réservations");
        }

        // Annuler chaque réservation du groupe
        for (Booking booking : bookings) {
            if (booking.getBookingStatus() != BookingStatus.CANCELLED) {
                if (booking.getBookingStatus() == BookingStatus.CONFIRMED) {
                    // Mettre à jour les quantités vendues pour chaque ticket de la réservation
                    for (Booking.ReservedTicket rt : booking.getTickets()) {
                        ticketRepository.findById(rt.getTicketId()).ifPresent(ticket -> {
                            int newSoldQuantity = Math.max(0, ticket.getSoldQuantity() - rt.getQuantity());
                            ticket.setSoldQuantity(newSoldQuantity);
                            ticketRepository.save(ticket);
                            System.out.println("=== DEBUG: Annulation groupe - Mise à jour de soldQuantity pour le ticket " + 
                                             rt.getTicketId() + ". Nouvelle valeur: " + newSoldQuantity);
                        });
                    }
                }

                booking.setBookingStatus(BookingStatus.CANCELLED);
                bookingRepository.save(booking);
            }
        }
    }

    public Map<String, Object> getTicketStatus(String ticketId) {
        System.out.println("=== DEBUG: Récupération du statut pour le ticket " + ticketId);
        
        // Récupérer le ticket
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket non trouvé: " + ticketId));

        // Afficher l'état actuel du ticket
        System.out.println("=== DEBUG: État actuel du ticket - Type: " + ticket.getTicketType() + 
                         ", Vendu: " + ticket.getSoldQuantity() + "/" + ticket.getInitialQuantity());

        // Récupérer toutes les réservations confirmées
        List<Booking> allConfirmedBookings = bookingRepository.findByBookingStatus(BookingStatus.CONFIRMED);
        
        // Filtrer pour ne garder que les réservations contenant le ticketId
        List<Booking> confirmedBookings = allConfirmedBookings.stream()
                .filter(booking -> booking.getTickets().stream()
                        .anyMatch(t -> t.getTicketId().equals(ticketId)))
                .collect(Collectors.toList());
                
        System.out.println("=== DEBUG: Nombre de réservations confirmées trouvées: " + confirmedBookings.size());
        
        // Calculer le nombre total de billets vendus
        int totalSold = confirmedBookings.stream()
                .flatMap(booking -> booking.getTickets().stream())
                .filter(t -> t.getTicketId().equals(ticketId))
                .mapToInt(Booking.ReservedTicket::getQuantity)
                .sum();

        System.out.println("=== DEBUG: Quantité totale vendue calculée: " + totalSold);

        // Mettre à jour la quantité vendue dans le ticket
        System.out.println("=== DEBUG: Avant mise à jour - soldQuantity: " + ticket.getSoldQuantity() + ", totalSold: " + totalSold);
        
        // S'assurer que la quantité vendue ne dépasse pas la quantité initiale
        if (totalSold > ticket.getInitialQuantity()) {
            System.out.println("=== ATTENTION: La quantité vendue (" + totalSold + ") dépasse la quantité initiale (" + ticket.getInitialQuantity() + ")");
            totalSold = ticket.getInitialQuantity();
        }
        
        ticket.setSoldQuantity(totalSold);
        
        try {
            Ticket updatedTicket = ticketRepository.save(ticket);
            System.out.println("=== DEBUG: Après save() - ID du ticket mis à jour: " + updatedTicket.getTicketId());
            System.out.println("=== DEBUG: Ticket mis à jour - Vendu: " + updatedTicket.getSoldQuantity() + "/" + updatedTicket.getInitialQuantity());
            
            // Vérifier si la mise à jour a été appliquée en rechargeant le ticket
            Ticket verifiedTicket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket non trouvé après mise à jour: " + ticketId));
            System.out.println("=== VÉRIFICATION: Quantité vendue après rechargement: " + verifiedTicket.getSoldQuantity());
            
        } catch (Exception e) {
            System.err.println("=== ERREUR lors de la mise à jour du ticket: " + e.getMessage());
            e.printStackTrace();
        }

        // Retourner les informations
        Map<String, Object> status = new HashMap<>();
        status.put("ticketId", ticketId);
        status.put("ticketType", ticket.getTicketType());
        status.put("initialQuantity", ticket.getInitialQuantity());
        status.put("soldQuantity", totalSold);
        status.put("available", ticket.getInitialQuantity() - totalSold);

        return status;
    }
}