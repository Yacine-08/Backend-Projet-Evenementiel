package dic1.projet.trans.backend.dtos;

import dic1.projet.trans.backend.validators.EmailOrPhoneRequired;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import dic1.projet.trans.backend.utils.PhoneNumberUtils;

@Data
@EmailOrPhoneRequired
public class LoginRequest implements AuthenticationRequest {
    @Email(message = "Email invalide")
    private String email;

    private String phoneNumber;
    
    public void setPhoneNumber(String phoneNumber) {
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            this.phoneNumber = PhoneNumberUtils.normalizePhoneNumber(phoneNumber);
        } else {
            this.phoneNumber = null;
        }
    }

    @NotBlank(message = "Le mot de passe est obligatoire")
    private String password;
}