package dic1.projet.trans.backend.services;

import dic1.projet.trans.backend.dtos.*;
import dic1.projet.trans.backend.entities.OtpCode;
import dic1.projet.trans.backend.enums.Role;

import java.util.Optional;
import java.util.stream.Collectors;
import dic1.projet.trans.backend.entities.PasswordResetToken;
import dic1.projet.trans.backend.entities.User;
import dic1.projet.trans.backend.enums.Role;
import dic1.projet.trans.backend.exceptions.ResourceNotFoundException;
import dic1.projet.trans.backend.exceptions.BadRequestException;
import dic1.projet.trans.backend.repositories.OtpCodeRepository;
import dic1.projet.trans.backend.repositories.PasswordResetTokenRepository;
import dic1.projet.trans.backend.repositories.UserRepository;
import dic1.projet.trans.backend.utils.PhoneNumberUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final SmsService smsService;


    @Transactional
    public AuthenticationResponse register(RegisterRequest request) {

        // Vérifier si l'email est fourni AVANT de vérifier s'il existe
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BadRequestException("Cet email est déjà utilisé");
            }
        }

        // Vérifier si le numéro de téléphone est fourni AVANT de vérifier s'il existe
        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new BadRequestException("Ce numéro de téléphone est déjà utilisé");
            }
        }

        // Vérifier qu'au moins un des deux est fourni
        if ((request.getEmail() == null || request.getEmail().isBlank()) &&
                (request.getPhoneNumber() == null || request.getPhoneNumber().isBlank())) {
            throw new BadRequestException("Veuillez fournir un email ou un numéro de téléphone");
        }
        
        // Vérifier si le nom d'utilisateur est déjà utilisé
        if((request.getUsername() != null || !request.getUsername().isBlank())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new BadRequestException("Ce nom d'utilisateur est déjà utilisé");
            }
        }


        // Obtenir les rôles depuis la requête
        List<Role> roles = request.getRoles().stream()
                .map(role -> Role.valueOf(role.name().toUpperCase()))
                .collect(Collectors.toList());

        if (roles.isEmpty()) {
            throw new BadRequestException("Au moins un rôle doit être spécifié");
        }

        // Créer l'utilisateur
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .username(request.getUsername())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(roles)
                .profilePhoto(request.getProfilePicture())
                .inscriptionDate(LocalDateTime.now())
                .enabled(false) // désactivé jusqu'à la vérification OTP
                .accountNonLocked(true)
                .build();

        User savedUser = userRepository.save(user);

        // Générer et envoyer le code OTP
        String otpCode = generateOtpCode();
        if (savedUser.getEmail() != null && !savedUser.getEmail().isBlank()) {
            saveOtpCode(savedUser.getEmail(), null, otpCode);
            emailService.sendOtpEmail(savedUser.getEmail(), otpCode, savedUser.getFirstName());
        } else if (savedUser.getPhoneNumber() != null && !savedUser.getPhoneNumber().isBlank()) {
            saveOtpCode(null, savedUser.getPhoneNumber(), otpCode);
            smsService.sendOtpSms(savedUser.getPhoneNumber(), otpCode);
        }

        // Générer le token
        String jwtToken = jwtService.generateToken(savedUser);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    public AuthenticationResponse login(LoginRequest request) {
        // Vérifier qu'un seul identifiant est fourni
        boolean hasEmail = request.getEmail() != null && !request.getEmail().isBlank();
        boolean hasPhone = request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank();
        
        if (hasEmail && hasPhone) {
            throw new BadRequestException("Veuillez fournir soit un email, soit un numéro de téléphone, mais pas les deux");
        }
        if (!hasEmail && !hasPhone) {
            throw new BadRequestException("Veuillez fournir un email ou un numéro de téléphone");
        }

        try {
            User user;
            String identifier;
            
            if (hasEmail) {
                // Connexion par email
                user = userRepository.findByEmail(request.getEmail())
                        .orElseThrow(() -> new UsernameNotFoundException("Aucun compte trouvé avec cet email"));
                identifier = request.getEmail();
            } else {
                // Connexion par téléphone - on utilise la même normalisation que dans CustomUserDetailsService
                String normalizedPhoneNumber = PhoneNumberUtils.normalizePhoneNumber(request.getPhoneNumber());
                user = userRepository.findByPhoneNumber(normalizedPhoneNumber)
                        .orElseThrow(() -> new UsernameNotFoundException("Aucun compte trouvé avec ce numéro de téléphone"));
                identifier = user.getPhoneNumber(); // On utilise le numéro tel qu'il est stocké en base
            }

            // Vérifier si le compte est vérifié
            if (!user.isEnabled()) {
                throw new BadRequestException("Compte non vérifié. Veuillez vérifier votre email ou votre téléphone.");
            }

            // Authentifier l'utilisateur
            try {
                authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                        identifier,
                        request.getPassword()
                    )
                );
            } catch (BadCredentialsException e) {
                throw new BadCredentialsException("Mot de passe incorrect");
            }


            // Generate JWT token
            String jwtToken = jwtService.generateToken(user);
            
            return AuthenticationResponse.builder()
                    .token(jwtToken)
                    .user(mapToUserDto(user))
                    .build();

        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Identifiants invalides");
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'authentification", e);
        }
    }


    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user;

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("Aucun compte avec cet email"));
        } else if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                    .orElseThrow(() -> new ResourceNotFoundException("Aucun compte avec ce numéro de téléphone"));
        } else {
            throw new BadRequestException("Veuillez fournir un email ou un numéro de téléphone");
        }

        // delete old tokens
        passwordResetTokenRepository.deleteByUserId(user.getIdUser());

        // create new token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .userId(user.getIdUser())
                .expiryDate(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);

        // send email or SMS based on user's preferred contact method
        String firstName = (user.getFirstName() != null && !user.getFirstName().isBlank()) ?
                user.getFirstName() : "User";
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            emailService.sendPasswordResetEmail(user.getEmail(), token, firstName);
        } else if (user.getPhoneNumber() != null && !user.getPhoneNumber().isBlank()) {
            smsService.sendPasswordResetSms(user.getPhoneNumber(), token, firstName);
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Token invalide"));

        if (resetToken.isUsed()) {
            throw new BadRequestException("Ce token a déjà été utilisé");
        }

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Ce token a expiré");
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        emailService.sendPasswordChangedEmail(user.getEmail(), user.getFirstName());
    }


    @Transactional
    public void generateOtp(String email, String phoneNumber) {
        User user;
        String normalizedPhoneNumber = null;

        if (email != null && !email.isBlank()) {
            user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec cet email"));

            otpCodeRepository.deleteByEmail(email);
        } else if (phoneNumber != null && !phoneNumber.isBlank()) {
            // Normaliser le numéro de téléphone
            normalizedPhoneNumber = PhoneNumberUtils.normalizePhoneNumber(phoneNumber);
            
            user = userRepository.findByPhoneNumber(normalizedPhoneNumber)
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec ce numéro"));

            otpCodeRepository.deleteByPhoneNumber(normalizedPhoneNumber);
        } else {
            throw new BadRequestException("Email ou numéro de téléphone requis");
        }

        String otp = generateOtpCode();

        // Sauvegarder le code OTP
        OtpCode otpCode = OtpCode.builder()
                .email(email)
                .phoneNumber(normalizedPhoneNumber)
                .code(otp)
                .expiryDate(LocalDateTime.now().plusMinutes(10))
                .verified(false)
                .attempts(0)
                .build();
        otpCodeRepository.save(otpCode);

        // Envoyer OTP
        if (email != null && !email.isBlank()) {
            emailService.sendOtpEmail(email, otp, user.getFirstName());
        } else {
            smsService.sendOtpSms(phoneNumber, otp);
        }
    }


    @Transactional
    public void verifyOtp(VerifyOtpRequest request) {
        OtpCode otpCode;

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            otpCode = otpCodeRepository.findByEmailAndVerifiedFalse(request.getEmail())
                    .orElseThrow(() -> new BadRequestException("Code OTP invalide ou expiré"));
        } else if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            otpCode = otpCodeRepository.findByPhoneNumberAndVerifiedFalse(request.getPhoneNumber())
                    .orElseThrow(() -> new BadRequestException("Code OTP invalide ou expiré"));
        } else {
            throw new BadRequestException("Email ou numéro de téléphone requis");
        }

        if (otpCode.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Le code OTP a expiré");
        }

        if (otpCode.getAttempts() > 3) {
            throw new BadRequestException("Trop de tentatives. Demandez un nouveau code.");
        }

        if (!otpCode.getCode().equals(request.getCode())) {
            otpCode.setAttempts(otpCode.getAttempts() + 1);
            otpCodeRepository.save(otpCode);
            throw new BadRequestException("Code OTP incorrect");
        }

        otpCode.setVerified(true);
        otpCodeRepository.save(otpCode);

        // enable user
        User user;
        if (otpCode.getEmail() != null) {
            user = userRepository.findByEmail(otpCode.getEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        } else {
            user = userRepository.findByPhoneNumber(otpCode.getPhoneNumber())
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        }
        user.setEnabled(true);
        userRepository.save(user);
    }


    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Ancien mot de passe incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        emailService.sendPasswordChangedEmail(user.getEmail(), user.getFirstName());
    }

    private String generateOtpCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // code à 6 chiffres
        return String.valueOf(code);
    }

    private void saveOtpCode(String email, String phoneNumber, String code) {
        OtpCode otpCode = OtpCode.builder()
                .email(email)
                .phoneNumber(phoneNumber)
                .code(code)
                .expiryDate(LocalDateTime.now().plusMinutes(10))
                .verified(false)
                .attempts(0)
                .build();
        otpCodeRepository.save(otpCode);
    }


    private UserDto mapToUserDto(User user) {
        UserDto dto = new UserDto();
        dto.setIdUser(user.getIdUser());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setProfilePhoto(user.getProfilePhoto());
        // Convertir la liste de rôles en une liste de noms de rôles
        if (user.getRoles() != null) {
            dto.setRoles(user.getRoles().stream()
                    .map(Role::name)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public User updateUser(User user) {
        // Vérifier que l'utilisateur existe
        User existingUser = userRepository.findById(user.getIdUser())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        
        // Mettre à jour uniquement les champs non nuls
        if (user.getFirstName() != null) {
            existingUser.setFirstName(user.getFirstName());
        }
        if (user.getLastName() != null) {
            existingUser.setLastName(user.getLastName());
        }
        if (user.getPhoneNumber() != null) {
            existingUser.setPhoneNumber(user.getPhoneNumber());
        }
        if (user.getUsername() != null) {
            existingUser.setUsername(user.getUsername());
        }
        if (user.getProfilePhoto() != null) {
            existingUser.setProfilePhoto(user.getProfilePhoto());
        }
        
        return userRepository.save(existingUser);
    }

    public User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("User not authenticated");
        }

        String email;

        if (authentication.getPrincipal() instanceof UserDetails) {
            email = ((UserDetails) authentication.getPrincipal()).getUsername();
        } else {
            email = authentication.getName();
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }
    
    public java.util.Optional<User> getUserById(String userId) {
        return userRepository.findById(userId);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByPhoneNumber(String phone) {
        return userRepository.findByPhoneNumber(phone);
    }

}