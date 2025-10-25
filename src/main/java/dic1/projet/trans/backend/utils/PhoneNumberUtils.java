package dic1.projet.trans.backend.utils;

public class PhoneNumberUtils {
    
    public static String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return null;
        }
        
        // Supprimer tous les caractères non numériques
        String digits = phoneNumber.replaceAll("\\D+", "");
        
        // Si le numéro commence par l'indicatif du Sénégal (221), on le retire
        if (digits.startsWith("221")) {
            digits = digits.substring(3);
        }
        
        // S'assurer que le numéro a une longueur valide (9 chiffres pour le Sénégal)
        if (digits.length() != 9) {
            throw new IllegalArgumentException("Le numéro de téléphone doit contenir 9 chiffres (hors indicatif)");
        }
        
        // Formater le numéro : +221 77 123 45 67
        return "+221 " + digits.substring(0, 2) + " " + 
               digits.substring(2, 5) + " " + 
               digits.substring(5, 7) + " " + 
               digits.substring(7);
    }
    
    public static boolean isValidSenegalPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        
        String digits = phoneNumber.replaceAll("\\D+", "");
        
        if (digits.startsWith("221")) {
            digits = digits.substring(3);
        }
        
        return digits.matches("^77\\d{7}$|^76\\d{7}$|^75\\d{7}$|^70\\d{7}$");
    }
}
