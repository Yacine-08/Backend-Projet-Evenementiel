package dic1.projet.trans.backend.dtos;

import dic1.projet.trans.backend.entities.Event;
import dic1.projet.trans.backend.enums.EventStatus;
import dic1.projet.trans.backend.enums.EventType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventUpdateDTO {
    
    private String title;
    private String description;
    private String location;
    
    @Future(message = "Start date must be in the future")
    private LocalDateTime dateTimeStart;
    
    private LocalDateTime dateTimeEnd;
    private EventType typeEvent;
    private EventStatus eventStatus;
    
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacityMaximal;
    
    private String image;
    private String refundPolicy;
    
    // Méthode utilitaire pour savoir quels champs ont été modifiés
    public String getChangesSummary(Event oldEvent) {
        StringBuilder changes = new StringBuilder();
        
        if (title != null && !title.equals(oldEvent.getTitle())) {
            changes.append("Titre modifié. ");
        }
        if (description != null && !description.equals(oldEvent.getDescription())) {
            changes.append("Description mise à jour. ");
        }
        if (location != null && !location.equals(oldEvent.getLocation())) {
            changes.append("Lieu changé vers ").append(location).append(". ");
        }
        if (dateTimeStart != null && !dateTimeStart.equals(oldEvent.getDateTimeStart())) {
            changes.append("Date/heure modifiée. ");
        }
        if (capacityMaximal != null && capacityMaximal != oldEvent.getCapacityMaximal()) {
            changes.append("Capacité ajustée. ");
        }
        
        return changes.length() > 0 ? changes.toString().trim() : "Modifications générales";
    }
}