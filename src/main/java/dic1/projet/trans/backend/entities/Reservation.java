package dic1.projet.trans.backend.entities;

import dic1.projet.trans.backend.enums.StatutReservation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "reservations")
public class Reservation {
    @Id
    private String idReservation;
    private double montantTotal;
    private LocalDateTime dateReservation;
    private String moyenPaiement;
    private StatutReservation statutReservation;
    private String clientId;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BilletReserve {
        private String billetId;
        private int quantite;
    }
    
    private List<BilletReserve> billets;
}