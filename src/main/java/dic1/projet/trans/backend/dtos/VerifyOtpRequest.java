package dic1.projet.trans.backend.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyOtpRequest {

    @Email
    private String email;

    @NotBlank(message = "Le code OTP est obligatoire")
    private String code;

    String phoneNumber;

}