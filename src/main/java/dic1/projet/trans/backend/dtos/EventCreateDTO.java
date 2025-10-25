package dic1.projet.trans.backend.dtos;

import dic1.projet.trans.backend.enums.EventStatus;
import dic1.projet.trans.backend.enums.EventType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventCreateDTO {
    
    @NotBlank(message = "Le titre est obligatoire")
    private String title;
    
    @NotBlank(message = "La description est obligatoire")
    private String description;

    @NotBlank(message = "La catégorie est obligatoire")
    private String category;
    
    @NotBlank(message = "Le lieu est obligatoire")
    private String location;
    
    @NotNull(message = "La date de début est obligatoire")
    @Future(message = "La date de début doit être dans le futur")
    private LocalDateTime dateTimeStart;
    
    @NotNull(message = "La date de fin est obligatoire")
    private LocalDateTime dateTimeEnd;
    
    @NotBlank(message = "Le type d'événement est obligatoire")
    private EventType typeEvent;
    
    @NotNull(message = "Le statut est obligatoire")
    private EventStatus eventStatus;
    
    @NotNull(message = "La capacité maximale est obligatoire")
    @Min(value = 1, message = "La capacité doit être d'au moins 1")
    private Integer capacityMaximal;
    
    private String image;

    private String refundPolicy;
}