package kr.ac.knue.commonfoundation.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import kr.ac.knue.commonfoundation.common.api.ApiError;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.EducationAchievementRequestFilter;
import kr.ac.knue.commonfoundation.common.api.EducationAchievementResponseAdvice;
import kr.ac.knue.commonfoundation.common.api.EducationAchievementRoutes;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;

/** Cross-cutting transport probes, following ApiEnvelopeTest's standalone setup (not feature API tests). */
class EducationAchievementFoundationTest {
    private static final String PATH = "/api/business/employment-rate-improvements";
    private MockMvc mockMvc;

    @BeforeEach
    void setUpStandaloneProbe() {
        mockMvc = MockMvcBuilders.standaloneSetup(new EducationEnvelopeProbe())
                .setControllerAdvice(new GlobalExceptionHandler(), new EducationAchievementResponseAdvice())
                .addFilters(new EducationAchievementRequestFilter())
                .build();
    }

    @ParameterizedTest
    @CsvSource({
        "/api/business/employment-rate-improvements,/faculty/employment-rate-improvement-achievements",
        "/api/business/course-operations,/faculty/course-offering-operation-achievements",
        "/api/business/lecture-improvements,/faculty/teaching-improvement-achievements",
        "/api/business/employment-rate-achievements,/faculty/employment-rate-achievements"
    })
    void mapsCollectionAndEverySubresourceToOneMenu(String api, String ui) {
        assertThat(EducationAchievementRoutes.uiRouteForApiPath(api)).isEqualTo(ui);
        assertThat(EducationAchievementRoutes.uiRouteForApiPath(api + "/123")).isEqualTo(ui);
        assertThat(EducationAchievementRoutes.uiRouteForApiPath(api + "/bulk-jobs/opaque-id")).isEqualTo(ui);
        assertThat(EducationAchievementRoutes.uiRouteForApiPath(api + "-other")).isNull();
    }

    @Test
    void leavesLegacyRoutesUnregistered() {
        assertThat(EducationAchievementRoutes.uiRouteForApiPath("/api/business/lecture-achievements")).isNull();
        assertThat(EducationAchievementRoutes.uiRouteForApiPath(null)).isNull();
    }

    @Test
    void successfulResponseAndControllerUseTheSameGeneratedId() throws Exception {
        var result = mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requiredValue\":\"normal\"}"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.meta.requestId").exists())
                .andReturn();
        String id = result.getResponse().getHeader("X-Request-Id");
        var tree = new ObjectMapper().readTree(result.getResponse().getContentAsString());
        assertThat(tree.path("data").asText()).isEqualTo(id);
        assertThat(tree.path("meta").path("requestId").asText()).isEqualTo(id);
        assertThat(tree.path("meta").path("traceId").asText()).isNotEqualTo(id);
    }

    @Test
    void preservesSuppliedRequestIdOnValidationError() throws Exception {
        mockMvc.perform(post(PATH).header("X-Request-Id", "request-validation-01")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Request-Id", "request-validation-01"))
                .andExpect(jsonPath("$.meta.requestId").value("request-validation-01"))
                .andExpect(jsonPath("$.error.fields.requiredValue").value("필수값입니다."));
    }

    @Test
    void generatedRequestIdAlsoExistsOnValidationFailure() throws Exception {
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.meta.requestId").isNotEmpty())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void businessValidationHasNamedFieldProperties() throws Exception {
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requiredValue\":\"business-validation\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields.managementItemCode").value("유효한 항목을 선택하세요."));
    }

    @Test
    void projectsTypedPeriodConflictWithoutChanging409() throws Exception {
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requiredValue\":\"conflict\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PERIOD_NOT_ACTIVE"))
                .andExpect(jsonPath("$.error.message").value("입력기간이 아닙니다."))
                .andExpect(jsonPath("$.error.fields").isMap())
                .andExpect(jsonPath("$.meta.requestId").exists());
    }

    @Test
    void legacyValidationKeepsArrayFieldsAndNoNewRequestMetadata() throws Exception {
        mockMvc.perform(post("/api/probe").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fields[0].field").value("requiredValue"))
                .andExpect(jsonPath("$.meta.requestId").doesNotExist())
                .andExpect(header().doesNotExist("X-Request-Id"));
    }

    @Test
    void legacyConflictsKeepTheirOriginalCodeAndMessage() throws Exception {
        mockMvc.perform(post("/api/probe").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requiredValue\":\"conflict\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"))
                .andExpect(jsonPath("$.error.message").value("PERIOD_NOT_ACTIVE: 입력기간이 아닙니다."))
                .andExpect(jsonPath("$.error.fields").isArray());
    }

    @Test
    void unexpectedFailureDoesNotExposeSensitiveDetails() throws Exception {
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requiredValue\":\"unexpected\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.message").value(
                        "오류가 발생했습니다. 잠시 후 다시 시도하거나 관리자에게 문의하세요."))
                .andExpect(jsonPath("$.meta.requestId").exists());
    }

    @Test
    void malformedJsonIs400WithoutParserLeakage() throws Exception {
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{broken-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields.body").value("입력 형식이 올바르지 않습니다."))
                .andExpect(jsonPath("$.meta.requestId").exists());
    }

    @Test
    void legacyMalformedJsonKeepsItsExistingTranslation() throws Exception {
        mockMvc.perform(post("/api/probe").contentType(MediaType.APPLICATION_JSON).content("{broken-json"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.fields").isArray());
    }

    @Test
    void authenticationLayerCanSerializeErrorWithTheSameIdentifier() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", PATH);
        MockHttpServletResponse response = new MockHttpServletResponse();
        new EducationAchievementRequestFilter().doFilter(request, response, (req, res) -> {
            var error = ApiResponse.fail(ApiError.of("UNAUTHENTICATED", "인증이 필요합니다."));
            assertThat(error.meta().get("requestId")).isEqualTo(req.getAttribute("requestId"));
            assertThat(((jakarta.servlet.http.HttpServletRequest) req).getHeader("X-Request-Id"))
                    .isEqualTo(req.getAttribute("requestId"));
            new ObjectMapper().writeValue(res.getWriter(), error);
        });
        assertThat(new ObjectMapper().readTree(response.getContentAsString())
                .path("meta").path("requestId").asText()).isEqualTo(response.getHeader("X-Request-Id"));
        assertThat(RequestContextHolder.getRequestAttributes()).isNull();
    }

    @Test
    void rejectsUnsafeIdentifierAndCleansContextOnFailure() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", PATH);
        request.addHeader("X-Request-Id", "unsafe\r\nvalue");
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertThatThrownBy(() -> new EducationAchievementRequestFilter().doFilter(request, response, (req, res) -> {
            throw new IllegalStateException("downstream failure");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(response.getHeader("X-Request-Id")).matches("[A-Za-z0-9._:-]{1,100}");
        assertThat(RequestContextHolder.getRequestAttributes()).isNull();
    }

    @RestController
    public static class EducationEnvelopeProbe {
        @PostMapping({PATH, "/api/probe"})
        ApiResponse<String> probe(
                @Valid @RequestBody ProbeRequest request,
                @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
            if (request.requiredValue().equals("conflict")) {
                throw new ConflictException("PERIOD_NOT_ACTIVE: 입력기간이 아닙니다.");
            }
            if (request.requiredValue().equals("business-validation")) {
                throw new BusinessValidationException("입력 오류", List.of(
                        new ValidationError("managementItemCode", "유효한 항목을 선택하세요.")));
            }
            if (request.requiredValue().equals("unexpected")) {
                throw new IllegalStateException("internal connection failure");
            }
            return ApiResponse.ok(requestId);
        }
    }

    record ProbeRequest(@NotBlank(message = "필수값입니다.") String requiredValue) {
    }
}
