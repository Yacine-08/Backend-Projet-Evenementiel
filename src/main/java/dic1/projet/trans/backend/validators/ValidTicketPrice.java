package dic1.projet.trans.backend.validators;

import dic1.projet.trans.backend.entities.Event;
import dic1.projet.trans.backend.entities.Ticket;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidTicketPrice.Validator.class)
@Documented
public @interface ValidTicketPrice {
    String message() default "Le prix du billet n'est pas valide pour ce type d'événement";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidTicketPrice, Double> {
        
        @Autowired
        private MongoTemplate mongoTemplate;
        
        @Override
        public boolean isValid(Double price, jakarta.validation.ConstraintValidatorContext context) {

            return true;
        }
    }
}
