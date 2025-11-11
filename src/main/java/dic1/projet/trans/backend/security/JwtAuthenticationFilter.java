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
        String method = request.getMethod();
        
        // Chemins qui ne nécessitent pas d'authentification
        if (path.startsWith("/api/auth/") ||
            path.startsWith("/swagger-ui/") ||
            path.startsWith("/v3/api-docs") ||
            path.equals("/api/events/events") ||
            path.startsWith("/api/events/search") ||
            (path.matches("/api/events/[^/]+$") && method.equals("GET")) ||  // Pour /api/events/{eventId}
            (path.matches("/api/events/[^/]+/details$") && method.equals("GET"))) {  // Pour /api/events/{eventId}/details
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Jeton d'authentification manquant ou invalide");
            return;
        }

        try {
            final String jwt = authHeader.substring(7);
            final String username = jwtService.extractUsername(jwt);

            if (username == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Jeton JWT invalide");
                return;
            }

            // Charger les détails de l'utilisateur
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            // Valider le token
            if (!jwtService.isTokenValid(jwt, userDetails)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Jeton JWT expiré ou invalide");
                return;
            }

            // Créer l'objet d'authentification
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );
            
            // Définir les détails de l'authentification
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            
            // Mettre à jour le contexte de sécurité
            SecurityContextHolder.getContext().setAuthentication(authToken);
            
            logger.info("Utilisateur authentifié avec succès: " + username);
            
        } catch (Exception e) {
            logger.error("Échec de l'authentification: ", e);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Échec de l'authentification: " + e.getMessage());
            return;
        }
        
        filterChain.doFilter(request, response);
    }
}

