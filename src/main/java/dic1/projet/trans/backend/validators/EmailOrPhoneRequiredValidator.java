package dic1.projet.trans.backend.validators;

import dic1.projet.trans.backend.dtos.AuthenticationRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EmailOrPhoneRequiredValidator implements ConstraintValidator<EmailOrPhoneRequired, AuthenticationRequest> {

    @Override
    public boolean isValid(AuthenticationRequest request, ConstraintValidatorContext context) {
        boolean hasEmail = request.getEmail() != null && !request.getEmail().isBlank();
        boolean hasPhone = request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank();
        
        if (!hasEmail && !hasPhone) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("L'email ou le numéro de téléphone est obligatoire")
                   .addPropertyNode("email")
                   .addConstraintViolation();
            return false;
        }
        
        if (hasEmail && hasPhone) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Veuillez fournir soit un email, soit un numéro de téléphone, mais pas les deux à la fois")
                   .addPropertyNode("email")
                   .addConstraintViolation();
            return false;
        }
        
        return true;
    }
}
