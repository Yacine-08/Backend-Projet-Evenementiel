package dic1.projet.trans.backend.repositories;

import dic1.projet.trans.backend.entities.Event;
import dic1.projet.trans.backend.enums.EventStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends MongoRepository<Event, String> {
    
    // search by title (insensible à la casse et aux accents)
    @Query("{'title': {$regex: ?0, $options: 'i'}}")
    List<Event> findByTitle(String title);
    
    // search by location (insensible à la casse et aux accents)
    @Query("{'location': {$regex: ?0, $options: 'i'}}")
    List<Event> findByLocation(String location);
    
    // search by type (insensible à la casse et aux accents)
    @Query("{'typeEvent': {$regex: ?0, $options: 'i'}}")
    List<Event> findByTypeEvent(String typeEvent);

    
    // search by event status
    @Query("{'eventStatus': {$regex: ?0, $options: 'i'}}")
    List<Event> findByEventStatus(EventStatus eventStatus);

    /**
     * Trouve les événements dont la date de début est dans la plage spécifiée
     */
    List<Event> findByDateTimeStartBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Trouve les événements actifs (non annulés) dans une plage de dates
     */
    @Query("{ 'dateTimeStart': { $gte: ?0, $lt: ?1 }, 'eventStatus': { $ne: 'CANCELLED' } }")
    List<Event> findActiveEventsBetween(LocalDateTime start, LocalDateTime end);

    List<Event> findByOrganizerIdUser(String organizerId);

}
