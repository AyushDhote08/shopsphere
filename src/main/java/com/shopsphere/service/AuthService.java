package com.shopsphere.service;

import com.shopsphere.dto.auth.LoginRequestDTO;
import com.shopsphere.dto.auth.RegisterRequestDTO;
import com.shopsphere.dto.auth.UserResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    UserResponseDTO register(RegisterRequestDTO requestDTO);

    UserResponseDTO login(LoginRequestDTO loginRequest, HttpServletRequest request, HttpServletResponse response);

    UserResponseDTO getCurrentUser();

    void logout(HttpServletRequest request, HttpServletResponse response);
}
