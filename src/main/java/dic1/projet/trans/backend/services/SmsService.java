package dic1.projet.trans.backend.services;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SmsService {

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.phone.number}")
    private String fromPhoneNumber;

    public void sendOtpSms(String toPhoneNumber, String otpCode) {
        try {
            Twilio.init(accountSid, authToken);
            Message message = Message.creator(
                    new PhoneNumber(toPhoneNumber),
                    new PhoneNumber(fromPhoneNumber),
                    "Votre code de vérification est: " + otpCode
            ).create();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'envoi du SMS: " + e.getMessage());
        }
    }

    public void sendPasswordResetSms(String toPhoneNumber, String resetToken, String firstName) {
        try {
            Twilio.init(accountSid, authToken);
            String messageBody = String.format(
                "Bonjour %s, voici votre lien de réinitialisation de mot de passe : http://votresite.com/reset-password?token=%s",
                firstName,
                resetToken
            );
            
            Message message = Message.creator(
                    new PhoneNumber(toPhoneNumber),
                    new PhoneNumber(fromPhoneNumber),
                    messageBody
            ).create();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'envoi du SMS de réinitialisation: " + e.getMessage());
        }
    }
}
