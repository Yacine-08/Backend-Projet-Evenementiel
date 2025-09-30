package dic1.projet.trans.backend.dtos;

import lombok.Data;

@Data
public class ResendOtpRequest {
    private String email;
    private String phoneNumber;
}
