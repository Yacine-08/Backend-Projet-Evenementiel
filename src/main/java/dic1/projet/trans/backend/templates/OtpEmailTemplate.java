package dic1.projet.trans.backend.templates;


public class OtpEmailTemplate {

    public static String buildOtpEmailTemplate(String firstName, String otpCode) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                        .content { background-color: #f9f9f9; padding: 30px; border-radius: 8px; }
                        .otp-code { font-size: 32px; font-weight: bold; color: #4CAF50; text-align: center; letter-spacing: 5px; padding: 20px; background: white; border-radius: 8px; margin: 20px 0; }
                        .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #666; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Vérification de votre compte</h1>
                        </div>
                        <div class="content">
                            <p>Bonjour <strong>%s</strong>,</p>
                            <p>Merci de vous être inscrit sur EventLoop. Pour activer votre compte, veuillez utiliser le code de vérification ci-dessous :</p>
                            <div class="otp-code">%s</div>
                            <p>Ce code expirera dans <strong>10 minutes</strong>.</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2025 EventLoop. Tous droits réservés.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(firstName, otpCode);
    }
}
