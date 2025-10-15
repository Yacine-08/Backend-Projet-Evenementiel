package dic1.projet.trans.backend.dtos;

import lombok.Data;
import dic1.projet.trans.backend.utils.PhoneNumberUtils;

@Data
public class ResendOtpRequest {
    private String email;
    private String phoneNumber;
    
    public void setPhoneNumber(String phoneNumber) {
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            this.phoneNumber = PhoneNumberUtils.normalizePhoneNumber(phoneNumber);
        } else {
            this.phoneNumber = null;
        }
    }
}
