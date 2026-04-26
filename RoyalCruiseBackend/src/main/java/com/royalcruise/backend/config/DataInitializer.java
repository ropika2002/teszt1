/** Ez a Java fájl a backendhez kapcsolódó logikát és viselkedést tartalmazza. */
package com.royalcruise.backend.config;

import com.royalcruise.backend.model.Role;
import com.royalcruise.backend.model.UserAccount;
import com.royalcruise.backend.repository.UserAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
// Ez a konfiguracios osztaly indulaskor ellenorzi es szukseg eseten letrehozza az admin felhasznalot.
public class DataInitializer {

    private static final String ADMIN_EMAIL = "admin@royalcruise.local";
    private static final String ADMIN_RAW_PASSWORD = "Admin-SZIKSZI";

    @Bean
    CommandLineRunner initAdminUser(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        // 1) Az induló admin fiók létrehozása vagy javítása mindig lefut alkalmazásinduláskor.
        return args -> {
            // 2) Megnézzük, létezik-e már az admin felhasználó az adatbázisban.
            UserAccount admin = userAccountRepository.findByEmail(ADMIN_EMAIL).orElse(null);

            if (admin == null) {
                // 3) Ha még nincs admin, akkor létrehozzuk az alapértelmezett jogosultsággal.
                UserAccount newAdmin = new UserAccount();
                newAdmin.setUsername("admin");
                newAdmin.setEmail(ADMIN_EMAIL);
                newAdmin.setPassword(passwordEncoder.encode(ADMIN_RAW_PASSWORD));
                newAdmin.setAddress("Nyíregyháza");
                newAdmin.setCountry("Hungary");
                newAdmin.setPostalCode("4400");
                newAdmin.setPhone("+3610000000");
                newAdmin.setGender("Férfi");
                newAdmin.setRole(Role.ADMIN);
                userAccountRepository.save(newAdmin);
                return;
            }

            // 4) A korábbi seedelt admin rekordok jelszavát is újrakódoljuk, ha még nem BCrypt formátumú.
            if (!passwordEncoder.matches(ADMIN_RAW_PASSWORD, admin.getPassword())) {
                admin.setPassword(passwordEncoder.encode(ADMIN_RAW_PASSWORD));
                admin.setRole(Role.ADMIN);
                userAccountRepository.save(admin);
            }
        };
    }
}
