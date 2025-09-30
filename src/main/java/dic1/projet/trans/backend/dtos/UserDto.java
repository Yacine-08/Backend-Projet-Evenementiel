package dic1.projet.trans.backend.dtos;

import lombok.Data;

@Data
public class UserDto {
    private String idUser;
    private String firstName;
    private String lastName;
    private String username;
    private String email;
    private String phoneNumber;
    private String profilePhoto;
    private String role;
}