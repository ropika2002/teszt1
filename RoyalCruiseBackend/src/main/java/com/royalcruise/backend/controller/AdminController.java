/** Ez a Java fájl a backendhez kapcsolódó logikát és viselkedést tartalmazza. */
package com.royalcruise.backend.controller;

import com.royalcruise.backend.model.AdminUserResponse;
import com.royalcruise.backend.model.ErrorResponse;
import com.royalcruise.backend.model.Role;
import com.royalcruise.backend.model.UserAccount;
import com.royalcruise.backend.repository.BookingRepository;
import com.royalcruise.backend.repository.UserAccountRepository;
import com.royalcruise.backend.service.AuthService;
import com.royalcruise.backend.service.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
// Ez a controller HTTP vegpontokat ad, bemeneti kerest fogad es a megfelelo service/repository logikara delegal.
public class AdminController {

    private final AuthService authService;
    private final UserAccountRepository userAccountRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    public AdminController(
            AuthService authService,
            UserAccountRepository userAccountRepository,
            BookingRepository bookingRepository,
            BookingService bookingService
    ) {
        this.authService = authService;
        this.userAccountRepository = userAccountRepository;
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
    }

    @GetMapping("/users")
    public ResponseEntity<?> getUsers(@RequestHeader(value = "Authorization", required = false) String authorization) {
        // 1) Csak admin token esetén szolgáljuk ki a teljes felhasználólistát.
        String token = extractBearerToken(authorization);
        if (!authService.isAdmin(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Nincs jogosultság az admin adatokhoz."));
        }

        try {
            List<AdminUserResponse> users = userAccountRepository.findAll().stream()
                    .map(user -> new AdminUserResponse(
                            user.getId(),
                            user.getUsername(),
                            user.getEmail(),
                            user.getAddress(),
                            user.getCountry(),
                            user.getPostalCode(),
                            user.getPhone(),
                            user.getGender(),
                            user.getRole() != null ? user.getRole().name() : "USER",
                            user.getCreatedAt()
                    ))
                    .toList();

            return ResponseEntity.ok(users);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Hiba az admin felhasználók lekérésében: " + ex.getMessage()));
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id
    ) {
        // 1) Admin jogosultság mellett töröljük a kiválasztott nem-admin felhasználót és a kapcsolódó foglalásait.
        String token = extractBearerToken(authorization);
        if (!authService.isAdmin(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Nincs jogosultság az admin adatokhoz."));
        }

        try {
            UserAccount user = userAccountRepository.findById(id).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("A felhasználó nem található."));
            }

            if (user.getRole() == Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ErrorResponse("Admin felhasználó nem törölhető."));
            }

            bookingRepository.deleteByUser(user);
            userAccountRepository.delete(user);
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Hiba a felhasználó törlésében: " + ex.getMessage()));
        }
    }

    @GetMapping("/bookings")
    public ResponseEntity<?> getBookings(@RequestHeader(value = "Authorization", required = false) String authorization) {
        // 1) Admin jogosultsággal lekérjük az összes foglalást.
        String token = extractBearerToken(authorization);
        if (!authService.isAdmin(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Nincs jogosultság az admin adatokhoz."));
        }

        try {
            return ResponseEntity.ok(bookingService.getAllBookings());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Hiba a foglalások lekérésében: " + ex.getMessage()));
        }
    }

    @DeleteMapping("/bookings/{id}")
    public ResponseEntity<?> deleteBooking(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id
    ) {
        // 1) Admin jogosultsággal töröljük a megadott foglalást.
        String token = extractBearerToken(authorization);
        if (!authService.isAdmin(token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Nincs jogosultság az admin adatokhoz."));
        }

        try {
            boolean deleted = bookingService.deleteBookingById(id);
            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("A foglalás nem található."));
            }
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Hiba a foglalás törlésében: " + ex.getMessage()));
        }
    }

    private String extractBearerToken(String authorization) {
        // 1) Csak a szabályos Bearer fejlécből vágjuk ki a JWT tokent.
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7);
    }

}

