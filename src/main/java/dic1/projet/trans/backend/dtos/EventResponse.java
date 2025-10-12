package dic1.projet.trans.backend.dtos;

import dic1.projet.trans.backend.enums.EventStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventResponse {
    private String idEvent;
    private String title;
    private String description;
    private String location;
    private LocalDateTime dateTimeStart;
    private LocalDateTime dateTimeEnd;
    private String typeEvent;
    private EventStatus eventStatus;
    private int capacityMaximal;
    private int currentParticipants;
    private boolean isFull;
    private LocalDateTime creationDateTime;
    private UserResponse organizer;
    private String image;
    private String refundPolicy;
}