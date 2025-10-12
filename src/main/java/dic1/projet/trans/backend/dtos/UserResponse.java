package dic1.projet.trans.backend.dtos;

import dic1.projet.trans.backend.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private String idUser;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private LocalDateTime inscriptionDate;
    private String profilePhoto;
    private List<Role> roles;

}
