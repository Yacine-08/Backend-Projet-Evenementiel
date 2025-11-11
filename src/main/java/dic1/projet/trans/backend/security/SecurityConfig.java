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
import org.springframework.http.HttpMethod;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Optional;
import java.util.Arrays;
import java.util.List;

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
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                    // Swagger UI & OpenAPI docs
                    .requestMatchers(
                            "/v3/api-docs/**",
                            "/swagger-ui.html",
                            "/swagger-ui/**",
                            "/swagger-resources/**",
                            "/webjars/**"
                    ).permitAll()
                    
                    // Authentication endpoints
                    .requestMatchers(
                            "/api/auth/**"
                    ).permitAll()

                    // Public event endpoints
                    .requestMatchers(
                            "/api/events/events",
                            "/api/events/search/**",
                            "/api/events/{eventId}",
                            "/api/events/{eventId}/details"
                    ).permitAll()

                    // Organizer endpoints
                    .requestMatchers(
                        "/api/events/my-events",
                        "/api/organizer/**"
                    ).hasAnyRole("ORGANIZER", "ADMINISTRATOR")
                    
                    // Event statistics endpoints - requiring authentication
                    .requestMatchers(
                        "/api/events/*/revenue",
                        "/api/events/*/potential-revenue",
                        "/api/events/*/booking-count"
                    ).authenticated()
                    
                    // Admin endpoints
                    .requestMatchers("/api/admin/**").hasRole("ADMINISTRATOR")
                    
                    // Authenticated user endpoints
                    .requestMatchers("/current-user").authenticated()
                    
                    // All other event endpoints require authentication
                    .requestMatchers("/api/events/**").authenticated()
                    
                    // Points d'entrée authentifiés
                    .requestMatchers(
                            "/api/favorites/**",
                            "/api/notifications/**"
                    ).authenticated()

                    // Toutes les autres requêtes nécessitent une authentification
                    .anyRequest().authenticated()
            )
            // Configuration de la gestion de session
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // Ajout du filtre JWT avant le filtre d'authentification par nom d'utilisateur/mot de passe
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Autoriser l'envoi des cookies/credentials
        config.setAllowCredentials(true);
        // Autoriser toutes les origines (à restreindre en production)
        config.setAllowedOriginPatterns(List.of("*"));
        // Autoriser les méthodes HTTP nécessaires
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // Autoriser les en-têtes nécessaires
        config.setAllowedHeaders(List.of(
            "Authorization", 
            "Content-Type", 
            "X-Requested-With", 
            "Accept", 
            "Origin",
            "Access-Control-Allow-Headers",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));
        // Exposer les en-têtes personnalisés
        config.setExposedHeaders(List.of(
            "Authorization", 
            "Content-Disposition",
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials"
        ));
        // Durée de vie de la configuration CORS en secondes (1 heure)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
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
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);
        filter.setUserDetailsService(userDetailsService());
        return filter;
    }
    
}
