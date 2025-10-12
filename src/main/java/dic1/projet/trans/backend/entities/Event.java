package dic1.projet.trans.backend.entities;

import dic1.projet.trans.backend.enums.EventStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "events")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Event {
    @Id
    @EqualsAndHashCode.Include
    private String idEvent;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Location is required")
    private String location;

    @NotNull(message = "Start date is required")
    @Future(message = "Start date must be in the future")
    private LocalDateTime dateTimeStart;

    @NotNull(message = "End date is required")
    private LocalDateTime dateTimeEnd;

    @NotBlank(message = "Type event is required")
    private String typeEvent;

    @NotNull(message = "Event status is required")
    private EventStatus eventStatus;

    @Min(value = 1, message = "Capacity must be at least 1")
    private int capacityMaximal;

    private LocalDateTime creationDateTime = LocalDateTime.now();

    @NotNull(message = "Organizer is required")
    @DBRef(lazy = true)
    private User organizer;

    // Liste des participants (utilisateurs ayant réservé)
    @DBRef(lazy = true)
    private List<User> participants = new ArrayList<>();

    private String image;
    private String refundPolicy;

    // Méthodes utilitaires
    public void addParticipant(User user) {
        if (participants == null) {
            participants = new ArrayList<>();
        }
        if (!participants.contains(user)) {
            participants.add(user);
        }
    }

    public int getCurrentParticipantCount() {
        return participants != null ? participants.size() : 0;
    }

    public boolean isFull() {
        return getCurrentParticipantCount() >= capacityMaximal;
    }

}