package kr.ac.knue.commonfoundation.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class ApiEnvelopeTest {
    MockMvc mockMvc;

    @BeforeEach
    void setUpStandaloneProbe() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ValidationProbeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void successfulResponsesExposeSuccessAndMeta() {
        ApiResponse<String> response = ApiResponse.ok("ok");
        org.assertj.core.api.Assertions.assertThat(response.success()).isTrue();
        org.assertj.core.api.Assertions.assertThat(response.meta()).containsKeys("timestamp", "traceId");
    }

    @Test
    void validationErrorsExposeApiErrorFieldsAndMeta() throws Exception {
        mockMvc.perform(post("/api/probe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.meta.timestamp").exists())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("requiredValue"));
    }

    @Test
    void namedConflictPrefixBecomesStableErrorCodeWithoutChangingGenericConflicts() throws Exception {
        mockMvc.perform(post("/api/conflict-probe/named"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFIRMED_DATA_LOCKED"));
        mockMvc.perform(post("/api/conflict-probe/generic"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @RestController
    public static class ValidationProbeController {
        @PostMapping("/api/probe")
        ApiResponse<String> probe(@Valid @RequestBody ProbeRequest request) {
            return ApiResponse.ok(request.requiredValue());
        }

        @PostMapping("/api/conflict-probe/named")
        ApiResponse<Void> namedConflict() {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 데이터는 수정할 수 없습니다.");
        }

        @PostMapping("/api/conflict-probe/generic")
        ApiResponse<Void> genericConflict() {
            throw new ConflictException("동시 수정 충돌입니다.");
        }
    }

    record ProbeRequest(@NotBlank(message = "필수값입니다.") String requiredValue) {}
}
