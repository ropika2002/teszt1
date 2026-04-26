/** Ez a Java fájl a backendhez kapcsolódó logikát és viselkedést tartalmazza. */
package com.royalcruise.backend.config;

import com.royalcruise.backend.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter
 * Validates JWT token from Authorization header
 */
@Component
// Ez a filter minden keresnel kinyeri es ellenorzi a Bearer JWT tokent, majd beallitja az autentikaciot.
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthService authService;

    public JwtAuthenticationFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // 1) A kérés Authorization fejlécéből kiolvassuk a JWT-t, ha van.
        String authorization = request.getHeader("Authorization");
        
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            
            // 2) Csak a védett végpontoknál állítjuk meg a kérést érvénytelen token esetén.
            if (!authService.isTokenValid(token)) {
                String path = request.getRequestURI();
                if (isProtectedEndpoint(path)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"message\": \"Érvénytelen vagy lejárt token\"}");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isProtectedEndpoint(String path) {
        // 1) Azokat az útvonalakat soroljuk ide, ahol a tokenellenőrzés ténylegesen kötelező.
        return path.startsWith("/api/bookings") ||
               path.startsWith("/api/admin") ||
               path.equals("/api/auth/me");
    }
}

