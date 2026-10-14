package com.shopsphere.repository;

import com.shopsphere.entity.Role;
import com.shopsphere.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User user = new User(
                "jane.doe@example.com",
                "$2a$12$hashedPasswordExample",
                "Jane Doe",
                "+1-555-0199",
                Role.ROLE_CUSTOMER
        );
        userRepository.save(user);
    }

    @Test
    @DisplayName("findByEmailIgnoreCase: Should find user regardless of letter casing")
    void shouldFindUserByEmailIgnoreCase() {
        Optional<User> found = userRepository.findByEmailIgnoreCase("JANE.DOE@EXAMPLE.COM");

        assertThat(found).isPresent();
        assertThat(found.get().getFullName()).isEqualTo("Jane Doe");
        assertThat(found.get().getRole()).isEqualTo(Role.ROLE_CUSTOMER);
    }

    @Test
    @DisplayName("existsByEmailIgnoreCase: Should return true for existing email")
    void shouldVerifyEmailExists() {
        assertThat(userRepository.existsByEmailIgnoreCase("jane.doe@example.com")).isTrue();
        assertThat(userRepository.existsByEmailIgnoreCase("JANE.DOE@EXAMPLE.COM")).isTrue();
        assertThat(userRepository.existsByEmailIgnoreCase("unknown@example.com")).isFalse();
    }
}
