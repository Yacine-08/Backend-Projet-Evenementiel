package dic1.projet.trans.backend.services;

import dic1.projet.trans.backend.dto.OrganizerStatsResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class OrganizerStatsService {

    public OrganizerStatsResponse getStatsForOrganizer(Long userId) {
        // TODO: Remplacer ces valeurs par des agrégations réelles via repositories
        int activeEventsCount = 0;
        long totalReservations = 0L;
        BigDecimal totalRevenue = BigDecimal.ZERO;

        // Exemple de calcul du taux de remplissage si données disponibles
        long totalCapacity = 0L; // somme des capacités des événements de l’organisateur
        long totalTicketsSold = 0L; // somme des billets/vérifications confirmées
        double fillRate = (totalCapacity > 0) ? (double) totalTicketsSold / (double) totalCapacity : 0.0;

        return new OrganizerStatsResponse(activeEventsCount, totalReservations, totalRevenue, fillRate);
    }
}