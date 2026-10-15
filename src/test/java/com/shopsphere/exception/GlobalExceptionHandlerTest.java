package com.shopsphere.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Malformed JSON payload returns 400 Bad Request without leaking internal Jackson details")
    void malformedJsonPayload_Returns400BadRequest() throws Exception {
        String malformedJson = "{ \"name\": \"Unclosed JSON string ";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Malformed request body or invalid data format"))
                .andExpect(content().string(not(containsString("com.fasterxml.jackson"))));
    }

    @Test
    @DisplayName("Method argument type mismatch on path variable returns 400 Bad Request")
    void typeMismatchPathVariable_Returns400BadRequest() throws Exception {
        mockMvc.perform(get("/api/products/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("Parameter 'id' should be of type 'Long'")));
    }

    @Test
    @DisplayName("Negative price range filter returns 400 Bad Request")
    void negativePriceRange_Returns400BadRequest() throws Exception {
        mockMvc.perform(get("/api/products?minPrice=-10.00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Minimum price cannot be negative"));
    }

    @Test
    @DisplayName("Inverted price range filter (minPrice > maxPrice) returns 400 Bad Request")
    void invertedPriceRange_Returns400BadRequest() throws Exception {
        mockMvc.perform(get("/api/products?minPrice=50.00&maxPrice=10.00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Minimum price cannot be greater than maximum price"));
    }

    @Test
    @DisplayName("Missing mandatory fields on registration returns 400 with field validation details")
    void missingMandatoryFields_Returns400WithFieldErrors() throws Exception {
        String invalidPayload = "{\"email\": \"invalid-email\", \"password\": \"\"}";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    @WithMockUser(username = "admin@shopsphere.com", roles = {"ADMIN"})
    @DisplayName("Inverted date range on admin dashboard returns 400 Bad Request")
    void invertedDateRange_Returns400BadRequest() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard?startDate=2026-12-31&endDate=2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("cannot be after end date")));
    }
}
