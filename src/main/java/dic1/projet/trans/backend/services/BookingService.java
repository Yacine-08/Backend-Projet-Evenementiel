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
                booking.setBookingStatus(BookingStatus.PENDING);
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
                    
            // Compter les réservations confirmées pour ce ticket
            int alreadySold = bookingRepository.countByTickets_TicketIdAndBookingStatus(
                rt.getTicketId(), BookingStatus.CONFIRMED).orElse(0);
                
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
                // Compter les réservations confirmées existantes
                int totalSold = bookingRepository.countByTickets_TicketIdAndBookingStatus(
                    rt.getTicketId(), BookingStatus.CONFIRMED).orElse(0);
                
                // Ajouter la quantité de la réservation actuelle
                totalSold += rt.getQuantity();
                
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