package dic1.projet.trans.backend.repositories;

import dic1.projet.trans.backend.entities.Booking;
import dic1.projet.trans.backend.enums.BookingStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface BookingRepository extends MongoRepository<Booking, String> {
    @Aggregation(pipeline = {
        "{$match: {eventId: ?0, bookingStatus: 'CONFIRMED'}}",
        "{$count: 'count'}"
    })
    Long countBookingsByEventId(String eventId);
    List<Booking> findByClientId(String clientId);
    
    List<Booking> findByEventIdAndBookingStatus(String eventId, BookingStatus status);
    
    @Aggregation(pipeline = {
        // Match all confirmed bookings for the event
        "{$match: {eventId: ?0, bookingStatus: 'CONFIRMED'}}",
        // Lookup the ticket details for each booking
        "{$lookup: {" +
        "    from: 'tickets'," +
        "    localField: 'tickets.ticketId'," +
        "    foreignField: 'ticketId'," +
        "    as: 'ticketInfo'" +
        "}}",
        // Unwind the ticketInfo array (result of lookup)
        "{$unwind: '$ticketInfo'}",
        // Group by eventId and sum up the revenue (price * quantity)
        "{$group: {" +
        "    _id: '$eventId'," +
        "    totalRevenue: {$sum: {$multiply: ['$tickets.quantity', '$ticketInfo.price']}}" +
        "}}",
        // Project only the totalRevenue field
        "{$project: {" +
        "    _id: 0," +
        "    totalRevenue: 1" +
        "}}"
    })
    Double calculateTotalRevenueByEventId(String eventId);

    @Aggregation(pipeline = {
            "{$unwind: '$tickets'}",
            "{$match: {'tickets.ticketId': ?0, bookingStatus: 'CONFIRMED'}}",
            "{$group: {_id: null, total: {$sum: '$tickets.quantity'}}}"
    })
    Optional<Integer> countSoldTicketsByTicketId(String ticketId);

    List<Booking> findByGroupId(String groupId);

    List<Booking> findByClientIdOrderByBookingDateDesc(String clientId);
}