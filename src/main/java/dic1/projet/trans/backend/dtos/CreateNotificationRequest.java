package dic1.projet.trans.backend.dtos;

import dic1.projet.trans.backend.enums.NotificationType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.lang.annotation.*;
import java.util.List;
import java.util.Map;

@Data
@CreateNotificationRequest.ValidateNotificationContent
public class CreateNotificationRequest {
    
    private String title;
    
    private String content;
    
    @NotEmpty(message = "La liste des destinataires ne peut pas être vide")
    private List<String> recipientIds;
    
    private NotificationType type = NotificationType.CUSTOM;
    
    private Map<String, String> metadata;
    
    private String actionUrl;
    
    private String reason;
    
    // Annotation pour la validation conditionnelle
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = NotificationContentValidator.class)
    public @interface ValidateNotificationContent {
        String message() default "Les champs titre et contenu sont obligatoires pour les notifications de type CUSTOM";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
    }
    
    // Validateur personnalisé
    public static class NotificationContentValidator 
            implements ConstraintValidator<ValidateNotificationContent, CreateNotificationRequest> {
            
        @Override
        public boolean isValid(CreateNotificationRequest request, ConstraintValidatorContext context) {
            if (request.getType() == NotificationType.CUSTOM) {
                if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate("Le titre est obligatoire pour les notifications de type CUSTOM")
                           .addPropertyNode("title")
                           .addConstraintViolation();
                    return false;
                }
                if (request.getContent() == null || request.getContent().trim().isEmpty()) {
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate("Le contenu est obligatoire pour les notifications de type CUSTOM")
                           .addPropertyNode("content")
                           .addConstraintViolation();
                    return false;
                }
            }
            return true;
        }
    }
}