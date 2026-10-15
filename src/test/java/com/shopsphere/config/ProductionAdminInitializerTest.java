package com.shopsphere.config;

import com.shopsphere.entity.Role;
import com.shopsphere.entity.User;
import com.shopsphere.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductionAdminInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ProductionAdminInitializer initializer;

    @Test
    @DisplayName("Should create production admin when credentials provided and user does not exist")
    void shouldCreateAdminWhenCredentialsProvidedAndNotExists() {
        ReflectionTestUtils.setField(initializer, "adminEmail", "prod-admin@company.com");
        ReflectionTestUtils.setField(initializer, "adminPassword", "SuperSecureRandomPass#99");

        when(userRepository.existsByEmailIgnoreCase("prod-admin@company.com")).thenReturn(false);
        when(passwordEncoder.encode("SuperSecureRandomPass#99")).thenReturn("prod_hash_123");

        initializer.run();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("prod-admin@company.com");
        assertThat(saved.getPasswordHash()).isEqualTo("prod_hash_123");
        assertThat(saved.getRole()).isEqualTo(Role.ROLE_ADMIN);
    }

    @Test
    @DisplayName("Should skip creation when admin email or password is null or blank")
    void shouldSkipWhenCredentialsBlank() {
        ReflectionTestUtils.setField(initializer, "adminEmail", "");
        ReflectionTestUtils.setField(initializer, "adminPassword", "");

        initializer.run();

        verify(userRepository, never()).existsByEmailIgnoreCase(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should skip creation when admin email already exists in production")
    void shouldSkipWhenAdminAlreadyExists() {
        ReflectionTestUtils.setField(initializer, "adminEmail", "prod-admin@company.com");
        ReflectionTestUtils.setField(initializer, "adminPassword", "SuperSecureRandomPass#99");

        when(userRepository.existsByEmailIgnoreCase("prod-admin@company.com")).thenReturn(true);

        initializer.run();

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }
}
