package kr.ac.knue.commonfoundation.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.permissions.EffectivePermissionService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthenticationFilterRequestIdTest {
    @Test
    void preservesValidRequestIdentifierAcrossResponseMetadataAndLoggingContext() throws Exception {
        AuthenticationFilter filter = new AuthenticationFilter(
                Mockito.mock(AuthService.class),
                Mockito.mock(EffectivePermissionService.class),
                new ObjectMapper()
        );
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("X-Request-Id", "basic79-request-123");

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            assertThat(servletRequest.getAttribute("requestId")).isEqualTo("basic79-request-123");
            assertThat(MDC.get("requestId")).isEqualTo("basic79-request-123");
            new ObjectMapper().writeValue(
                    servletResponse.getWriter(),
                    ApiResponse.ok("UP")
            );
        });

        assertThat(response.getHeader("X-Request-Id")).isEqualTo("basic79-request-123");
        assertThat(response.getContentAsString()).contains("\"requestId\":\"basic79-request-123\"");
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void replacesMalformedRequestIdentifierBeforeItCanReachLogsOrResponse() throws Exception {
        AuthenticationFilter filter = new AuthenticationFilter(
                Mockito.mock(AuthService.class),
                Mockito.mock(EffectivePermissionService.class),
                new ObjectMapper()
        );
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("X-Request-Id", "unsafe\r\nvalue");

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            assertThat(servletRequest.getAttribute("requestId"))
                    .matches(value -> value instanceof String requestId
                            && requestId.matches("[A-Za-z0-9-]{1,100}"));
        });

        assertThat(response.getHeader("X-Request-Id"))
                .matches(requestId -> requestId.matches("[A-Za-z0-9-]{1,100}"));
    }
}
