package kr.ac.knue.commonfoundation.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.RequestIdFilter;
import kr.ac.knue.commonfoundation.common.api.RequestIdResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/** Verifies the shared request identifier boundary used by BASIC-83 API and security fixtures. */
class RequestIdFilterTest {
    @AfterEach
    void clearRequestIdContext() {
        kr.ac.knue.commonfoundation.common.api.RequestIdContext.clear();
    }

    @Test
    void preservesSafeClientRequestIdAcrossHeaderControllerAndEnvelope() throws Exception {
        MockMvc mockMvc = mockMvc();

        mockMvc.perform(get("/api/request-id-probe")
                        .header(RequestIdFilter.HEADER_NAME, "B83-REQUEST-001"))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestIdFilter.HEADER_NAME, "B83-REQUEST-001"))
                .andExpect(jsonPath("$.data.requestId").value("B83-REQUEST-001"))
                .andExpect(jsonPath("$.meta.requestId").value("B83-REQUEST-001"));
    }

    @Test
    void replacesUnsafeClientRequestIdInsteadOfReflectingIt() throws Exception {
        MockMvc mockMvc = mockMvc();

        mockMvc.perform(get("/api/request-id-probe")
                        .header(RequestIdFilter.HEADER_NAME, "invalid request id"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        RequestIdFilter.HEADER_NAME,
                        org.hamcrest.Matchers.not("invalid request id")))
                .andExpect(jsonPath("$.data.requestId").isNotEmpty())
                .andExpect(jsonPath("$.meta.requestId").isNotEmpty());
    }

    @Test
    void trimsSafeRequestIdsAndGeneratesIdsForMissingOrUnsafeValues() {
        assertThat(RequestIdResolver.resolve(" B83-REQUEST-002 ")).isEqualTo("B83-REQUEST-002");
        assertThat(RequestIdResolver.resolve(null)).matches("[0-9a-f-]{36}");
        assertThat(RequestIdResolver.resolve("invalid request id")).matches("[0-9a-f-]{36}");
    }

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(new RequestIdProbeController())
                .addFilters(new RequestIdFilter())
                .build();
    }

    @RestController
    static class RequestIdProbeController {
        @GetMapping("/api/request-id-probe")
        ApiResponse<RequestIdProbeResponse> probe(
                @RequestHeader(RequestIdFilter.HEADER_NAME) String requestId) {
            return ApiResponse.ok(new RequestIdProbeResponse(requestId));
        }
    }

    record RequestIdProbeResponse(String requestId) {
    }
}
