package dic1.projet.trans.backend.services;

import dic1.projet.trans.backend.entities.Event;
import dic1.projet.trans.backend.entities.User;
import dic1.projet.trans.backend.repositories.EventRepository;
import dic1.projet.trans.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class UserFavoriteService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    public void addFavorite(String userId, String eventId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        user.addFavorite(event);
        userRepository.save(user);
    }

    public void removeFavorite(String userId, String eventId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        user.removeFavorite(event);
        userRepository.save(user);
    }

    public List<Event> getUserFavorites(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getFavoriteEvents();
    }

    public boolean isEventInFavorites(String userId, String eventId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        return user.hasFavorite(event);
    }

}
