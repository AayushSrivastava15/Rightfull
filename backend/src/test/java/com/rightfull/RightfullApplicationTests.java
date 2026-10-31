package com.rightfull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class RightfullApplicationTests {

    @Value("${spring.application.name}")
    private String applicationName;

    @Test
    @DisplayName("Spring application context bootstraps cleanly on Java 25 and loads properties")
    void contextLoads() {
        assertNotNull(applicationName);
        assertEquals("rightfull-backend", applicationName);
    }
}
