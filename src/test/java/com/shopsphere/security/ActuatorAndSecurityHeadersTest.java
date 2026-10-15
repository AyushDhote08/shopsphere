package com.shopsphere.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ActuatorAndSecurityHeadersTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /actuator/health is publicly accessible and returns 200 UP")
    void healthEndpoint_PubliclyAccessible_Returns200Up() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("HTTP responses contain hardened security headers")
    void securityHeaders_PresentOnPublicEndpoints() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")));
    }

    @Test
    @DisplayName("Unauthenticated access to protected actuator endpoints returns 401")
    void actuatorRoot_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/actuator"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @WithMockUser(username = "customer@example.com", roles = {"CUSTOMER"})
    @DisplayName("Customer role access to protected actuator endpoints returns 403")
    void actuatorRoot_CustomerRole_Returns403() throws Exception {
        mockMvc.perform(get("/actuator"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @WithMockUser(username = "admin@shopsphere.com", roles = {"ADMIN"})
    @DisplayName("Admin role access to actuator root returns 200")
    void actuatorRoot_AdminRole_Returns200() throws Exception {
        mockMvc.perform(get("/actuator"))
                .andExpect(status().isOk());
    }
}
