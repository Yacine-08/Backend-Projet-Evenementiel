package dic1.projet.trans.backend.dtos;

import dic1.projet.trans.backend.validators.EmailOrPhoneRequired;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EmailOrPhoneRequiredValidator implements ConstraintValidator<EmailOrPhoneRequired, RegisterRequest> {

    @Override
    public boolean isValid(RegisterRequest request, ConstraintValidatorContext context) {
        if ((request.getEmail() == null || request.getEmail().isBlank()) &&
            (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("L'email ou le numéro de téléphone est obligatoire")
                   .addConstraintViolation();
            return false;
        }
        return true;
    }
}
