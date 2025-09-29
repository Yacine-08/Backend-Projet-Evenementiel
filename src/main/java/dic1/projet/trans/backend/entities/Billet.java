package dic1.projet.trans.backend.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "billets")
public class Billet {
    @Id
    private String idBillet;
    private String typeBillet;
    private double prix;
    private int quantiteInitiale;
    private int quantiteVendue;
    private String evenementId;
}