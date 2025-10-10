package dic1.projet.trans.backend.dtos;

import dic1.projet.trans.backend.enums.EventStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateEventRequest {
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

    @NotBlank(message = "Organizer ID is required")
    private String organizerId;
    
    private String image;
    private String refundPolicy;
}
