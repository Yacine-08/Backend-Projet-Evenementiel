package dic1.projet.trans.backend.entities;

import dic1.projet.trans.backend.validators.ValidTicketPrice;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tickets")
public class Ticket {
    @Id
    private String ticketId;

    @NotBlank(message = "Le type de billet est requis")
    private String ticketType;

    private String description;

    @Min(value = 0, message = "Le prix ne peut pas être négatif")
    @ValidTicketPrice
    private double price;

    @Min(value = 1, message = "La quantité initiale doit être d'au moins 1")
    private int initialQuantity;

    private int soldQuantity = 0;

    @NotNull(message = "L'événement est requis")
    private String eventId;


}