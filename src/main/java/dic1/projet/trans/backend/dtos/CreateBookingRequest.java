package dic1.projet.trans.backend.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateBookingRequest {
    @NotBlank(message = "L'ID de l'événement est requis")
    private String eventId;
    
    @NotBlank(message = "Le mode de paiement est requis")
    private String paymentMethod;

    @NotEmpty(message = "Au moins un ticket doit être réservé")
    private List<ReservedTicketRequest> tickets;
}