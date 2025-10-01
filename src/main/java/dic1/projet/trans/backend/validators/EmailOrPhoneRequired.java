package dic1.projet.trans.backend.validators;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EmailOrPhoneRequiredValidator.class)
@Documented
public @interface EmailOrPhoneRequired {
    String message() default "L'email ou le numéro de téléphone est obligatoire";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
