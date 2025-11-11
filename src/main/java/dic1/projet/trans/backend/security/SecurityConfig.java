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
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // OPTIONS requests (preflight CORS)
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
                        .requestMatchers("/api/auth/**").permitAll()

                        // Public event endpoints (GET only)
                        .requestMatchers(HttpMethod.GET, 
                            "/api/events",
                            "/api/events/search",
                            "/api/events/events",
                            "/api/events/{eventId}",
                            "/api/events/{eventId}/tickets"
                        ).permitAll()

                        // Protected event endpoints - require authentication
                        .requestMatchers("/api/events/my-events").authenticated()
                        .requestMatchers("/api/events/create").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/events/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/events/**").authenticated()
                        .requestMatchers("/api/events/*/bookings").authenticated()
                        .requestMatchers("/api/events/*/booking-count").authenticated()
                        .requestMatchers("/api/events/*/revenue").authenticated()
                        .requestMatchers("/api/events/*/potential-revenue").authenticated()

                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
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
            System.out.println("=== UserDetailsService: Loading user: " + username);

            // First try to find by username, email, or phone number
            Optional<User> userOptional = userRepository.findByUsernameOrEmailOrPhoneNumber(username, username, username);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                System.out.println("=== UserDetailsService: User found: " + user.getUsername() + ", Roles: " + user.getRoles());
                
                // Return our custom User object that implements UserDetails
                return user;
            }

            // If not found, try more specific lookups
            if (username.contains("@")) {
                userOptional = userRepository.findByEmail(username);
            } else if (username.matches(".*\\d.*")) {
                // Try different phone number formats
                String[] possibleFormats = {
                        username, // Original format
                        username.replaceAll("\\s+", ""), // Without spaces
                        username.replaceAll("\\D+", ""), // Numbers only
                        username.replaceAll("^\\+221", "").replaceAll("\\s+", ""), // Without +221 and spaces
                        "221" + username.replaceAll("\\D+", ""), // With 221 prefix
                        "+" + username.replaceAll("\\D+", ""), // With + prefix
                        "221" + username // Simple format with 221
                };

                for (String phoneNumber : possibleFormats) {
                    if (phoneNumber == null || phoneNumber.trim().isEmpty()) continue;
                    userOptional = userRepository.findByPhoneNumber(phoneNumber);
                    if (userOptional.isPresent()) break;
                }
            }

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                System.out.println("=== UserDetailsService: User found (alternative lookup): " + user.getUsername() + ", Roles: " + user.getRoles());
                
                // Return our custom User object that implements UserDetails
                return user;
            }

            System.out.println("=== UserDetailsService: User NOT found: " + username);
            throw new UsernameNotFoundException("Aucun compte trouvé avec cet identifiant: " + username);
        };
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtService, userDetailsService());
    }
}