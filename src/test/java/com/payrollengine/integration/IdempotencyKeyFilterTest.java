package com.payrollengine.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payrollengine.dto.PayPeriodRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves the client-supplied Idempotency-Key mechanism independently of the
 * pay-run-specific business-level idempotency (see PayrollRunIntegrationTest
 * for that one). This one applies to any POST, using nothing but the
 * header — POST /api/pay-periods has no business-level idempotency of its
 * own, so it's a clean way to test this mechanism in isolation.
 */
@SpringBootTest
@AutoConfigureMockMvc
class IdempotencyKeyFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void sameKeyAndSameBodyReplaysTheOriginalResponseInsteadOfCreatingASecondResource() throws Exception {
        String body = objectMapper.writeValueAsString(new PayPeriodRequest(
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 14), LocalDate.of(2025, 1, 19)));

        String firstResponse = mockMvc.perform(post("/api/pay-periods")
                        .header("Idempotency-Key", "test-key-alpha")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long firstId = objectMapper.readTree(firstResponse).get("id").asLong();

        // Same key, identical body: must return the ORIGINAL response,
        // proving no second pay period was created.
        mockMvc.perform(post("/api/pay-periods")
                        .header("Idempotency-Key", "test-key-alpha")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(firstId));
    }

    @Test
    void sameKeyWithADifferentBodyIsRejected() throws Exception {
        String bodyA = objectMapper.writeValueAsString(new PayPeriodRequest(
                LocalDate.of(2025, 2, 1), LocalDate.of(2025, 2, 14), LocalDate.of(2025, 2, 19)));
        String bodyB = objectMapper.writeValueAsString(new PayPeriodRequest(
                LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 14), LocalDate.of(2025, 3, 19)));

        mockMvc.perform(post("/api/pay-periods")
                        .header("Idempotency-Key", "test-key-beta")
                        .contentType(MediaType.APPLICATION_JSON).content(bodyA))
                .andExpect(status().isCreated());

        // Same key, different body: this is a client bug, not a safe retry.
        mockMvc.perform(post("/api/pay-periods")
                        .header("Idempotency-Key", "test-key-beta")
                        .contentType(MediaType.APPLICATION_JSON).content(bodyB))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void requestsWithoutAnIdempotencyKeyAreUnaffected() throws Exception {
        String body = objectMapper.writeValueAsString(new PayPeriodRequest(
                LocalDate.of(2025, 4, 1), LocalDate.of(2025, 4, 14), LocalDate.of(2025, 4, 19)));

        // No header at all — two identical POSTs create two distinct resources,
        // exactly as a plain (non-idempotent-by-default) POST should.
        String firstId = objectMapper.readTree(mockMvc.perform(post("/api/pay-periods")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        String secondId = objectMapper.readTree(mockMvc.perform(post("/api/pay-periods")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        org.assertj.core.api.Assertions.assertThat(secondId).isNotEqualTo(firstId);
    }
}
