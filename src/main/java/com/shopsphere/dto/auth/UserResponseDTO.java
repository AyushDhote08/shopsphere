package com.shopsphere.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Safe user profile representation")
public class UserResponseDTO {

    @Schema(description = "Unique user ID", example = "1")
    private Long id;

    @Schema(description = "User's full name", example = "Jane Doe")
    private String fullName;

    @Schema(description = "User's email", example = "jane.doe@example.com")
    private String email;

    @Schema(description = "User's phone number", example = "+1-555-0199")
    private String phone;

    @Schema(description = "Assigned security role", example = "ROLE_CUSTOMER")
    private String role;

    @Schema(description = "Whether account is currently active", example = "true")
    private boolean active;

    @Schema(description = "Account registration timestamp")
    private LocalDateTime createdAt;

    public UserResponseDTO() {
    }

    public UserResponseDTO(Long id, String fullName, String email, String phone, String role, boolean active, LocalDateTime createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.active = active;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
