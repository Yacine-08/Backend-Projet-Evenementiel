package dic1.projet.trans.backend.controllers;

import dic1.projet.trans.backend.dto.OrganizerStatsResponse;
import dic1.projet.trans.backend.services.OrganizerStatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizer")
public class OrganizerStatsController {

    private final OrganizerStatsService statsService;

    public OrganizerStatsController(OrganizerStatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/{userId}/stats")
    public ResponseEntity<OrganizerStatsResponse> getStats(@PathVariable("userId") Long userId) {
        OrganizerStatsResponse stats = statsService.getStatsForOrganizer(userId);
        return ResponseEntity.ok(stats);
    }
}