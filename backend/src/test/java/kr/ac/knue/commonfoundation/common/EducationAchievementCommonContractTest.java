package kr.ac.knue.commonfoundation.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.LectureAchievementController;
import kr.ac.knue.commonfoundation.basic81.LectureAchievementRow;
import kr.ac.knue.commonfoundation.basic81.LectureAchievementSaveResult;
import kr.ac.knue.commonfoundation.basic81.LectureAchievementService;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Pins the shared validation/security/error envelope using the real existing lecture controller.
 * This is a legacy compatibility fixture, not coverage of the new education endpoints or DB guards.
 */
@WebMvcTest(LectureAchievementController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EducationAchievementCommonContractTest {
    private static final String LEGACY_PATH = "/api/business/lecture-achievements";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private LectureAchievementService service;

    @Test
    void approvedFixtureDoesNotAcceptEditableStatusOrOwner() {
        var request = EducationAchievementContractFixtures.request(objectMapper, "FR-030");
        assertThat(request.path("achievementDate").asText()).isEqualTo("2026-04-10");
        assertThat(request.has("teacherUserId")).isFalse();
        assertThat(request.has("achievementStatus")).isFalse();
        assertThat(request.has("achievementId")).isFalse();
    }

    @Test
    void missingDateReturnsFieldArrayBeforeServiceRuns() throws Exception {
        mockMvc.perform(post(LEGACY_PATH)
                        .requestAttr("currentUser", EducationAchievementContractFixtures.principal("R01"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managementItemCode\":\"LECTURE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[?(@.field == 'occurredDate')]").isNotEmpty())
                .andExpect(jsonPath("$.meta.traceId").isString());
        verifyNoInteractions(service);
    }

    @Test
    void noPrincipalReturns401BeforeListServiceRuns() throws Exception {
        mockMvc.perform(get(LEGACY_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"R07", "R09"})
    void nonBusinessRolesCannotUseTheLegacyWriteBoundary(String roleCode) throws Exception {
        mockMvc.perform(post(LEGACY_PATH)
                        .requestAttr("currentUser", EducationAchievementContractFixtures.principal(roleCode))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validLegacyBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verifyNoInteractions(service);
    }

    @Test
    void legacyPeriodConflictRetainsTheBusinessToken() throws Exception {
        when(service.save(any(), any(), any()))
                .thenThrow(new ConflictException("PERIOD_NOT_ACTIVE: 입력기간이 아닙니다."));
        mockMvc.perform(post(LEGACY_PATH)
                        .requestAttr("currentUser", EducationAchievementContractFixtures.principal("R01"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validLegacyBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                .andExpect(jsonPath("$.error.message").value(containsString("PERIOD_NOT_ACTIVE")));
    }

    @Test
    void occurredDateWarningAndRequestIdAreReturnedWithoutChangingLegacySerialization() throws Exception {
        CurrentUser owner = EducationAchievementContractFixtures.principal("R01");
        LectureAchievementRow row = new LectureAchievementRow(
                82L,
                "education-legacy-fixture",
                owner.userId(),
                owner.name(),
                "2026",
                "LECTURE",
                EducationAchievementContractFixtures.OUTSIDE_PERIOD_DATE,
                "{}",
                "DRAFT",
                null,
                LocalDateTime.of(2026, 4, 10, 9, 0),
                LocalDateTime.of(2026, 4, 10, 9, 0));
        when(service.save(any(), eq(owner), eq(EducationAchievementContractFixtures.REQUEST_ID)))
                .thenReturn(new LectureAchievementSaveResult(row, true, "평가기간 밖 발생일 경고"));

        mockMvc.perform(post(LEGACY_PATH)
                        .requestAttr("currentUser", owner)
                        .header("X-Request-Id", EducationAchievementContractFixtures.REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validLegacyBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.achievement.occurredDate").value("2025-12-31"))
                .andExpect(jsonPath("$.data.occurredDateWarning").value(true))
                .andExpect(jsonPath("$.meta.requestId").value(EducationAchievementContractFixtures.REQUEST_ID))
                .andExpect(jsonPath("$.meta.traceId").isString());
        verify(service).save(any(), eq(owner), eq(EducationAchievementContractFixtures.REQUEST_ID));
    }

    @Test
    void unexpectedPersistenceFailureDoesNotLeakImplementationDetails() throws Exception {
        when(service.save(any(), any(), any()))
                .thenThrow(new IllegalStateException("private-storage-implementation-detail"));
        mockMvc.perform(post(LEGACY_PATH)
                        .requestAttr("currentUser", EducationAchievementContractFixtures.principal("R01"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validLegacyBody()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.message").value(not(containsString("private-storage"))));
    }

    private String validLegacyBody() throws Exception {
        var body = objectMapper.createObjectNode();
        body.put("managementItemCode", "LECTURE");
        body.put("occurredDate", EducationAchievementContractFixtures.OUTSIDE_PERIOD_DATE.toString());
        return objectMapper.writeValueAsString(body);
    }
}
