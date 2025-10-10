package dic1.projet.trans.backend.dtos;

import dic1.projet.trans.backend.enums.Role;
import dic1.projet.trans.backend.validators.EmailOrPhoneRequired;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
@EmailOrPhoneRequired
public class RegisterRequest implements AuthenticationRequest {
    @NotBlank(message = "Le prénom est obligatoire")
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire")
    private String lastName;

    @Email(message = "Email invalide")
    private String email;

    @Size(min = 9, message = "Le numéro de téléphone doit contenir au moins 9 chiffres")
    @Pattern(regexp = "^[0-9+().\\s-]*$", message = "Numéro de téléphone invalide")
    private String phoneNumber;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String password;

    @NotEmpty(message = "Veuillez choisir au moins un profile")
    private List<Role> roles;

    private String profilePicture;
}