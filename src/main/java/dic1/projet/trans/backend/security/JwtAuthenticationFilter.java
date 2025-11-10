package dic1.projet.trans.backend.security;

import dic1.projet.trans.backend.services.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private UserDetailsService userDetailsService;
    
    public void setUserDetailsService(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/") ||
                path.startsWith("/api/events/events") ||
                path.startsWith("/swagger-ui/") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/api/events/search/") ||
                path.startsWith("/api/events/{eventId}")) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            final String authHeader = request.getHeader("Authorization");
            final String jwt;
            final String username;

            // Vérifier si l'en-tête Authorization existe et commence par "Bearer "
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            try {
                // Extraire le token
                jwt = authHeader.substring(7);
                username = jwtService.extractUsername(jwt);

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    // Charger les détails de l'utilisateur
                    UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                    // Valider le token
                    if (jwtService.isTokenValid(jwt, userDetails)) {
                        // Créer l'objet d'authentification
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                        
                        // Définir les détails de l'authentification
                        authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                        );
                        
                        // Mettre à jour le contexte de sécurité
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        
                        logger.info("Authentifié l'utilisateur : " + username);
                    }
                }
            } catch (Exception e) {
                logger.error("Impossible d'authentifier l'utilisateur: ", e);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Erreur d'authentification");
                return;
            }
            
            filterChain.doFilter(request, response);
            
        } catch (Exception e) {
            logger.error("Erreur dans le filtre JWT: ", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Erreur interne du serveur");
        }
    }
}

