package dic1.projet.trans.backend.services;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import static dic1.projet.trans.backend.templates.OtpEmailTemplate.buildOtpEmailTemplate;
import static dic1.projet.trans.backend.templates.PasswordResetTemplate.buildPasswordResetTemplate;
import static dic1.projet.trans.backend.templates.PasswordchangedTemplate.buildPasswordChangedTemplate;
import static dic1.projet.trans.backend.templates.WelcomeEmailTemplate.buildWelcomeEmailTemplate;



@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    // sender email (noreply@gmail.com)
    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    // send otp email
    public void sendOtpEmail(String to, String otpCode, String firstName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Code de vérification - EventLoop");

            String htmlContent = buildOtpEmailTemplate(firstName, otpCode);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Erreur lors de l'envoi de l'email", e);
        }
    }

    // send password reset email
    public void sendPasswordResetEmail(String to, String token, String firstName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Réinitialisation de mot de passe - EventLoop");

            String resetUrl = frontendUrl + "/reset-password?token=" + token;
            String htmlContent = buildPasswordResetTemplate(firstName, resetUrl);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Erreur lors de l'envoi de l'email", e);
        }
    }

    // confirm password changed
    public void sendPasswordChangedEmail(String to, String firstName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Mot de passe modifié - EventLoop");

            String htmlContent = buildPasswordChangedTemplate(firstName);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Erreur lors de l'envoi de l'email", e);
        }
    }

    // welcome email
    public void sendWelcomeEmail(String to, String firstName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Bienvenue sur EventLoop");
            String htmlContent = buildWelcomeEmailTemplate(firstName);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Erreur lors de l'envoi de l'email de bienvenue", e);
        }
    }

    // booking confirmation email
    public void sendBookingConfirmationEmail(String to, String firstName, String eventName, String bookingId, boolean isGroup, int bookingCount) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Confirmation de votre réservation - EventLoop");

            String bookingDetails;
            if (isGroup) {
                bookingDetails = String.format("Vous avez effectué %d réservations pour l'événement : %s", bookingCount, eventName);
            } else {
                bookingDetails = String.format("Vous avez effectué une réservation pour l'événement : %s", eventName);
            }

            String htmlContent = "<html><body>" +
                    "<h2>Confirmation de réservation</h2>" +
                    "<p>Bonjour " + firstName + ",</p>" +
                    "<p>" + bookingDetails + "</p>" +
                    "<p>Numéro de réservation : " + bookingId + "</p>" +
                    "<p>Vous pouvez consulter les détails de votre réservation dans votre espace personnel.</p>" +
                    "<p>Cordialement,<br>L'équipe EventLoop</p>" +
                    "</body></html>";

            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Erreur lors de l'envoi de l'email de confirmation de réservation", e);
        }
    }
}