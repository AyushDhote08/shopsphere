package com.shopsphere.service;

import com.shopsphere.dto.auth.LoginRequestDTO;
import com.shopsphere.dto.auth.RegisterRequestDTO;
import com.shopsphere.dto.auth.UserResponseDTO;
import com.shopsphere.entity.Role;
import com.shopsphere.entity.User;
import com.shopsphere.exception.DuplicateResourceException;
import com.shopsphere.repository.UserRepository;
import com.shopsphere.security.UserPrincipal;
import com.shopsphere.service.impl.AuthServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private HttpServletResponse httpServletResponse;

    @Mock
    private HttpSession httpSession;

    @InjectMocks
    private AuthServiceImpl authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User(
                "jane.doe@example.com",
                "$2a$12$encodedPasswordHash",
                "Jane Doe",
                "+1-555-0199",
                Role.ROLE_CUSTOMER
        );
        sampleUser.setId(10L);
    }

    @Test
    @DisplayName("register: Successfully hashes password and assigns ROLE_CUSTOMER")
    void shouldRegisterCustomerSuccessfully() {
        RegisterRequestDTO request = new RegisterRequestDTO(
                "Jane Doe", "jane.doe@example.com", "Password123!", "+1-555-0199"
        );

        when(userRepository.existsByEmailIgnoreCase("jane.doe@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("$2a$12$encodedPasswordHash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });

        UserResponseDTO response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getEmail()).isEqualTo("jane.doe@example.com");
        assertThat(response.getRole()).isEqualTo("ROLE_CUSTOMER");

        verify(passwordEncoder).encode("Password123!");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register: Throws DuplicateResourceException when email is taken")
    void shouldRejectDuplicateEmailRegistration() {
        RegisterRequestDTO request = new RegisterRequestDTO(
                "Jane Doe", "jane.doe@example.com", "Password123!", null
        );

        when(userRepository.existsByEmailIgnoreCase("jane.doe@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("User already exists with email: 'jane.doe@example.com'");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("login: Authenticates and returns UserResponseDTO")
    void shouldLoginSuccessfully() {
        LoginRequestDTO request = new LoginRequestDTO("jane.doe@example.com", "Password123!");
        UserPrincipal principal = new UserPrincipal(sampleUser);
        Authentication authResult = mock(Authentication.class);
        when(authResult.getPrincipal()).thenReturn(principal);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authResult);
        when(userRepository.findById(10L)).thenReturn(Optional.of(sampleUser));
        when(httpServletRequest.getSession(anyBoolean())).thenReturn(httpSession);

        UserResponseDTO response = authService.login(request, httpServletRequest, httpServletResponse);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("jane.doe@example.com");
        assertThat(response.getFullName()).isEqualTo("Jane Doe");
    }

    @Test
    @DisplayName("login: Throws BadCredentialsException on authentication failure")
    void shouldThrowExceptionOnBadCredentials() {
        LoginRequestDTO request = new LoginRequestDTO("jane.doe@example.com", "WrongPassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request, httpServletRequest, httpServletResponse))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Bad credentials");
    }
}
