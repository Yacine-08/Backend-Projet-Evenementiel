package dic1.projet.trans.backend.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    private String Email;

    String phoneNumber;

    @NotBlank(message = "Le mot de passe est obligatoire")
    private String password;
}