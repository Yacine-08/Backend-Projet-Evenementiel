package dic1.projet.trans.backend.repositories;

import dic1.projet.trans.backend.entities.Ticket;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends MongoRepository<Ticket, String> {
    List<Ticket> findByEventId(String eventId);
}