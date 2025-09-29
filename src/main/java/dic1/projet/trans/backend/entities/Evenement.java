package dic1.projet.trans.backend.entities;

import dic1.projet.trans.backend.enums.StatutEvenement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "evenements")
public class Evenement {
    @Id
    private String idEvent;
    private String titre;
    private String description;
    private String lieu;
    private LocalDateTime dateHeureDebut;
    private LocalDateTime dateHeureFin;
    private String typeEvent;
    private StatutEvenement statutEvent;
    private int capaciteMaximale;
    private LocalDateTime dateCreationEvent;
    private String image;
    private String politiqueRemboursement;
    private String organisateurId;
}