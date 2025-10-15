package dic1.projet.trans.backend.dtos;

import dic1.projet.trans.backend.enums.Role;
import dic1.projet.trans.backend.validators.EmailOrPhoneRequired;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;
import dic1.projet.trans.backend.utils.PhoneNumberUtils;

@Data
@EmailOrPhoneRequired
public class RegisterRequest implements AuthenticationRequest {
    @NotBlank(message = "Le prénom est obligatoire")
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire")
    private String lastName;

    @Email(message = "Email invalide")
    private String email;

    @NotBlank(message = "Le nom d'utilisateur est obligatoire")
    private String username;

    private String phoneNumber;
    
    public void setPhoneNumber(String phoneNumber) {
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            this.phoneNumber = PhoneNumberUtils.normalizePhoneNumber(phoneNumber);
        } else {
            this.phoneNumber = null;
        }
    }

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String password;

    @NotEmpty(message = "Veuillez choisir au moins un profile")
    private List<Role> roles;

    private String profilePicture;
}