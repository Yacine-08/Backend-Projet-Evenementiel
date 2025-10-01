package dic1.projet.trans.backend.dtos;

import dic1.projet.trans.backend.validators.EmailOrPhoneRequired;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@EmailOrPhoneRequired
public class ForgotPasswordRequest implements AuthenticationRequest {
    @Email(message = "Email invalide")
    private String email;

    @Size(min = 9, message = "Le numéro de téléphone doit contenir au moins 9 chiffres")
    @Pattern(regexp = "^[0-9+().\\s-]*$", message = "Numéro de téléphone invalide")
    private String phoneNumber;
}