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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final NotificationService notificationService;

    public Booking createBooking(String userId, CreateBookingRequest request) {
        if (request.getTickets() == null || request.getTickets().isEmpty()) {
            throw new BadRequestException("Aucun ticket fourni pour la réservation");
        }

        double totalAmount = 0.0;
        List<Booking.ReservedTicket> reservedTickets = new ArrayList<>();

        // Vérifier disponibilités et calculer le montant
        for (ReservedTicketRequest rt : request.getTickets()) {
            Ticket ticket = ticketRepository.findById(rt.getTicketId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket non trouvé: " + rt.getTicketId()));

            int available = ticket.getInitialQuantity() - ticket.getSoldQuantity();
            if (rt.getQuantity() > available) {
                throw new BadRequestException("Quantité demandée supérieure au stock disponible pour le ticket: " + rt.getTicketId());
            }

            totalAmount += ticket.getPrice() * rt.getQuantity();
            reservedTickets.add(new Booking.ReservedTicket(ticket.getTicketId(), rt.getQuantity()));
        }

        // Mettre à jour les tickets vendus
        for (ReservedTicketRequest rt : request.getTickets()) {
            Ticket ticket = ticketRepository.findById(rt.getTicketId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket non trouvé: " + rt.getTicketId()));
            ticket.setSoldQuantity(ticket.getSoldQuantity() + rt.getQuantity());
            ticketRepository.save(ticket);
        }

        // Créer et sauver la réservation
        Booking booking = new Booking();
        booking.setTotalAmount(totalAmount);
        booking.setBookingDate(LocalDateTime.now());
        booking.setPaymentMethod(request.getPaymentMethod());
        booking.setBookingStatus(BookingStatus.CONFIRMED); 
        booking.setClientId(userId);
        booking.setTickets(reservedTickets);

        Booking savedBooking = bookingRepository.save(booking);
        
        // Récupérer l'événement associé au premier ticket pour envoyer une notification à l'organisateur
        if (!reservedTickets.isEmpty()) {
            String ticketId = reservedTickets.get(0).getTicketId();
            Ticket firstTicket = ticketRepository.findById(ticketId)
                    .orElseThrow(() -> new ResourceNotFoundException("Ticket non trouvé: " + ticketId));
                    
            // Récupérer l'événement associé au ticket
            Event event = eventRepository.findById(firstTicket.getEventId())
                    .orElseThrow(() -> new ResourceNotFoundException("Événement non trouvé pour le ticket: " + ticketId));
            
            // Envoyer une notification à l'organisateur
            notificationService.notifyNewBooking(event, savedBooking.getBookingId());
        }

        return savedBooking;
    }

    public List<Booking> getBookingsForUser(String userId) {
        return bookingRepository.findByClientId(userId);
    }

    public Booking getBookingByIdOwned(String bookingId, String userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation non trouvée"));
        if (!booking.getClientId().equals(userId)) {
            throw new BadRequestException("Vous n'êtes pas propriétaire de cette réservation");
        }
        return booking;
    }

    public Booking cancelBooking(String bookingId, String userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation non trouvée"));

        if (!booking.getClientId().equals(userId)) {
            throw new BadRequestException("Vous n'êtes pas propriétaire de cette réservation");
        }
        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("La réservation est déjà annulée");
        }

        // Réajuster les quantités vendues
        if (booking.getTickets() != null) {
            for (Booking.ReservedTicket rt : booking.getTickets()) {
                Ticket ticket = ticketRepository.findById(rt.getTicketId())
                        .orElseThrow(() -> new ResourceNotFoundException("Ticket non trouvé: " + rt.getTicketId()));
                int newSold = Math.max(0, ticket.getSoldQuantity() - rt.getQuantity());
                ticket.setSoldQuantity(newSold);
                ticketRepository.save(ticket);
            }
        }

        booking.setBookingStatus(BookingStatus.CANCELLED);
        return bookingRepository.save(booking);
    }
}