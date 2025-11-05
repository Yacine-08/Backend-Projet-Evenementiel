package dic1.projet.trans.backend.validation;

import dic1.projet.trans.backend.dtos.EventCreateDTO;
import dic1.projet.trans.backend.enums.EventType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RefundPolicyValidator implements ConstraintValidator<ValidRefundPolicy, EventCreateDTO> {

    @Override
    public void initialize(ValidRefundPolicy constraintAnnotation) {
    }

    @Override
    public boolean isValid(EventCreateDTO eventDTO, ConstraintValidatorContext context) {
        // Si l'événement est gratuit, pas besoin de politique de remboursement
        if (eventDTO.getTypeEvent() == EventType.FREE) {
            return true;
        }
        
        // Pour les événements payants, la politique de remboursement est obligatoire
        // uniquement si l'événement propose un remboursement
        if (eventDTO.getTypeEvent() == EventType.PAID && eventDTO.isRefundable()) {
            return eventDTO.getRefundPolicy() != null && !eventDTO.getRefundPolicy().trim().isEmpty();
        }
        
        return true;
    }
}
