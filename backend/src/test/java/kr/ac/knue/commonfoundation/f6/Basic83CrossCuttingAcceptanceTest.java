package kr.ac.knue.commonfoundation.f6;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatus;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusHistory;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusTransitionPolicy;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementStatusTransitionRequest;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.RequestIdContext;
import kr.ac.knue.commonfoundation.common.api.RequestIdFilter;
import kr.ac.knue.commonfoundation.health.HealthController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Exercises the shared lifecycle, request-tracing, and health boundaries that
 * BASIC-83 achievement slices must preserve during integration.
 */
class Basic83CrossCuttingAcceptanceTest {
    @AfterEach
    void clearRequestIdContext() {
        RequestIdContext.clear();
    }

    @Test
    void permittedLifecycleTransitionKeepsCompleteHistoryAndRejectsInvalidOrUnexplainedChanges() {
        EducationAchievementStatusTransitionPolicy policy = new EducationAchievementStatusTransitionPolicy();
        LocalDateTime processedAt = LocalDateTime.parse("2026-10-02T10:15:30");

        EducationAchievementStatusHistory history = policy.transition(
                new EducationAchievementStatusTransitionRequest(
                        "LECTURE",
                        83L,
                        EducationAchievementStatus.SUBMITTED,
                        EducationAchievementStatus.DEPARTMENT_CONFIRMED,
                        "DEPARTMENT_CONFIRM",
                        null,
                        "학과장 확인",
                        101L,
                        processedAt));

        assertThat(history.previousStatus()).isEqualTo(EducationAchievementStatus.SUBMITTED);
        assertThat(history.nextStatus()).isEqualTo(EducationAchievementStatus.DEPARTMENT_CONFIRMED);
        assertThat(history.processedBy()).isEqualTo(101L);
        assertThat(history.processedAt()).isEqualTo(processedAt);
        assertThatThrownBy(() -> policy.transition(
                new EducationAchievementStatusTransitionRequest(
                        "LECTURE",
                        83L,
                        EducationAchievementStatus.DRAFT,
                        EducationAchievementStatus.CERTIFIED,
                        "CERTIFY",
                        null,
                        null,
                        101L,
                        processedAt)))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> policy.transition(
                new EducationAchievementStatusTransitionRequest(
                        "LECTURE",
                        83L,
                        EducationAchievementStatus.SUBMITTED,
                        EducationAchievementStatus.DEPARTMENT_REJECTED,
                        "DEPARTMENT_REJECT",
                        " ",
                        null,
                        101L,
                        processedAt)))
                .isInstanceOf(BusinessValidationException.class);
    }

    @Test
    void healthEndpointIsTraceableAndAverageStandaloneResponseIsBelowThreeSeconds() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new HealthController())
                .addFilters(new RequestIdFilter())
                .build();
        List<Long> elapsedNanos = new ArrayList<>();

        for (int index = 1; index <= 3; index++) {
            String requestId = "B83-HEALTH-" + index;
            long startedAt = System.nanoTime();
            mockMvc.perform(get("/api/health")
                            .header(RequestIdFilter.HEADER_NAME, requestId))
                    .andExpect(status().isOk())
                    .andExpect(header().string(RequestIdFilter.HEADER_NAME, requestId))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("UP"))
                    .andExpect(jsonPath("$.meta.requestId").value(requestId));
            elapsedNanos.add(System.nanoTime() - startedAt);
        }

        double averageNanos = elapsedNanos.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElseThrow();
        assertThat(averageNanos).isLessThan((double) Duration.ofSeconds(3).toNanos());
    }

    @Test
    void requestIdentifierFilterDoesNotReflectUnsafeInputInHealthResponses() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new HealthController())
                .addFilters(new RequestIdFilter())
                .build();

        mockMvc.perform(get("/api/health")
                        .header(RequestIdFilter.HEADER_NAME, "unsafe request id"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        RequestIdFilter.HEADER_NAME,
                        org.hamcrest.Matchers.not("unsafe request id")))
                .andExpect(jsonPath("$.meta.requestId").isNotEmpty());
    }
}
