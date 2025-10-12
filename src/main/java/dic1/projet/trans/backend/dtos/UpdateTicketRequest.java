package dic1.projet.trans.backend.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UpdateTicketRequest {
    private String ticketType; // null = inchangé

    @DecimalMin(value = "0.0", inclusive = false, message = "Le prix doit être > 0")
    private Double price; // null = inchangé

    @Min(value = 1, message = "La quantité initiale doit être au moins 1")
    private Integer initialQuantity; // null = inchangé
}