package dic1.projet.trans.backend.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateTicketRequest {
    @NotBlank(message = "Le type de billet est requis")
    private String ticketType;

    @DecimalMin(value = "0.0", inclusive = false, message = "Le prix doit être > 0")
    private double price;

    @Min(value = 1, message = "La quantité initiale doit être au moins 1")
    private int initialQuantity;
}