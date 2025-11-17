package dic1.projet.trans.backend.dtos;

import dic1.projet.trans.backend.enums.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentEventDTO {
    private String idEvent;
    private String title;
    private LocalDateTime dateTimeStart;
    private Integer capacityMaximal;
    private EventStatus eventStatus;
    private long reservationsSeats;
    private double revenue;
}