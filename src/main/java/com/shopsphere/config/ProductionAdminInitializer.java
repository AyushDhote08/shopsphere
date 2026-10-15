package com.shopsphere.config;

import com.shopsphere.entity.Role;
import com.shopsphere.entity.User;
import com.shopsphere.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Initializes the primary production administrator account if explicit environment variables
 * (APP_ADMIN_EMAIL and APP_ADMIN_PASSWORD) are supplied to the application.
 * If these variables are omitted, no administrator account is created automatically,
 * requiring explicit database promotion (e.g. UPDATE users SET role = 'ROLE_ADMIN' WHERE email = '...').
 */
@Component
@Profile("prod")
public class ProductionAdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductionAdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${APP_ADMIN_EMAIL:}")
    private String adminEmail;

    @Value("${APP_ADMIN_PASSWORD:}")
    private String adminPassword;

    public ProductionAdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            log.info("No APP_ADMIN_EMAIL / APP_ADMIN_PASSWORD configured. Skipping automated production admin initialization.");
            return;
        }

        String normalizedEmail = adminEmail.trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            log.info("Production admin account already exists for: {}. Skipping creation.", normalizedEmail);
            return;
        }

        User admin = new User(
                normalizedEmail,
                passwordEncoder.encode(adminPassword.trim()),
                "Production Administrator",
                null,
                Role.ROLE_ADMIN
        );
        userRepository.save(admin);
        log.info("Initialized initial production administrator account: {}", normalizedEmail);
    }
}
