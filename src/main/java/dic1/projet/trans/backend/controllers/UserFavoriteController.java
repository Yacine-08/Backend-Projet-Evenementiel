package dic1.projet.trans.backend.controllers;

import dic1.projet.trans.backend.entities.Event;
import dic1.projet.trans.backend.entities.User;
import dic1.projet.trans.backend.services.AuthenticationService;
import dic1.projet.trans.backend.services.UserFavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class UserFavoriteController {
    private final UserFavoriteService userFavoriteService;
    private final AuthenticationService authenticationService;

    @PostMapping("/me/favorites/{eventId}")
    public ResponseEntity<Void> addFavorite(
            @PathVariable String eventId,
            Authentication authentication) {
        User currentUser = authenticationService.getCurrentUser(authentication);
        userFavoriteService.addFavorite(currentUser.getIdUser(), eventId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/me/favorites/{eventId}")
    public ResponseEntity<Void> removeFavorite(
            @PathVariable String eventId,
            Authentication authentication) {
        User currentUser = authenticationService.getCurrentUser(authentication);
        userFavoriteService.removeFavorite(currentUser.getIdUser(), eventId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/favorites")
    public ResponseEntity<List<Event>> getFavorites(Authentication authentication) {
        User currentUser = authenticationService.getCurrentUser(authentication);
        return ResponseEntity.ok(userFavoriteService.getUserFavorites(currentUser.getIdUser()));
    }

    @GetMapping("/me/favorites/{eventId}")
    public ResponseEntity<Boolean> isEventInFavorite(
            @PathVariable String eventId,
            Authentication authentication) {
        User currentUser = authenticationService.getCurrentUser(authentication);
        return ResponseEntity.ok(
                userFavoriteService.isEventInFavorites(currentUser.getIdUser(), eventId)
        );
    }
}