package dic1.projet.trans.backend.controllers;

import dic1.projet.trans.backend.dtos.*;
import dic1.projet.trans.backend.entities.User;
import dic1.projet.trans.backend.services.AuthenticationService;
import dic1.projet.trans.backend.enums.Role;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Authentification", description = "Endpoints pour gérer l'inscription, la connexion et le profil utilisateur")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    @Operation(summary = "Inscription d'un utilisateur", description = "Crée un compte utilisateur et envoie un code OTP pour activer le compte.")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthenticationResponse response = authenticationService.register(request);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Inscription réussie. Veuillez vérifier votre email pour activer votre compte.",
                    "data", response
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Connexion", description = "Authentifie l'utilisateur et retourne un jeton JWT.")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthenticationResponse response = authenticationService.login(request);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Connexion réussie",
                    "data", response
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Vérifier le code OTP", description = "Valide le code OTP pour activer le compte utilisateur.")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        try {
            authenticationService.verifyOtp(request);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Code OTP vérifié avec succès. Votre compte est maintenant actif."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Renvoyer le code OTP", description = "Génère et renvoie un nouveau code OTP par email ou SMS.")
    public ResponseEntity<?> resendOtp(@RequestBody ResendOtpRequest request) {
        try {
            if ((request.getEmail() == null || request.getEmail().isBlank()) &&
                    (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Veuillez fournir un email ou un numéro de téléphone"
                ));
            }

            authenticationService.generateOtp(request.getEmail(), request.getPhoneNumber());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Un nouveau code OTP a été envoyé"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }


    @PostMapping("/forgot-password")
    @Operation(summary = "Mot de passe oublié", description = "Commence la procédure de réinitialisation du mot de passe.")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            authenticationService.forgotPassword(request);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Un email de réinitialisation a été envoyé à votre adresse"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Réinitialiser le mot de passe", description = "Réinitialise le mot de passe à l'aide du token de réinitialisation.")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            authenticationService.resetPassword(request);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Mot de passe réinitialisé avec succès"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/change-password")
    @Operation(summary = "Changer le mot de passe", description = "Modifie le mot de passe de l'utilisateur connecté.")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        try {
            // Extraire l'ID utilisateur depuis UserDetails
            String userId = ((User) userDetails).getIdUser();
            authenticationService.changePassword(userId, request);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Mot de passe modifié avec succès"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/current-user")
    @Operation(summary = "Informations de l'utilisateur connecté", description = "Retourne les informations du profil de l'utilisateur actuellement connecté.")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            User user = (User) userDetails;
            
            UserDto userDto = new UserDto();
            userDto.setIdUser(user.getIdUser());
            userDto.setFirstName(user.getFirstName());
            userDto.setLastName(user.getLastName());
            userDto.setUsername(user.getUsername());
            userDto.setEmail(user.getEmail());
            userDto.setPhoneNumber(user.getPhoneNumber());
            userDto.setProfilePhoto(user.getProfilePhoto());
            userDto.setRoles(user.getRoles().stream()
                    .map(Role::name)
                    .collect(Collectors.toList()));

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", userDto
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/users")
    @Operation(summary = "Lister les utilisateurs", description = "Retourne la liste de tous les utilisateurs.")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = authenticationService.getAllUsers();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", users
            ));
        }catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping("/update-user")
    @Operation(summary = "Mettre à jour l'utilisateur", description = "Met à jour les informations du profil utilisateur.")
    public ResponseEntity<?> updateUser(@AuthenticationPrincipal UserDetails userDetails, @Valid @RequestBody UserDto userDto) {
        try {
            authenticationService.updateUser(userDto);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Utilisateur mis à jour avec succès"
            ));
        }catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }



    @PostMapping("/logout")
    @Operation(summary = "Déconnexion", description = "Procède à la déconnexion côté client en supprimant le JWT.")
    public ResponseEntity<?> logout() {
        // Avec JWT, le logout se fait côté client en supprimant le token
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Déconnexion réussie"
        ));
    }

    @GetMapping("/validate-token")
    @Operation(summary = "Valider le token", description = "Vérifie si le token JWT actuel est valide.")
    public ResponseEntity<?> validateToken(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "valid", true
            ));
        }
        return ResponseEntity.ok(Map.of(
                "success", true,
                "valid", false
        ));
    }
}