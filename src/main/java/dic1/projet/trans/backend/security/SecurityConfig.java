package dic1.projet.trans.backend.security;

import dic1.projet.trans.backend.entities.User;
import dic1.projet.trans.backend.repositories.UserRepository;
import dic1.projet.trans.backend.services.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Optional;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Swagger UI & OpenAPI docs should be accessible
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/api/auth/**"
                        ).permitAll()
                        .requestMatchers("/current-user").authenticated()
                        .requestMatchers("/api/admin/**").hasRole("ADMINISTRATOR")
                        .requestMatchers("/api/organizer/**").hasAnyRole("ORGANIZER", "ADMINISTRATOR")
                        .requestMatchers("/api/favorites/**").authenticated()
                        .requestMatchers("/api/events/**").hasAnyRole("CLIENT", "ORGANIZER", "ADMINISTRATOR")
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService());
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            Optional<User> userByUsername = userRepository.findByUsername(username);
            if (userByUsername.isPresent()) {
                return userByUsername.get();
            }

            if (username.contains("@")) {
                return userRepository.findByEmail(username)
                        .orElseThrow(() -> new UsernameNotFoundException("Aucun compte trouvé avec cet email: " + username));
            }

            if (username.matches(".*\\d.*")) { // Si la chaîne contient des chiffres
                // Essayer différents formats de numéro de téléphone
                String[] possibleFormats = {
                    username, // Format original
                    username.replaceAll("\\s+", ""), // Sans espaces
                    username.replaceAll("\\D+", ""), // Uniquement les chiffres
                    username.replaceAll("^\\+221", "").replaceAll("\\s+", ""), // Sans +221 et sans espaces
                    "221" + username.replaceAll("\\D+", ""), // Avec préfixe 221
                    "+" + username.replaceAll("\\D+", ""), // Avec préfixe +
                    "221" + username // Format simple avec 221
                };

                for (String phoneNumber : possibleFormats) {
                    if (phoneNumber == null || phoneNumber.trim().isEmpty()) continue;
                    
                    Optional<User> userOpt = userRepository.findByPhoneNumber(phoneNumber);
                    if (userOpt.isPresent()) {
                        return userOpt.get();
                    }
                }
            }

            throw new UsernameNotFoundException("Aucun compte trouvé avec cet identifiant: " + username);
        };
    }
    
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtService);
    }
    
}
