package com.rightfull;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class RightfullApplicationTests {

    @Test
    void contextLoads() {
        // Confirms Spring Boot application context bootstraps cleanly on Java 25
    }
}


