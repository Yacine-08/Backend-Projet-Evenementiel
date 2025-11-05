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
    CUSTOM("Personnalisé"),
    EVENT_CREATED_DRAFT("Événement créé (brouillon)"),
    EVENT_PUBLISHED("Événement publié"),
    NEW_BOOKING("Nouvelle réservation"),
    TICKETS_LOW_STOCK("Stock de billets faible (10 restants)"),
    TICKETS_SOLD_OUT("Événement complet - Plus de billets disponibles");

    private final String displayName;

    NotificationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}