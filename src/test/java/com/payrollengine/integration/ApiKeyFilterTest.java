package com.payrollengine.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The rest of the test suite disables the API key gate (see
 * src/test/resources/application.yml) so tests can focus on business logic.
 * This class re-enables it to prove the gate itself actually works. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.api-key=test-secret-key")
class ApiKeyFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void requestWithoutApiKeyIsRejected() throws Exception {
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requestWithWrongApiKeyIsRejected() throws Exception {
        mockMvc.perform(get("/api/employees").header("X-API-Key", "wrong-key"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requestWithCorrectApiKeyIsAccepted() throws Exception {
        mockMvc.perform(get("/api/employees").header("X-API-Key", "test-secret-key"))
                .andExpect(status().isOk());
    }

    @Test
    void actuatorHealthIsNeverGatedByTheApiKey() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
