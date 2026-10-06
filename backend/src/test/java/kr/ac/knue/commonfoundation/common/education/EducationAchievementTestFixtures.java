package kr.ac.knue.commonfoundation.common.education;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import jakarta.servlet.http.Cookie;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.AuthController;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * Shared test inputs and envelope assertions, not production responses or bypassed authorization.
 * A principal here supplies no persisted role, menu or organization scope; integration tests
 * must insert those separately and use the real session filter before claiming access.
 */
public final class EducationAchievementTestFixtures {
    public static final String REQUEST_ID = "education-contract-request-001";
    public static final String UNSAFE_SEARCH_TEXT = "' OR 1=1 -- <script>alert(1)</script>";
    // Inputs for the final browser acceptance step, not evidence that a browser was exercised here.
    public static final List<Viewport> BROWSER_VIEWPORTS = List.of(
            new Viewport("tablet", 768, 1024),
            new Viewport("desktop", 1440, 900));

    public record Viewport(String name, int width, int height) {
    }

    private EducationAchievementTestFixtures() {
    }

    public static CurrentUser principal(Long userId, String role) {
        return new CurrentUser(
                userId,
                "education-test-" + userId,
                "TEST-" + userId,
                "교육 실적 테스트 사용자",
                List.of(role),
                List.of());
    }

    public static MockHttpServletRequest request(String method, String path, String sessionId) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        request.addHeader("X-Request-Id", REQUEST_ID);
        if (sessionId != null) {
            request.setCookies(new Cookie(AuthController.SESSION_COOKIE, sessionId));
        }
        return request;
    }

    /** Success correlation assertion for actual feature-controller MockMvc tests. */
    public static ResultMatcher correlatedSuccess(String requestId) {
        return result -> {
            jsonPath("$.success").value(true).match(result);
            jsonPath("$.error").doesNotExist().match(result);
            jsonPath("$.meta.requestId").value(requestId).match(result);
        };
    }

    /** Retains field-level validation checks instead of testing only a generic HTTP 400. */
    public static ResultMatcher validationField(String field) {
        return result -> {
            jsonPath("$.success").value(false).match(result);
            jsonPath("$.error.code").value("VALIDATION_ERROR").match(result);
            jsonPath("$.error.fields[?(@.field == '" + field + "')]").isNotEmpty().match(result);
        };
    }
}
