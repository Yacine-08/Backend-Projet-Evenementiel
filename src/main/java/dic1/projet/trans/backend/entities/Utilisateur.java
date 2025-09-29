package dic1.projet.trans.backend.entities;

import dic1.projet.trans.backend.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "utilisateurs")
public class Utilisateur {
    @Id
    private String idUser;
    private String prenom;
    private String nom;
    private String username;
    private String email;
    private String telephone;
    private String motDePasse;
    private LocalDateTime dateInscription;
    private String photoProfil;
    private Role role;
}