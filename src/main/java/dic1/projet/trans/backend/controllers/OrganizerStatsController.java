package dic1.projet.trans.backend.controllers;

import dic1.projet.trans.backend.dtos.OrganizerStatsResponse;
import dic1.projet.trans.backend.dtos.BookingDetailsDTO;
import dic1.projet.trans.backend.entities.Event;
import dic1.projet.trans.backend.services.EventService;
import dic1.projet.trans.backend.services.OrganizerStatsService;
import dic1.projet.trans.backend.services.BookingService;
import dic1.projet.trans.backend.enums.BookingStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizer")
public class OrganizerStatsController {

    private final OrganizerStatsService statsService;
    private final EventService eventService;
    private final BookingService bookingService;

    public OrganizerStatsController(OrganizerStatsService statsService, EventService eventService, BookingService bookingService) {
        this.statsService = statsService;
        this.eventService = eventService;
        this.bookingService = bookingService;
    }

    @GetMapping("/{userId}/stats")
    public ResponseEntity<OrganizerStatsResponse> getStats(@PathVariable("userId") String userId) {
        OrganizerStatsResponse stats = statsService.getStatsForOrganizer(userId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/{userId}/events/recent")
    public ResponseEntity<?> getRecentEvents(@PathVariable("userId") String userId,
                                             @RequestParam(value = "limit", required = false, defaultValue = "5") int limit) {
        return ResponseEntity.ok(eventService.getRecentEventsByOrganizer(userId, limit));
    }

    @GetMapping("/{userId}/bookings/recent")
    public ResponseEntity<?> getRecentBookings(@PathVariable("userId") String userId,
                                               @RequestParam(value = "limit", required = false, defaultValue = "5") int limit,
                                               @RequestParam(value = "status", required = false) String status) {
        BookingStatus st = null;
        if (status != null) {
            try {
                st = BookingStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        return ResponseEntity.ok(bookingService.getRecentBookingsForOrganizer(userId, limit, st));
    }

    @GetMapping("/{userId}/events/revenues")
    public ResponseEntity<?> getEventRevenues(@PathVariable("userId") String userId) {
        return ResponseEntity.ok(eventService.getEventRevenuesByOrganizer(userId));
    }

    @GetMapping("/{userId}/sales/monthly")
    public ResponseEntity<?> getMonthlySales(@PathVariable("userId") String userId,
                                             @RequestParam(value = "months", required = false, defaultValue = "6") int months) {
        return ResponseEntity.ok(bookingService.getMonthlySalesForOrganizer(userId, months));
    }
}