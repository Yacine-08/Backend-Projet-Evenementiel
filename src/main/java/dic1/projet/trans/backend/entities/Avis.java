package dic1.projet.trans.backend.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "avis")
public class Avis {
    @Id
    private String idAvis;
    private int note;
    private String commentaire;
    private String clientId;
    private String evenementId;
}