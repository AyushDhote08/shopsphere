package com.shopsphere.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.dto.auth.LoginRequestDTO;
import com.shopsphere.dto.auth.RegisterRequestDTO;
import com.shopsphere.dto.auth.UserResponseDTO;
import com.shopsphere.exception.DuplicateResourceException;
import com.shopsphere.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("POST /api/auth/register: Should return 201 on valid customer registration")
    void shouldRegisterCustomerSuccessfully() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO(
                "Jane Doe", "jane.doe@example.com", "SecurePassword123!", "+1-555-0199"
        );

        UserResponseDTO response = new UserResponseDTO(
                1L, "Jane Doe", "jane.doe@example.com", "+1-555-0199", "ROLE_CUSTOMER", true, LocalDateTime.now()
        );

        when(authService.register(any(RegisterRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("jane.doe@example.com"))
                .andExpect(jsonPath("$.data.role").value("ROLE_CUSTOMER"));
    }

    @Test
    @DisplayName("POST /api/auth/register: Should return 409 when email is already registered")
    void shouldReturn409OnDuplicateEmail() throws Exception {
        RegisterRequestDTO request = new RegisterRequestDTO(
                "Jane Doe", "existing@example.com", "SecurePassword123!", null
        );

        when(authService.register(any(RegisterRequestDTO.class)))
                .thenThrow(new DuplicateResourceException("User", "email", "existing@example.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @DisplayName("POST /api/auth/register: Should return 400 when validation rules fail")
    void shouldReturn400OnValidationFailure() throws Exception {
        RegisterRequestDTO invalidRequest = new RegisterRequestDTO(
                "", // Blank name
                "invalid-email", // Malformed email
                "short", // Password < 8 characters
                null
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors", hasSize(greaterThanOrEqualTo(3))));
    }

    @Test
    @DisplayName("POST /api/auth/login: Should return 200 on valid credentials")
    void shouldLoginSuccessfully() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("jane.doe@example.com", "SecurePassword123!");
        UserResponseDTO userResponse = new UserResponseDTO(
                1L, "Jane Doe", "jane.doe@example.com", "+1-555-0199", "ROLE_CUSTOMER", true, LocalDateTime.now()
        );

        when(authService.login(any(LoginRequestDTO.class), any(), any())).thenReturn(userResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.email").value("jane.doe@example.com"))
                .andExpect(jsonPath("$.data.user.role").value("ROLE_CUSTOMER"));
    }

    @Test
    @DisplayName("POST /api/auth/login: Should return 401 on bad credentials")
    void shouldReturn401OnInvalidCredentials() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("jane.doe@example.com", "WrongPassword");

        when(authService.login(any(LoginRequestDTO.class), any(), any()))
                .thenThrow(new BadCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("POST /api/auth/logout: Should return 200")
    @WithMockUser
    void shouldLogoutSuccessfully() throws Exception {
        doNothing().when(authService).logout(any(), any());

        mockMvc.perform(post("/api/auth/logout").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("GET /api/auth/me: Should return user profile when authenticated")
    @WithMockUser(username = "jane.doe@example.com", roles = "CUSTOMER")
    void shouldReturnCurrentUserWhenAuthenticated() throws Exception {
        UserResponseDTO userResponse = new UserResponseDTO(
                1L, "Jane Doe", "jane.doe@example.com", "+1-555-0199", "ROLE_CUSTOMER", true, LocalDateTime.now()
        );

        when(authService.getCurrentUser()).thenReturn(userResponse);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("jane.doe@example.com"));
    }

    @Test
    @DisplayName("GET /api/auth/me: Should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
