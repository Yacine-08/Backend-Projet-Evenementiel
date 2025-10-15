package dic1.projet.trans.backend.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import dic1.projet.trans.backend.utils.PhoneNumberUtils;

@Data
public class VerifyOtpRequest {

    @Email
    private String email;

    @NotBlank(message = "Le code OTP est obligatoire")
    private String code;

    private String phoneNumber;
    
    public void setPhoneNumber(String phoneNumber) {
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            this.phoneNumber = PhoneNumberUtils.normalizePhoneNumber(phoneNumber);
        } else {
            this.phoneNumber = null;
        }
    }

}