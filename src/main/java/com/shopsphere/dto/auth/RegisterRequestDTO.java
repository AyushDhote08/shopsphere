package com.shopsphere.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Customer Registration Request")
public class RegisterRequestDTO {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 120, message = "Full name must be between 2 and 120 characters")
    @Schema(description = "User's full legal name", example = "Jane Doe")
    private String fullName;

    @NotBlank(message = "Email address is required")
    @Email(message = "Must be a well-formed email address")
    @Size(max = 150, message = "Email cannot exceed 150 characters")
    @Schema(description = "User's email (serves as login identifier)", example = "jane.doe@example.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters long")
    @Schema(description = "Secure password (at least 8 characters)", example = "SecretPassword123!")
    private String password;

    @Pattern(regexp = "^$|^\\+?[0-9\\s-]{7,25}$", message = "Phone number must be a valid format or empty")
    @Schema(description = "Optional contact phone number", example = "+1-555-0199")
    private String phone;

    public RegisterRequestDTO() {
    }

    public RegisterRequestDTO(String fullName, String email, String password, String phone) {
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.phone = phone;
    }

    // Getters and Setters
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
