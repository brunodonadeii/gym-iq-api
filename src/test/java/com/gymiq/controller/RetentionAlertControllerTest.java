package com.gymiq.controller;

import com.gymiq.dto.response.RetentionAlertResponse;
import com.gymiq.service.RetentionAlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RetentionAlertControllerTest {

    @Mock
    private RetentionAlertService retentionAlertService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RetentionAlertController controller = new RetentionAlertController(retentionAlertService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void generateForOverdueStudentsShouldReturnGeneratedAlerts() throws Exception {
        RetentionAlertResponse alert = RetentionAlertResponse.builder()
                .retentionAlertId(6)
                .studentId(1)
                .studentName("Ana Silva")
                .studentEmail("ana@gymiq.com")
                .riskScore(70)
                .riskLevel("HIGH")
                .inactiveDays(15)
                .overduePayments(2)
                .message("Risco HIGH")
                .status("OPEN")
                .build();

        when(retentionAlertService.generateForOverdueStudents()).thenReturn(List.of(alert));

        mockMvc.perform(post("/api/retention-alerts/generate-overdue-students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentId").value(1))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].overduePayments").value(2));
    }
}
