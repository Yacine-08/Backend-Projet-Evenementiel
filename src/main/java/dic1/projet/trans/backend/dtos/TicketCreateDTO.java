package dic1.projet.trans.backend.dtos;

import dic1.projet.trans.backend.validators.ValidTicketPrice;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TicketCreateDTO {
    @NotBlank(message = "Le type de billet est requis")
    private String ticketType;

    private String description;

    @Min(value = 0, message = "Le prix ne peut pas être négatif")
    @ValidTicketPrice
    private double price;

    @Min(value = 1, message = "La quantité initiale doit être d'au moins 1")
    private int initialQuantity;
}
