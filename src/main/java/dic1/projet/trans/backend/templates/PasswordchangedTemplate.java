package dic1.projet.trans.backend.templates;

public class PasswordchangedTemplate {

    public static String buildPasswordChangedTemplate(String firstName) {
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
                        .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #666; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>Mot de passe modifié avec succès</h1>
                        </div>
                        <div class="content">
                            <p>Bonjour <strong>%s</strong>,</p>
                            <p>Votre mot de passe a été modifié avec succès.</p>
                            <p>Si vous n'êtes pas à l'origine de cette modification, veuillez contacter immédiatement notre support.</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2025 EventLoop. Tous droits réservés.</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(firstName);
    }
}
