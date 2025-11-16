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
        System.out.println("=== STATS: Computing organizer stats for userId=" + organizerId + ", eventsFound=" + (events != null ? events.size() : 0));

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
            List<Booking> allBookings = bookingRepository.findByEventId(eventId);
            long confirmedCount = allBookings.stream().filter(b -> b.getBookingStatus() == BookingStatus.CONFIRMED).count();
            long pendingCount = allBookings.stream().filter(b -> b.getBookingStatus() == BookingStatus.PENDING).count();
            long cancelledCount = allBookings.stream().filter(b -> b.getBookingStatus() == BookingStatus.CANCELLED).count();

            totalReservations += (confirmedCount + pendingCount);
            cancelledReservations += cancelledCount;

            double eventRevenue = allBookings.stream()
                    .filter(b -> b.getBookingStatus() != BookingStatus.CANCELLED)
                    .mapToDouble(Booking::getTotalAmount)
                    .sum();
            totalRevenueDouble += eventRevenue;

            System.out.println("=== STATS: Event=" + eventId + " title='" + event.getTitle() + "' confirmed=" + confirmedCount + ", pending=" + pendingCount + ", cancelled=" + cancelledCount + ", eventRevenue=" + eventRevenue);

            Integer capacity = event.getCapacityMaximal();
            if (capacity != null && capacity > 0) {
                List<Booking> confirmedBookings = allBookings.stream()
                        .filter(b -> b.getBookingStatus() == BookingStatus.CONFIRMED)
                        .toList();
                int ticketsSold = confirmedBookings.stream()
                        .flatMap(b -> b.getTickets().stream())
                        .mapToInt(Booking.ReservedTicket::getQuantity)
                        .sum();
                fillRateSum += ((double) ticketsSold / (double) capacity);
                fillRateEventCount++;
            }
        }

        double averageFillRate = fillRateEventCount > 0 ? (fillRateSum / fillRateEventCount) : 0.0;
        System.out.println("=== STATS: Organizer=" + organizerId + " totals -> activeEvents=" + activeEventsCount + ", totalReservations=" + totalReservations + ", cancelledReservations=" + cancelledReservations + ", totalRevenue=" + totalRevenueDouble + ", avgFillRate=" + averageFillRate);

        return new OrganizerStatsResponse(
                activeEventsCount,
                totalReservations,
                cancelledReservations,
                BigDecimal.valueOf(totalRevenueDouble),
                averageFillRate
        );
    }
}