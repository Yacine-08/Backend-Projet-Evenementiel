package dic1.projet.trans.backend.repositories;

import dic1.projet.trans.backend.entities.Booking;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends MongoRepository<Booking, String> {
    List<Booking> findByClientId(String clientId);
    
    @Aggregation(pipeline = {
        "{$unwind: '$tickets'}",
        "{$lookup: {" +
        "    from: 'tickets'," +
        "    localField: 'tickets.ticketId'," +
        "    foreignField: '_id'," +
        "    as: 'ticketInfo'" +
        "}}",
        "{$unwind: '$ticketInfo'}",
        "{$match: {'ticketInfo.eventId': ?0}}",
        "{$group: {" +
        "    _id: null," +
        "    totalRevenue: {$sum: {$multiply: ['$tickets.quantity', '$ticketInfo.price']}}" +
        "}}"
    })
    Optional<Double> calculateTotalRevenueByEventId(String eventId);
}