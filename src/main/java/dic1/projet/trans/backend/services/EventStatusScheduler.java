package dic1.projet.trans.backend.services;

import dic1.projet.trans.backend.entities.Event;
import dic1.projet.trans.backend.enums.EventStatus;
import dic1.projet.trans.backend.repositories.EventRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventStatusScheduler {

    private final EventRepository eventRepository;

    public EventStatusScheduler(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Scheduled(cron = "0 0 * * * *") // Exécuté toutes les heures
    @Transactional
    public void updateExpiredEventsStatus() {
        LocalDateTime now = LocalDateTime.now();
        // Récupérer les événements PUBLISHED dont la date de fin est passée
        List<Event> eventsToClose = eventRepository.findByEventStatusAndDateTimeEndBefore(EventStatus.PUBLISHED, now);
        
        // Mettre à jour le statut de chaque événement
        for (Event event : eventsToClose) {
            event.setEventStatus(EventStatus.CLOSED);
            eventRepository.save(event);
        }
        
        if (!eventsToClose.isEmpty()) {
            System.out.println("Mise à jour du statut de " + eventsToClose.size() + " événements à CLOSED");
        }
    }
}
