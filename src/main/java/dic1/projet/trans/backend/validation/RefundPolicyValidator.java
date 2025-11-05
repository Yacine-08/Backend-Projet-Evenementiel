package dic1.projet.trans.backend.validation;

import dic1.projet.trans.backend.dtos.EventCreateDTO;
import dic1.projet.trans.backend.enums.EventType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RefundPolicyValidator implements ConstraintValidator<ValidRefundPolicy, EventCreateDTO> {

    @Override
    public boolean isValid(EventCreateDTO eventDTO, ConstraintValidatorContext context) {
        // Si l'objet est nul, on considère qu'il sera vérifié ailleurs (ex. @NotNull sur le contrôleur)
        if (eventDTO == null) {
            return true;
        }

        EventType type = eventDTO.getTypeEvent();

        // Si le type d'événement n'est pas encore défini, on ne valide pas cette contrainte ici
        if (type == null) {
            return true;
        }

        // Si l'événement est gratuit → pas besoin de politique de remboursement
        if (type == EventType.FREE) {
            return true;
        }

        // Si l'événement est payant et remboursable → la politique de remboursement est obligatoire
        if (type == EventType.PAID && eventDTO.isRefundable()) {
            boolean validPolicy = eventDTO.getRefundPolicy() != null && !eventDTO.getRefundPolicy().trim().isEmpty();

            if (!validPolicy) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        "La politique de remboursement est obligatoire pour un événement payant remboursable."
                ).addPropertyNode("refundPolicy").addConstraintViolation();
            }

            return validPolicy;
        }

        // Cas par défaut : valide
        return true;
    }
}
