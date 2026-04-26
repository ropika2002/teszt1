/** Ez a Java fájl a backendhez kapcsolódó logikát és viselkedést tartalmazza. */
package com.royalcruise.backend.controller;

import com.royalcruise.backend.model.AuthResponse;
import com.royalcruise.backend.model.ErrorResponse;
import com.royalcruise.backend.model.LoginRequest;
import com.royalcruise.backend.model.RegisterRequest;
import com.royalcruise.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
// Ez a controller HTTP vegpontokat ad, bemeneti kerest fogad es a megfelelo service/repository logikara delegal.
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        // 1) Regisztrációs kérés feldolgozása és válasz DTO összeállítása.
        try {
            String token = authService.register(request);
            return authService.getUserByEmail(request.email())
                    .<ResponseEntity<?>>map(user -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(new AuthResponse(
                            token,
                            user.getEmail(),
                            user.getUsername(),
                            user.getRole().name(),
                            user.getAddress(),
                            user.getCountry(),
                            user.getPostalCode(),
                            user.getPhone(),
                            user.getGender(),
                            user.getCreatedAt() != null ? user.getCreatedAt().toString() : null
                        )))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(new ErrorResponse("Sikertelen regisztráció.")));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        // 1) Bejelentkezési kérés feldolgozása és az auth válasz visszaadása.
        try {
            String token = authService.login(request.email(), request.password());
            return authService.getUserByEmail(request.email())
                    .<ResponseEntity<?>>map(user -> ResponseEntity.ok(
                        new AuthResponse(
                            token,
                            user.getEmail(),
                            user.getUsername(),
                            user.getRole().name(),
                            user.getAddress(),
                            user.getCountry(),
                            user.getPostalCode(),
                            user.getPhone(),
                            user.getGender(),
                            user.getCreatedAt() != null ? user.getCreatedAt().toString() : null
                        )
                    ))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(new ErrorResponse("Hibás email vagy jelszó.")));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(ex.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        // 1) Bearer token kinyerése, érvényesség-ellenőrzés és felhasználó visszaadása.
        String token = extractBearerToken(authorization);
        if (token == null || !authService.isTokenValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Érvénytelen vagy hiányzó token."));
        }
        return authService.getUserByToken(token)
            .<ResponseEntity<?>>map(user -> ResponseEntity.ok(
                new AuthResponse(
                        token,
                        user.getEmail(),
                        user.getUsername(),
                        user.getRole().name(),
                        user.getAddress(),
                        user.getCountry(),
                        user.getPostalCode(),
                        user.getPhone(),
                        user.getGender(),
                        user.getCreatedAt() != null ? user.getCreatedAt().toString() : null
                )
            ))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Érvénytelen vagy hiányzó token.")));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        // 1) A stateless JWT miatt itt nincs szerveroldali tokenérvénytelenítés.
        return ResponseEntity.noContent().build();
    }

    private String extractBearerToken(String authorization) {
        // 1) Csak a szabályos Bearer fejlécből vágjuk ki a tényleges JWT tokent.
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7);
    }
}

