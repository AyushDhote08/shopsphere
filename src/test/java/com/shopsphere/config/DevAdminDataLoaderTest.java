package com.shopsphere.config;

import com.shopsphere.entity.Role;
import com.shopsphere.entity.User;
import com.shopsphere.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DevAdminDataLoaderTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private DevAdminDataLoader loader;

    @BeforeEach
    void setUp() {
        loader = new DevAdminDataLoader(userRepository, passwordEncoder);
        ReflectionTestUtils.setField(loader, "adminEmail", "admin@shopsphere.com");
        ReflectionTestUtils.setField(loader, "adminPassword", "Admin@12345");
    }

    @Test
    @DisplayName("Should seed dev admin when admin user does not exist")
    void shouldSeedDevAdminWhenNotExists() {
        when(userRepository.existsByEmailIgnoreCase("admin@shopsphere.com")).thenReturn(false);
        when(passwordEncoder.encode("Admin@12345")).thenReturn("hashed_dev_password");

        loader.run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("admin@shopsphere.com");
        assertThat(saved.getPasswordHash()).isEqualTo("hashed_dev_password");
        assertThat(saved.getRole()).isEqualTo(Role.ROLE_ADMIN);
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    @DisplayName("Should skip seeding dev admin when user already exists")
    void shouldSkipSeedingDevAdminWhenAlreadyExists() {
        when(userRepository.existsByEmailIgnoreCase("admin@shopsphere.com")).thenReturn(true);

        loader.run();

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }
}
