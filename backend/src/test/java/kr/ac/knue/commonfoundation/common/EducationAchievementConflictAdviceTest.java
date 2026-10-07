package kr.ac.knue.commonfoundation.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.EducationAchievementConflictAdvice;
import kr.ac.knue.commonfoundation.common.api.EducationAchievementConflictException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.common.api.RequestIds;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Tests shared advice through HTTP only; the probe is test-local, not a new business endpoint. */
class EducationAchievementConflictAdviceTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUpStandaloneProbe() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ConflictProbeController())
                .setControllerAdvice(new EducationAchievementConflictAdvice(), new GlobalExceptionHandler())
                .build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"PERIOD_NOT_ACTIVE", "CONFIRMED_DATA_LOCKED"})
    void typedConflictUsesExactCodeAndCommandRequestId(String code) throws Exception {
        mockMvc.perform(post("/api/probe/conflicts/{code}", code)
                        .header("X-Request-Id", "  command-correlation  "))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value(code))
                .andExpect(jsonPath("$.error.message").value("현재 업무 조건에서는 저장할 수 없습니다."))
                .andExpect(jsonPath("$.error.fields").isArray())
                .andExpect(jsonPath("$.meta.requestId").value("command-correlation"));
    }

    @Test
    void legacyConflictRemainsGeneric() throws Exception {
        mockMvc.perform(post("/api/probe/conflicts/legacy"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                .andExpect(jsonPath("$.error.message").value("기존 충돌"));
    }

    @Test
    void unexpectedErrorsDoNotLeakSensitiveDetails() throws Exception {
        mockMvc.perform(post("/api/probe/conflicts/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.message").value(
                        "오류가 발생했습니다. 잠시 후 다시 시도하거나 관리자에게 문의하세요."));
    }

    @Test
    void blankRequestIdIsGeneratedOnceAndReused() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", " ");
        String requestId = RequestIds.resolve(request);
        assertThat(UUID.fromString(requestId).toString()).isEqualTo(requestId);
        assertThat(RequestIds.resolve(request)).isEqualTo(requestId);
        assertThat(ApiResponse.ok("저장됨", requestId).meta()).containsEntry("requestId", requestId);
    }

    @RestController
    public static class ConflictProbeController {
        @PostMapping("/api/probe/conflicts/{code}")
        public ApiResponse<Void> conflict(@PathVariable String code, HttpServletRequest request) {
            if ("legacy".equals(code)) {
                throw new ConflictException("기존 충돌");
            }
            if ("unexpected".equals(code)) {
                throw new IllegalStateException("internal diagnostic detail");
            }
            throw new EducationAchievementConflictException(
                    code, "현재 업무 조건에서는 저장할 수 없습니다.", RequestIds.resolve(request));
        }
    }
}
