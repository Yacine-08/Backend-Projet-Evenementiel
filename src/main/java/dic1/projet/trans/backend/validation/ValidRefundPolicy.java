package dic1.projet.trans.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = RefundPolicyValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRefundPolicy {
    String message() default "Une politique de remboursement est requise pour les événements payants";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
