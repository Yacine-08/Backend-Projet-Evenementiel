package dic1.projet.trans.backend.entities;

import dic1.projet.trans.backend.enums.EventStatus;
import dic1.projet.trans.backend.enums.EventType;
import jakarta.validation.constraints.*;
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

    @NotBlank(message = "Event's category is required")
    private String category;

    @NotBlank(message = "Location is required")
    private String location;

    private String address;

    @NotNull(message = "Start date is required")
    @Future(message = "Start date must be in the future")
    private LocalDateTime dateTimeStart;

    @NotNull(message = "End date is required")
    private LocalDateTime dateTimeEnd;

    @NotNull(message = "Type event is required")
    private EventType typeEvent;

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

    @AssertTrue(message = "Si le remboursement est activé, le délai et la politique de remboursement doivent être spécifiés")
    public boolean isRefundConfigValid() {
        // Si le remboursement est désactivé, on ne valide pas les autres champs
        if (!refundEnabled) {
            return true;
        }
        
        // Si le remboursement est activé, on vérifie que les champs requis sont présents
        boolean hasValidDeadline = refundDeadlineDays != null && refundDeadlineDays >= 0;
        boolean hasValidPolicy = refundPolicy != null && !refundPolicy.trim().isEmpty();
        
        return hasValidDeadline && hasValidPolicy;
    }
    
    private String refundPolicy;
    
    private boolean refundEnabled = false;
    
    @Min(value = 0, message = "Le délai de remboursement ne peut pas être négatif")
    private Integer refundDeadlineDays;

    private String refundConditions;

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
    
    public boolean isPaidEvent() {
        return EventType.PAID.equals(this.typeEvent);
    }
    
    public boolean isRefundAllowed() {
        if (!refundEnabled) {
            return false;
        }
        
        if (refundDeadlineDays == null || refundDeadlineDays < 0) {
            return false;
        }
        
        LocalDateTime refundDeadline = dateTimeStart.minusDays(refundDeadlineDays);
        return LocalDateTime.now().isBefore(refundDeadline);
    }

}