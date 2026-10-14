package com.shopsphere.controller;

import com.shopsphere.dto.auth.AuthResponseDTO;
import com.shopsphere.dto.auth.LoginRequestDTO;
import com.shopsphere.dto.auth.RegisterRequestDTO;
import com.shopsphere.dto.auth.UserResponseDTO;
import com.shopsphere.dto.common.ApiResponse;
import com.shopsphere.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication & Authorization", description = "Endpoints for customer registration, session login, logout, and current user profile")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register customer", description = "Creates a new customer account with BCrypt password hashing and default ROLE_CUSTOMER")
    public ResponseEntity<ApiResponse<UserResponseDTO>> register(@Valid @RequestBody RegisterRequestDTO requestDTO) {
        UserResponseDTO user = authService.register(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful. Please log in.", user));
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates user credentials and establishes a secure HTTP session")
    public ResponseEntity<ApiResponse<AuthResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO loginRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        UserResponseDTO user = authService.login(loginRequest, request, response);
        AuthResponseDTO authResponse = new AuthResponseDTO("Login successful", user);
        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }

    @PostMapping("/logout")
    @Operation(summary = "User logout", description = "Invalidates the current session and clears the security context")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ResponseEntity.ok(ApiResponse.success("Logout successful", "Session cleared"));
    }

    @GetMapping("/me")
    @Operation(summary = "Current user profile", description = "Retrieves the profile of the currently authenticated user")
    public ResponseEntity<ApiResponse<UserResponseDTO>> getCurrentUser() {
        UserResponseDTO user = authService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @GetMapping("/csrf")
    @Operation(summary = "CSRF Token provider", description = "Returns and attaches CSRF cookie for same-origin client requests")
    public ResponseEntity<ApiResponse<String>> getCsrfToken(CsrfToken csrfToken) {
        return ResponseEntity.ok(ApiResponse.success("CSRF token initialized", csrfToken != null ? csrfToken.getToken() : null));
    }
}
