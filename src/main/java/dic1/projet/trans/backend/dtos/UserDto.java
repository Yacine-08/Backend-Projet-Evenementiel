package dic1.projet.trans.backend.dtos;

import dic1.projet.trans.backend.enums.Role;
import lombok.Data;

import java.util.List;

@Data
public class UserDto {
    private String idUser;
    private String firstName;
    private String lastName;
    private String username;
    private String email;
    private String phoneNumber;
    private String profilePhoto;
    private List<String> roles;
}