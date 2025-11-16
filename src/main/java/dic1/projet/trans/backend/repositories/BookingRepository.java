package dic1.projet.trans.backend.repositories;

import dic1.projet.trans.backend.entities.Booking;
import dic1.projet.trans.backend.enums.BookingStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.Query;
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
    
    List<Booking> findByEventId(String eventId);
    
    List<Booking> findByEventIdAndBookingStatus(String eventId, BookingStatus status);
    List<Booking> findByClientIdAndEventIdAndBookingStatus(String clientId, String eventId, BookingStatus status);
    List<Booking> findByEventIdInOrderByBookingDateDesc(List<String> eventIds);
    List<Booking> findByEventIdInAndBookingStatusOrderByBookingDateDesc(List<String> eventIds, BookingStatus status);
    
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
    
    // Trouver la première réservation d'un groupe
    Optional<Booking> findFirstByGroupId(String groupId);

    @Query("{'tickets': {$elemMatch: {'ticketId': ?0}}, 'bookingStatus': ?1}")
    List<Booking> findByTicketIdAndBookingStatus(String ticketId, BookingStatus status);
    
    @Aggregation(pipeline = {
            "{$match: {bookingStatus: ?1, 'tickets.ticketId': ?0}}",
            "{$group: {_id: '$_id'}}",
            "{$count: 'count'}"
    })
    Optional<Integer> countByTickets_TicketIdAndBookingStatus(String ticketId, BookingStatus status);
    
    @Aggregation(pipeline = {
            "{$unwind: '$tickets'}",
            "{$match: {'tickets.ticketId': ?0, bookingStatus: 'CONFIRMED'}}",
            "{$group: {_id: null, total: {$sum: '$tickets.quantity'}}}"
    })
    Optional<Integer> countSoldTicketsByTicketId(String ticketId);

    List<Booking> findByGroupId(String groupId);

    List<Booking> findByClientIdOrderByBookingDateDesc(String clientId);
    
    List<Booking> findByBookingStatus(BookingStatus status);
    
    // Interface pour le résultat de l'agrégation des ventes de billets
    interface TicketSales {
        String getTicketId();
        Integer getTotalSold();
    }
    
    @Aggregation(pipeline = {
        "{$match: {bookingStatus: 'CONFIRMED'}}",
        "{$unwind: '$tickets'}",
        "{$group: {_id: '$tickets.ticketId', totalSold: {$sum: '$tickets.quantity'}}}"
    })
    List<TicketSales> findConfirmedTicketsCount();

    List<Booking> findByGroupIdAndBookingStatusNot(String groupId, BookingStatus status);
}