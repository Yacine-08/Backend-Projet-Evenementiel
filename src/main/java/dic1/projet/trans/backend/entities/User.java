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
@Document(collection = "users")
public class User {
    @Id
    private String idUser;
    private String firstName;
    private String lastName;
    private String username;
    private String email;
    private String phoneNumber;
    private String password;
    private LocalDateTime inscriptionDate;
    private String profilePhoto;
    private Role role;
}