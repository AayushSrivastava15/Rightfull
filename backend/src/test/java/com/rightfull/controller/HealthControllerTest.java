package com.rightfull.controller;

import com.rightfull.common.controller.HealthController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HealthControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new HealthController()).build();
    }

    @Test
    @DisplayName("GET /api/v1/health should return UP status and platform metadata")
    void healthEndpointShouldReturnOkAndMetadata() throws Exception {
        mockMvc.perform(get("/api/v1/health").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("UP")))
                .andExpect(jsonPath("$.data.product", is("Rightfull")))
                .andExpect(jsonPath("$.data.version", is("0.0.1")))
                .andExpect(jsonPath("$.data.javaVersion", notNullValue()))
                .andExpect(jsonPath("$.data.springBootVersion", notNullValue()));
    }
}
