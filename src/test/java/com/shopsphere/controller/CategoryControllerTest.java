package com.shopsphere.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.dto.category.CategoryRequestDTO;
import com.shopsphere.dto.category.CategoryResponseDTO;
import com.shopsphere.exception.DuplicateResourceException;
import com.shopsphere.exception.ResourceNotFoundException;
import com.shopsphere.service.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    @Test
    @DisplayName("GET /api/categories: Should return list of active categories")
    void shouldReturnAllActiveCategories() throws Exception {
        CategoryResponseDTO dto1 = new CategoryResponseDTO();
        dto1.setId(1L);
        dto1.setName("Electronics");
        dto1.setSlug("electronics");

        when(categoryService.getAllActiveCategories()).thenReturn(List.of(dto1));

        mockMvc.perform(get("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("Electronics"));
    }

    @Test
    @DisplayName("GET /api/categories/{id}: Should return category when exists")
    void shouldReturnCategoryById() throws Exception {
        CategoryResponseDTO dto = new CategoryResponseDTO();
        dto.setId(1L);
        dto.setName("Audio");
        dto.setSlug("audio");

        when(categoryService.getCategoryById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Audio"));
    }

    @Test
    @DisplayName("GET /api/categories/{id}: Should return 404 when not found")
    void shouldReturn404WhenCategoryNotFound() throws Exception {
        when(categoryService.getCategoryById(999L))
                .thenThrow(new ResourceNotFoundException("Category", "id", 999L));

        mockMvc.perform(get("/api/categories/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(containsString("Category not found with id: '999'")));
    }

    @Test
    @DisplayName("POST /api/categories: Should create category and return 201")
    void shouldCreateCategorySuccessfully() throws Exception {
        CategoryRequestDTO request = new CategoryRequestDTO("Smart Watches", "smart-watches", "Wearables", null, true);

        CategoryResponseDTO response = new CategoryResponseDTO();
        response.setId(5L);
        response.setName("Smart Watches");
        response.setSlug("smart-watches");

        when(categoryService.createCategory(any(CategoryRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.name").value("Smart Watches"));
    }

    @Test
    @DisplayName("POST /api/categories: Should return 400 when name is blank")
    void shouldReturn400WhenCategoryNameIsBlank() throws Exception {
        CategoryRequestDTO invalidRequest = new CategoryRequestDTO("", null, "Description", null, true);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    @DisplayName("POST /api/categories: Should return 409 when category name or slug is duplicate")
    void shouldReturn409WhenCategoryAlreadyExists() throws Exception {
        CategoryRequestDTO duplicateRequest = new CategoryRequestDTO("Audio", "audio", "Duplicate", null, true);

        when(categoryService.createCategory(any(CategoryRequestDTO.class)))
                .thenThrow(new DuplicateResourceException("Category", "name", "Audio"));

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @DisplayName("DELETE /api/categories/{id}: Should return 204 No Content on deactivation")
    void shouldDeleteCategorySuccessfully() throws Exception {
        doNothing().when(categoryService).deleteCategory(1L);

        mockMvc.perform(delete("/api/categories/1"))
                .andExpect(status().isNoContent());
    }
}
