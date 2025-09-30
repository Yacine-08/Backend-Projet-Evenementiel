package dic1.projet.trans.backend.templates;

public class PasswordResetTemplate {

    public static String buildPasswordResetTemplate(String firstName, String resetUrl) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #2196F3; color: white; padding: 20px; text-align: center; }
                        .content { background-color: #f9f9f9; padding: 30px; border-radius: 8px; }
                        .button { display: inline-block; padding: 12px 30px; background-color: #2196F3; color: white; text-decoration: none; border-radius: 5px; margin: 20px 0; }
                        .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #666; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Réinitialisation de mot de passe</h1>
                        </div>
                        <div class="content">
                            <p>Bonjour <strong>%s</strong>,</p>
                            <p>Vous avez demandé à réinitialiser votre mot de passe. Cliquez sur le bouton ci-dessous pour créer un nouveau mot de passe :</p>
                            <div style="text-align: center;">
                                <a href="%s" class="button">Réinitialiser mon mot de passe</a>
                            </div>
                            <p>Ce lien expirera dans <strong>24 heures</strong>.</p>
                            <p>Si vous n'avez pas demandé cette réinitialisation, veuillez ignorer cet email ou contacter notre support.</p>
                            <p style="font-size: 12px; color: #666; margin-top: 20px;">Si le bouton ne fonctionne pas, copiez ce lien : %s</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2025 EventLoop. Tous droits réservés.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(firstName, resetUrl, resetUrl);
    }
}
