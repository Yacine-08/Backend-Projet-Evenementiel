package dic1.projet.trans.backend.dtos;

import dic1.projet.trans.backend.entities.Booking;
import dic1.projet.trans.backend.entities.Event;
import dic1.projet.trans.backend.entities.Ticket;
import lombok.Data;

import java.util.List;

@Data
public class EventDetailsDTO {
    private Event event;
    private List<Ticket> tickets;
    private List<Booking> bookings;
    private int totalTicketsSold;
    private double totalRevenue;

    public EventDetailsDTO(Event event, List<Ticket> tickets, List<Booking> bookings) {
        this.event = event;
        this.tickets = tickets;
        this.bookings = bookings;
        this.totalTicketsSold = calculateTotalTicketsSold(bookings);
        this.totalRevenue = calculateTotalRevenue(bookings);
    }

    private int calculateTotalTicketsSold(List<Booking> bookings) {
        return bookings.stream()
                .filter(b -> b.getTickets() != null)
                .flatMap(b -> b.getTickets().stream())
                .mapToInt(Booking.ReservedTicket::getQuantity)
                .sum();
    }

    private double calculateTotalRevenue(List<Booking> bookings) {
        return bookings.stream()
                .mapToDouble(Booking::getTotalAmount)
                .sum();
    }
}
