package com.rightful.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/health should return UP status and platform metadata")
    void healthEndpointShouldReturnOkAndMetadata() throws Exception {
        mockMvc.perform(get("/api/v1/health").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("UP")))
                .andExpect(jsonPath("$.data.product", is("Rightful")))
                .andExpect(jsonPath("$.data.version", is("0.0.1")))
                .andExpect(jsonPath("$.data.defaultOfflineGracePeriodHours", is(7)))
                .andExpect(jsonPath("$.data.defaultValidationIntervalMinutes", is(5)))
                .andExpect(jsonPath("$.data.javaVersion", notNullValue()))
                .andExpect(jsonPath("$.data.springBootVersion", notNullValue()));
    }
}
