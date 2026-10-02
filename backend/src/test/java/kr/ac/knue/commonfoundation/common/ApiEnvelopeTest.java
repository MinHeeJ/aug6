package kr.ac.knue.commonfoundation.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.common.api.RequestIdFilter;
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
                .addFilters(new RequestIdFilter())
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
                        .header(RequestIdFilter.HEADER_NAME, "B83-VALIDATION-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(RequestIdFilter.HEADER_NAME, "B83-VALIDATION-001"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.meta.timestamp").exists())
                .andExpect(jsonPath("$.meta.requestId").value("B83-VALIDATION-001"))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fields[0].field").value("requiredValue"));
    }

    @RestController
    public static class ValidationProbeController {
        @PostMapping("/api/probe")
        ApiResponse<String> probe(@Valid @RequestBody ProbeRequest request) {
            return ApiResponse.ok(request.requiredValue());
        }
    }

    record ProbeRequest(@NotBlank(message = "필수값입니다.") String requiredValue) {}
}
