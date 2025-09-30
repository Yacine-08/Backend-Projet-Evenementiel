package dic1.projet.trans.backend.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "otp_codes")
public class OtpCode {
    @Id
    private String id;
    private String email;
    private String phoneNumber;
    private String code;
    private LocalDateTime expiryDate;
    // true if the OTP code is used
    private boolean verified;
    // number of attempts to use the OTP code
    private int attempts = 3;
}