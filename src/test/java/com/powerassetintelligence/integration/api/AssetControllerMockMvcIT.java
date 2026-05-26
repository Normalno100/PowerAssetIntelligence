package com.powerassetintelligence.integration.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powerassetintelligence.application.dto.AssetCreateRequest;
import com.powerassetintelligence.domain.model.AssetCriticality;
import com.powerassetintelligence.domain.model.AssetType;
import com.powerassetintelligence.testsupport.BaseIntegrationTest;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class AssetControllerMockMvcIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createShouldReturn201AndBody() throws Exception {
        AssetCreateRequest request = new AssetCreateRequest(
                AssetType.TRANSFORMER,
                "TX-999",
                LocalDate.of(2020, 10, 10),
                "Central",
                "Siemens",
                AssetCriticality.HIGH,
                35,
                Map.of("phase", "3")
        );

        mockMvc.perform(post("/api/v1/assets")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("TX-999"))
                .andExpect(jsonPath("$.criticality").value("HIGH"));
    }
}
