package dic1.projet.trans.backend.services;

import dic1.projet.trans.backend.dtos.*;
import dic1.projet.trans.backend.entities.OtpCode;
import dic1.projet.trans.backend.entities.PasswordResetToken;
import dic1.projet.trans.backend.entities.User;
import dic1.projet.trans.backend.enums.Role;
import dic1.projet.trans.backend.exceptions.ResourceNotFoundException;
import dic1.projet.trans.backend.exceptions.BadRequestException;
import dic1.projet.trans.backend.repositories.OtpCodeRepository;
import dic1.projet.trans.backend.repositories.PasswordResetTokenRepository;
import dic1.projet.trans.backend.repositories.UserRepository;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

        // verify if the email is already used
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Cet email est déjà utilisé");
        }

        // verify if the username is already used
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Ce nom d'utilisateur est déjà utilisé");
        }

        // create user
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .username(request.getUsername())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.valueOf(request.getRole()))
                .inscriptionDate(LocalDateTime.now())
                .enabled(false) // disabled until otp verification
                .accountNonLocked(true)
                .build();

        User savedUser = userRepository.save(user);

        // generate otp code and send it to the user
        String otpCode = generateOtpCode();
        if (savedUser.getEmail() != null && !savedUser.getEmail().isBlank()) {
            saveOtpCode(savedUser.getEmail(), null, otpCode);
            emailService.sendOtpEmail(savedUser.getEmail(), otpCode, savedUser.getFirstName());
        } else if (savedUser.getPhoneNumber() != null && !savedUser.getPhoneNumber().isBlank()) {
            saveOtpCode(null, savedUser.getPhoneNumber(), otpCode);
            smsService.sendOtpSms(savedUser.getPhoneNumber(), otpCode);
        }

        // generate token
        String jwtToken = jwtService.generateToken(savedUser);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .user(mapToUserDto(savedUser))
                .build();
    }

    public AuthenticationResponse login(LoginRequest request) {
        User user;

        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé avec cet email"));
        } else if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                    .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé avec ce numéro"));
        } else {
            throw new BadRequestException("Veuillez fournir un email ou un numéro de téléphone");
        }

        // check if the account is verified
        if (!user.isEnabled()) {
            throw new BadRequestException("Compte non vérifié. Veuillez vérifier votre email ou votre téléphone.");
        }

        // authenticate user
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        user.getUsername(), // toujours username pour Spring Security
                        request.getPassword()
                )
        );

        // generate token
        String jwtToken = jwtService.generateToken(user);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .user(mapToUserDto(user))
                .build();
    }


    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Aucun compte avec cet email"));

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

        // send email
        emailService.sendPasswordResetEmail(user.getEmail(), token, user.getFirstName());
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

        if (email != null && !email.isBlank()) {
            user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec cet email"));
            // Supprimer anciens codes OTP pour cet email
            otpCodeRepository.deleteByEmail(email);
        } else if (phoneNumber != null && !phoneNumber.isBlank()) {
            user = userRepository.findByPhoneNumber(phoneNumber)
                    .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec ce numéro"));
            // Supprimer anciens codes OTP pour ce numéro
            otpCodeRepository.deleteByPhoneNumber(phoneNumber);
        } else {
            throw new BadRequestException("Email ou numéro de téléphone requis");
        }

        String otp = generateOtpCode();

        // Sauvegarder le code OTP
        OtpCode otpCode = OtpCode.builder()
                .email(email)
                .phoneNumber(phoneNumber)
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
        dto.setRole(user.getRole().name());
        return dto;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void updateUser(@Valid UserDto userDto) {
        User user = userRepository.findById(userDto.getIdUser())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        user.setFirstName(userDto.getFirstName());
        user.setLastName(userDto.getLastName());
        user.setUsername(userDto.getUsername());
        user.setEmail(userDto.getEmail());
        user.setPhoneNumber(userDto.getPhoneNumber());
        user.setProfilePhoto(userDto.getProfilePhoto());
        user.setRole(Role.valueOf(userDto.getRole()));
        userRepository.save(user);
    }
}