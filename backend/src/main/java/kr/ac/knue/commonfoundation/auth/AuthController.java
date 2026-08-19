package kr.ac.knue.commonfoundation.auth;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    public static final String SESSION_COOKIE = "COMMON_FOUNDATION_SESSION";
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/api/auth/login")
    public ApiResponse<CurrentUser> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthenticatedSession session = authService.login(request);
        ResponseCookie cookie = ResponseCookie.from(SESSION_COOKIE, session.sessionId())
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(8 * 60 * 60)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ApiResponse.ok(session.user());
    }

    @GetMapping("/api/auth/me")
    public ApiResponse<CurrentUser> me(@CookieValue(name = SESSION_COOKIE, required = false) String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new UnauthenticatedException();
        }
        return ApiResponse.ok(authService.currentUser(sessionId));
    }

    @PostMapping("/api/auth/logout")
    public ApiResponse<Void> logout(@CookieValue(name = SESSION_COOKIE, required = false) String sessionId, HttpServletResponse response) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new UnauthenticatedException();
        }
        authService.logout(sessionId);
        ResponseCookie cookie = ResponseCookie.from(SESSION_COOKIE, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ApiResponse.empty();
    }
}
