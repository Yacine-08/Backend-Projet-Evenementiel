package dic1.projet.trans.backend.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReservedTicketRequest {
    @NotBlank(message = "ticketId requis")
    private String ticketId;

    @Min(value = 1, message = "La quantité doit être au moins 1")
    private int quantity;
}