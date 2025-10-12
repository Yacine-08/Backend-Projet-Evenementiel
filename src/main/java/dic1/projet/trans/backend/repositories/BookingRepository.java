package dic1.projet.trans.backend.repositories;

import dic1.projet.trans.backend.entities.Booking;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends MongoRepository<Booking, String> {
    List<Booking> findByClientId(String clientId);
}