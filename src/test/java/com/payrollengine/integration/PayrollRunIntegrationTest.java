package com.payrollengine.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payrollengine.domain.Employee;
import com.payrollengine.domain.EmploymentType;
import com.payrollengine.domain.FilingStatus;
import com.payrollengine.domain.PayFrequency;
import com.payrollengine.repository.EmployeeRepository;
import com.payrollengine.repository.PayPeriodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full stack (HTTP -> service -> JPA -> H2) exercise of the golden path and,
 * critically, of idempotency: posting the exact same pay-run request twice
 * must produce one payroll, not two.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PayrollRunIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PayPeriodRepository payPeriodRepository;

    /** Every test in this class gets a blank company: no employees left
     * active from a previous test, since a pay run processes ALL active
     * employees. */
    @BeforeEach
    void deactivateLeftoverEmployees() {
        employeeRepository.findAll().forEach(e -> {
            e.setActive(false);
            employeeRepository.save(e);
        });
    }

    @Test
    void payingASalariedAndAnHourlyEmployeeProducesTwoBalancedPayStubs() throws Exception {
        Employee salaried = employeeRepository.save(new Employee(
                "Ada", "Lovelace", "1111", FilingStatus.SINGLE, EmploymentType.SALARY,
                PayFrequency.BIWEEKLY, new BigDecimal("104000.00"), null));
        Employee hourly = employeeRepository.save(new Employee(
                "Grace", "Hopper", "2222", FilingStatus.SINGLE, EmploymentType.HOURLY,
                PayFrequency.BIWEEKLY, null, new BigDecimal("50.00")));

        var period = payPeriodRepository.save(new com.payrollengine.domain.PayPeriod(
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 14), LocalDate.of(2024, 1, 19)));

        String requestBody = objectMapper.writeValueAsString(new com.payrollengine.dto.PayRunRequest(
                period.getId(),
                java.util.List.of(new com.payrollengine.dto.TimeEntryRequest(
                        hourly.getId(), new BigDecimal("80"), new BigDecimal("5")))
        ));

        mockMvc.perform(post("/api/pay-runs").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.idempotentReplay").value(false))
                .andExpect(jsonPath("$.payStubs", hasSize(2)));

        mockMvc.perform(get("/api/ledger/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanced").value(true));

        Employee reloadedSalaried = employeeRepository.findById(salaried.getId()).orElseThrow();
        assertThatYtdAdvancedExactlyOnce(reloadedSalaried);
    }

    private void assertThatYtdAdvancedExactlyOnce(Employee employee) {
        org.assertj.core.api.Assertions.assertThat(employee.getYtdGrossWages())
                .isEqualByComparingTo("4000.00"); // 104000 / 26, exactly one period's worth
    }

    @Test
    void postingTheSamePayPeriodTwiceIsIdempotent() throws Exception {
        employeeRepository.save(new Employee(
                "Katherine", "Johnson", "3333", FilingStatus.SINGLE, EmploymentType.SALARY,
                PayFrequency.MONTHLY, new BigDecimal("60000.00"), null));

        var period = payPeriodRepository.save(new com.payrollengine.domain.PayPeriod(
                LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29), LocalDate.of(2024, 3, 1)));

        String requestBody = objectMapper.writeValueAsString(
                new com.payrollengine.dto.PayRunRequest(period.getId(), java.util.List.of()));

        String firstResponse = mockMvc.perform(post("/api/pay-runs").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idempotentReplay").value(false))
                .andReturn().getResponse().getContentAsString();

        Long firstRunId = objectMapper.readTree(firstResponse).get("id").asLong();

        mockMvc.perform(post("/api/pay-runs").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk()) // 200, not 201 — this is a replay, not a new resource
                .andExpect(jsonPath("$.id").value(firstRunId))
                .andExpect(jsonPath("$.idempotentReplay").value(true))
                .andExpect(jsonPath("$.payStubs", hasSize(1)));

        // Re-processing must not have advanced YTD wages a second time.
        Employee employee = employeeRepository.findAll().stream()
                .filter(e -> e.getLastName().equals("Johnson")).findFirst().orElseThrow();
        org.assertj.core.api.Assertions.assertThat(employee.getYtdGrossWages())
                .isEqualByComparingTo("5000.00"); // 60000 / 12, exactly once
    }

    @Test
    void payRunForAnUnknownPayPeriodReturns404() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                new com.payrollengine.dto.PayRunRequest(999_999L, java.util.List.of()));

        mockMvc.perform(post("/api/pay-runs").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isNotFound());
    }

    @Test
    void hourlyEmployeeMissingATimeEntryIsRejectedAndNothingIsPersisted() throws Exception {
        employeeRepository.save(new Employee(
                "Marie", "Curie", "4444", FilingStatus.SINGLE, EmploymentType.HOURLY,
                PayFrequency.WEEKLY, null, new BigDecimal("45.00")));

        var period = payPeriodRepository.save(new com.payrollengine.domain.PayPeriod(
                LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 7), LocalDate.of(2024, 3, 12)));

        String requestBody = objectMapper.writeValueAsString(
                new com.payrollengine.dto.PayRunRequest(period.getId(), java.util.List.of())); // no time entry!

        mockMvc.perform(post("/api/pay-runs").contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isBadRequest());

        // The failed attempt must not have left a stuck PayRun behind — a
        // fresh attempt with the correct payload should succeed.
        String fixedBody = objectMapper.writeValueAsString(new com.payrollengine.dto.PayRunRequest(
                period.getId(),
                java.util.List.of(new com.payrollengine.dto.TimeEntryRequest(
                        employeeRepository.findAll().stream().filter(e -> e.getLastName().equals("Curie"))
                                .findFirst().orElseThrow().getId(),
                        new BigDecimal("40"), BigDecimal.ZERO))));

        mockMvc.perform(post("/api/pay-runs").contentType(MediaType.APPLICATION_JSON).content(fixedBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idempotentReplay").value(false));
    }
}
