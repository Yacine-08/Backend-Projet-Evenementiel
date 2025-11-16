package dic1.projet.trans.backend.services;

import dic1.projet.trans.backend.dtos.OrganizerStatsResponse;
import dic1.projet.trans.backend.entities.Booking;
import dic1.projet.trans.backend.entities.Event;
import dic1.projet.trans.backend.enums.BookingStatus;
import dic1.projet.trans.backend.repositories.BookingRepository;
import dic1.projet.trans.backend.repositories.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizerStatsService {

    private final EventRepository eventRepository;
    private final BookingRepository bookingRepository;

    public OrganizerStatsResponse getStatsForOrganizer(String organizerId) {
        List<Event> events = eventRepository.findByOrganizerIdUser(organizerId);

        int activeEventsCount = (int) events.stream()
                .filter(e -> e.getEventStatus() != null && !"CANCELLED".equals(e.getEventStatus().name()))
                .count();

        long totalReservations = 0L;
        long cancelledReservations = 0L;
        double totalRevenueDouble = 0.0;

        double fillRateSum = 0.0;
        int fillRateEventCount = 0;

        for (Event event : events) {
            String eventId = event.getIdEvent();

            List<Booking> confirmed = bookingRepository.findByEventIdAndBookingStatus(eventId, BookingStatus.CONFIRMED);
            List<Booking> cancelled = bookingRepository.findByEventIdAndBookingStatus(eventId, BookingStatus.CANCELLED);

            totalReservations += confirmed.size();
            cancelledReservations += cancelled.size();

            Double revenue = bookingRepository.calculateTotalRevenueByEventId(eventId);
            if (revenue != null) {
                totalRevenueDouble += revenue;
            }

            Integer capacity = event.getCapacityMaximal();
            if (capacity != null && capacity > 0) {
                int ticketsSold = confirmed.stream()
                        .flatMap(b -> b.getTickets().stream())
                        .mapToInt(Booking.ReservedTicket::getQuantity)
                        .sum();
                fillRateSum += ((double) ticketsSold / (double) capacity);
                fillRateEventCount++;
            }
        }

        double averageFillRate = fillRateEventCount > 0 ? (fillRateSum / fillRateEventCount) : 0.0;

        return new OrganizerStatsResponse(
                activeEventsCount,
                totalReservations,
                cancelledReservations,
                BigDecimal.valueOf(totalRevenueDouble),
                averageFillRate
        );
    }
}