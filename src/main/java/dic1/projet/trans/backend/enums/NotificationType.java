package dic1.projet.trans.backend.enums;


public enum NotificationType {
    BOOKING_CONFIRMATION("Confirmation de réservation"),
    BOOKING_CANCELLED("Annulation de réservation"),
    BOOKING_REMINDER("Rappel de réservation"),
    EVENT_REMINDER("Rappel d'événement"),
    EVENT_UPDATED("Événement modifié"),
    EVENT_CANCELLED("Événement annulé"),
    PAYMENT_CONFIRMATION("Confirmation de paiement"),
    PAYMENT_REFUND("Remboursement"),
    ACCOUNT_VERIFIED("Compte vérifié"),
    SYSTEM_ALERT("Alerte système"),
    CUSTOM("Personnalisé");

    private final String displayName;

    NotificationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}