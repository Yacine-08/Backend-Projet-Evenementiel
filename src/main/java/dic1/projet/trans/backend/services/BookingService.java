package dic1.projet.trans.backend.services;

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

    @Transactional
    public List<Booking> createBooking(String userId, CreateBookingRequest request) {
        if (request.getTickets() == null || request.getTickets().isEmpty()) {
            throw new BadRequestException("Aucun ticket fourni pour la réservation");
        }

        // Vérifier que l'événement existe
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Événement non trouvé: " + request.getEventId()));

        List<Booking> createdBookings = new ArrayList<>();
        String groupId = UUID.randomUUID().toString(); // Générer un ID de groupe unique

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

            // Créer une réservation pour chaque quantité de ticket
            for (int i = 0; i < rt.getQuantity(); i++) {
                Booking booking = new Booking();
                booking.setGroupId(groupId);  // Même groupId pour toutes les réservations du même groupe
                booking.setTotalAmount(ticket.getPrice());
                booking.setBookingDate(LocalDateTime.now());
                booking.setPaymentMethod(request.getPaymentMethod());
//                booking.setBookingStatus(BookingStatus.PENDING);
                booking.setBookingStatus(BookingStatus.CONFIRMED);
                booking.setClientId(userId);
                booking.setEventId(request.getEventId());

                // Un seul ticket par réservation
                List<Booking.ReservedTicket> reservedTickets = new ArrayList<>();
                reservedTickets.add(new Booking.ReservedTicket(ticket.getTicketId(), 1));
                booking.setTickets(reservedTickets);

                createdBookings.add(bookingRepository.save(booking));
            }
        }

        return createdBookings;
    }
    
    /**
     * Confirme une réservation et met à jour les quantités vendues
     * @param bookingId ID de la réservation à confirmer
     * @return La réservation confirmée
     */
    @Transactional
    public Booking confirmBooking(String bookingId) {
        System.out.println("=== DEBUG: Confirming booking: " + bookingId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation non trouvée"));

        if (booking.getBookingStatus() == BookingStatus.CONFIRMED) {
            return booking;
        }

        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Impossible de confirmer une réservation annulée");
        }

        // Calculer le nombre total de billets vendus pour chaque ticket
        Map<String, Integer> ticketQuantities = new HashMap<>();
        for (Booking.ReservedTicket rt : booking.getTickets()) {
            ticketQuantities.merge(rt.getTicketId(), rt.getQuantity(), Integer::sum);
        }

        // Vérifier la disponibilité
        for (Map.Entry<String, Integer> entry : ticketQuantities.entrySet()) {
            String ticketId = entry.getKey();
            int quantity = entry.getValue();

            // Calculer le nombre de billets déjà vendus
            int alreadySold = bookingRepository.countSoldTicketsByTicketId(ticketId).orElse(0);
            Ticket ticket = ticketRepository.findById(ticketId)
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket non trouvé: " + ticketId));

            int available = ticket.getInitialQuantity() - alreadySold;
            if (quantity > available) {
                throw new BadRequestException(
                        String.format("Stock insuffisant pour le ticket %s. Disponible: %d, Demandé: %d",
                                ticketId, available, quantity)
                );
            }
        }

        // Mettre à jour le statut de la réservation
        booking.setBookingStatus(BookingStatus.CONFIRMED);

        System.out.println("=== DEBUG: Réservation confirmée avec succès");
        return booking;
    }

    public List<Booking> getBookingsForUser(String userId) {
        // Retourne directement la liste des réservations triées par date décroissante
        return bookingRepository.findByClientIdOrderByBookingDateDesc(userId);
    }

    public Booking getBookingByIdOwned(String bookingId, String userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation non trouvée"));
        if (!booking.getClientId().equals(userId)) {
            throw new BadRequestException("Vous n'êtes pas propriétaire de cette réservation");
        }
        return booking;
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
            Booking.ReservedTicket rt = booking.getTickets().get(0);
            Ticket ticket = ticketRepository.findById(rt.getTicketId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket non trouvé: " + rt.getTicketId()));

            int newSoldQuantity = Math.max(0, ticket.getSoldQuantity() - 1); // On décrémente de 1 car une réservation = 1 ticket
            ticket.setSoldQuantity(newSoldQuantity);
            ticketRepository.save(ticket);
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
                    // Libérer les billets réservés
                    Booking.ReservedTicket rt = booking.getTickets().get(0);
                    Ticket ticket = ticketRepository.findById(rt.getTicketId())
                            .orElseThrow(() -> new ResourceNotFoundException("Ticket non trouvé: " + rt.getTicketId()));

                    int newSoldQuantity = Math.max(0, ticket.getSoldQuantity() - 1);
                    ticket.setSoldQuantity(newSoldQuantity);
                    ticketRepository.save(ticket);
                }

                booking.setBookingStatus(BookingStatus.CANCELLED);
                bookingRepository.save(booking);
            }
        }
    }

    public Map<String, Object> getTicketStatus(String ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket non trouvé: " + ticketId));

        int totalSold = bookingRepository.countSoldTicketsByTicketId(ticketId).orElse(0);

        Map<String, Object> status = new HashMap<>();
        status.put("ticketId", ticketId);
        status.put("initialQuantity", ticket.getInitialQuantity());
        status.put("soldQuantity", totalSold); // Utiliser le comptage réel
        status.put("available", ticket.getInitialQuantity() - totalSold);

        return status;
    }
}