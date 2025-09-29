package dic1.projet.trans.backend.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tickets")
public class Ticket {
    @Id
    private String ticketId;
    private String ticketType;
    private double price;
    private int initialQuantity;
    private int soldQuantity;
    private String eventId;
}