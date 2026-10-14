package com.shopsphere.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordEncoderTest {

    @Test
    void testBCryptEncoding() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        String rawPassword = "Admin@12345";
        String encoded = encoder.encode(rawPassword);
        System.out.println("GENERATED_ADMIN_HASH: " + encoded);

        assertThat(encoder.matches(rawPassword, encoded)).isTrue();
        assertThat(encoder.matches("WrongPassword", encoded)).isFalse();
    }
}
