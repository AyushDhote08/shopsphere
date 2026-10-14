package com.shopsphere;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ShopSphereApplicationTests {

    @Test
    @DisplayName("Context Loads: Verifies that the Spring Boot ApplicationContext starts without errors")
    void contextLoads() {
        // Test passes if the Spring ApplicationContext starts successfully
    }
}
