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
 * Seeds the development demo administrator account only when the 'dev' Spring profile is active.
 * Ensures local developers have out-of-the-box admin access for testing without creating
 * publicly known demo credentials in production.
 */
@Component
@Profile("dev")
public class DevAdminDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevAdminDataLoader.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${shopsphere.dev.admin.email:admin@shopsphere.com}")
    private String adminEmail;

    @Value("${shopsphere.dev.admin.password:Admin@12345}")
    private String adminPassword;

    public DevAdminDataLoader(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        String normalizedEmail = adminEmail.trim().toLowerCase();
        if (!userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            User admin = new User(
                    normalizedEmail,
                    passwordEncoder.encode(adminPassword),
                    "System Administrator",
                    "+1-555-0100",
                    Role.ROLE_ADMIN
            );
            userRepository.save(admin);
            log.info("Initialized development administrator account: {}", normalizedEmail);
        }
    }
}
