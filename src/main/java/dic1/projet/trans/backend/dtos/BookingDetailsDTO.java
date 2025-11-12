package dic1.projet.trans.backend.dtos;

import dic1.projet.trans.backend.entities.Booking;
import dic1.projet.trans.backend.entities.Event;
import dic1.projet.trans.backend.entities.Ticket;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDetailsDTO {
    private String bookingId;
    private String eventId;
    private String eventTitle;
    private LocalDateTime eventDate;
    private String eventLocation;
    private LocalDateTime bookingDate;
    private String status;
    private List<BookedTicketDTO> tickets;
    private double totalAmount;

    public static BookingDetailsDTO fromBookingAndEvent(Booking booking, Event event, List<Ticket> ticketDetails) {
        // Créer une map des détails des tickets pour un accès rapide
        Map<String, Ticket> ticketMap = ticketDetails.stream()
                .collect(Collectors.toMap(Ticket::getTicketId, t -> t));

        // Calculer le montant total
        double total = booking.getTickets().stream()
                .mapToDouble(t -> {
                    Ticket ticket = ticketMap.get(t.getTicketId());
                    return ticket != null ? t.getQuantity() * ticket.getPrice() : 0;
                })
                .sum();

        // Construire la liste des billets réservés avec détails
        List<BookedTicketDTO> bookedTickets = booking.getTickets().stream()
                .map(t -> {
                    Ticket ticket = ticketMap.get(t.getTicketId());
                    return BookedTicketDTO.builder()
                            .ticketId(t.getTicketId())
                            .ticketName(ticket != null ? ticket.getTicketType() : "Inconnu")
                            .quantity(t.getQuantity())
                            .unitPrice(ticket != null ? ticket.getPrice() : 0)
                            .totalPrice(ticket != null ? t.getQuantity() * ticket.getPrice() : 0)
                            .build();
                })
                .collect(Collectors.toList());

        return BookingDetailsDTO.builder()
                .bookingId(booking.getBookingId())
                .eventId(event.getIdEvent())
                .eventTitle(event.getTitle())
                .eventDate(event.getDateTimeStart())
                .eventLocation(event.getLocation())
                .bookingDate(booking.getBookingDate())
                .status(booking.getBookingStatus().toString())
                .tickets(bookedTickets)
                .totalAmount(total)
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookedTicketDTO {
        private String ticketId;
        private String ticketName;
        private int quantity;
        private double unitPrice;
        private double totalPrice;
    }
}
