package com.shopsphere.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Authentication response with user details")
public class AuthResponseDTO {

    @Schema(description = "Success message", example = "Login successful")
    private String message;

    @Schema(description = "Authenticated user profile")
    private UserResponseDTO user;

    public AuthResponseDTO() {
    }

    public AuthResponseDTO(String message, UserResponseDTO user) {
        this.message = message;
        this.user = user;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public UserResponseDTO getUser() {
        return user;
    }

    public void setUser(UserResponseDTO user) {
        this.user = user;
    }
}
