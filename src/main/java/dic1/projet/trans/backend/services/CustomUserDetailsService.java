// UserDetailsService Implementation
package dic1.projet.trans.backend.services;

import dic1.projet.trans.backend.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import dic1.projet.trans.backend.utils.PhoneNumberUtils;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService {

    private final UserRepository userRepository;

    public UserDetails loadUserByMailOrPhoneNumber(String email,String phoneNumber) throws UsernameNotFoundException {

        if(email != null) {
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé avec le mail: " + email));
        }
        else if(phoneNumber != null) {
            // Normaliser le numéro de téléphone avant la recherche
            String normalizedPhoneNumber = PhoneNumberUtils.normalizePhoneNumber(phoneNumber);
            return userRepository.findByPhoneNumber(normalizedPhoneNumber)
                    .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé avec le numéro de téléphone: " + phoneNumber));
        }
        else {
            throw new UsernameNotFoundException("Email ou numéro de téléphone requis");
        }
    }
}