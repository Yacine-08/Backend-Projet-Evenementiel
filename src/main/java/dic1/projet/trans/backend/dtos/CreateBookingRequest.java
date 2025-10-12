package dic1.projet.trans.backend.dtos;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CreateBookingRequest {
    private String paymentMethod;

    @NotEmpty(message = "Au moins un ticket doit être réservé")
    private List<ReservedTicketRequest> tickets;
}