package dic1.projet.trans.backend.entities;

import dic1.projet.trans.backend.enums.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "events")
public class Event {
    @Id
    private String idEvent;
    private String title;
    private String description;
    private String location;
    private LocalDateTime dateTimeStart;
    private LocalDateTime dateTimeEnd;
    private String typeEvent;
    private EventStatus eventStatus;
    private int capacityMaximal;
    private LocalDateTime creationDateTime;
    private String image;
    private String refundPolicy;
    private String organizer_id;
}