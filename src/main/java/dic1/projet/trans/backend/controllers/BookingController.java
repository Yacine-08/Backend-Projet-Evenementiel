package dic1.projet.trans.backend.controllers;

import dic1.projet.trans.backend.dtos.CreateBookingRequest;
import dic1.projet.trans.backend.entities.Booking;
import dic1.projet.trans.backend.entities.User;
import dic1.projet.trans.backend.services.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Réservations", description = "Endpoints pour gérer les réservations de tickets")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/create")
    @Operation(summary = "Créer une réservation", description = "Crée une ou plusieurs réservations pour des tickets. Authentification requise.")
    public ResponseEntity<List<Booking>> createBooking(@AuthenticationPrincipal UserDetails userDetails,
                                                     @Valid @RequestBody CreateBookingRequest request) {
        User user = (User) userDetails;
        List<Booking> bookings = bookingService.createBooking(user.getIdUser(), request);
        return ResponseEntity.ok(bookings);
    }



    @PutMapping("/confirm/{id}")
    @Operation(summary = "Confirmer une réservation", description = "Confirme une réservation (organisateur requis). Si group=true, confirme toutes les réservations du même groupe.")
    public ResponseEntity<List<Booking>> confirmBooking(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "false") boolean group) {
        
        if (group) {
            return ResponseEntity.ok(bookingService.confirmBookingGroup(id));
        } else {
            return ResponseEntity.ok(Collections.singletonList(bookingService.confirmSingleBooking(id)));
        }
    }

    @GetMapping("/me")
    @Operation(summary = "Lister mes réservations", description = "Retourne les réservations de l'utilisateur connecté.")
    public ResponseEntity<List<Booking>> myBookings(@AuthenticationPrincipal UserDetails userDetails) {
        User user = (User) userDetails;
        List<Booking> bookings = bookingService.getBookingsForUser(user.getIdUser());
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détails d'une réservation", description = "Récupère une réservation si elle appartient à l'utilisateur connecté.")
    public ResponseEntity<Booking> getBooking(@AuthenticationPrincipal UserDetails userDetails,
                                              @PathVariable String id) {
        User user = (User) userDetails;
        Booking booking = bookingService.getBookingByIdOwned(id, user.getIdUser());
        return ResponseEntity.ok(booking);
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Annuler une réservation", description = "Annule une réservation et réajuste les quantités des tickets.")
    public ResponseEntity<Booking> cancelBooking(@AuthenticationPrincipal UserDetails userDetails,
                                                 @PathVariable String id) {
        User user = (User) userDetails;
        Booking booking = bookingService.cancelBooking(id, user.getIdUser());
        return ResponseEntity.ok(booking);
    }
}