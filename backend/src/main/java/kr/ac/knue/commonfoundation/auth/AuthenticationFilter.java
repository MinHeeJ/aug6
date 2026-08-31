package kr.ac.knue.commonfoundation.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import kr.ac.knue.commonfoundation.common.api.ApiError;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.permissions.EffectivePermissionService;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

public class AuthenticationFilter extends OncePerRequestFilter {
    private final AuthService authService;
    private final EffectivePermissionService permissionService;
    private final ObjectMapper objectMapper;

    public AuthenticationFilter(AuthService authService, EffectivePermissionService permissionService, ObjectMapper objectMapper) {
        this.authService = authService;
        this.permissionService = permissionService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/") || path.equals("/api/health") || path.equals("/api/auth/login")) {
            filterChain.doFilter(request, response);
            return;
        }
        String sessionId = extractSession(request);
        if (sessionId == null) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, ApiError.of("UNAUTHENTICATED", "인증이 필요합니다."));
            return;
        }
        try {
            CurrentUser user = authService.currentUser(sessionId);
            request.setAttribute("currentUser", user);
            if (requiresMenuPermission(path) && !permissionService.canAccess(user.userId(), user.roles(), pathToUiRoute(path))) {
                writeError(response, HttpServletResponse.SC_FORBIDDEN, ApiError.of("FORBIDDEN", "접근 권한이 없습니다."));
                return;
            }
            filterChain.doFilter(request, response);
        } catch (RuntimeException exception) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, ApiError.of("UNAUTHENTICATED", "인증이 필요합니다."));
        }
    }

    private boolean requiresMenuPermission(String path) {
        return path.startsWith("/api/admin/") || path.startsWith("/api/business/");
    }

    private String pathToUiRoute(String apiPath) {
        if (apiPath.equals("/api/business/evaluation-organization-mappings")) {
            return "/admin/evaluation-organization-mappings";
        }
        if (apiPath.equals("/api/admin/business-status-codes")) {
            return "/admin/business-status-codes";
        }
        if (apiPath.equals("/api/admin/business-status-transitions")) {
            return "/admin/business-status-transitions";
        }
        if (apiPath.equals("/api/admin/rejection-reasons")) {
            return "/admin/rejection-reasons";
        }
        if (apiPath.equals("/api/admin/data-change-histories")) {
            return "/admin/data-change-histories";
        }
        if (apiPath.equals("/api/admin/deleted-business-data")) {
            return "/admin/deleted-business-data";
        }
        if (apiPath.equals("/api/admin/users") || apiPath.matches("/api/admin/users/[^/]+/(account|roles)")) {
            return "/admin/users";
        }
        if (apiPath.equals("/api/admin/menus/exposure") || apiPath.equals("/api/admin/menus/exposure-save")) {
            return "/admin/menu-usage";
        }
        if (apiPath.equals("/api/admin/menus/tree") || apiPath.equals("/api/admin/menus/reorder") || apiPath.matches("/api/admin/menus/[^/]+/parent")) {
            return "/admin/menu-structure";
        }
        if (apiPath.equals("/api/admin/organizations/tree")) {
            return "/admin/organizations";
        }
        if (apiPath.matches("/api/admin/menus/[^/]+/execution")) {
            return "/admin/menu-info";
        }
        if (apiPath.matches("/api/admin/code-groups/[^/]+")) {
            return "/admin/code-groups";
        }
        if (apiPath.matches("/api/admin/code-groups/[^/]+/codes/[^/]+/usage") || apiPath.matches("/api/admin/code-groups/[^/]+/codes-usage")) {
            return "/admin/detail-code-usage";
        }
        if (apiPath.matches("/api/admin/code-groups/[^/]+/codes(/[^/]+)?")) {
            return "/admin/detail-codes";
        }
        if (apiPath.matches("/api/admin/organizations/[^/]+/parent-relations(/history)?")) {
            return "/admin/organizations";
        }
        if (apiPath.matches("/api/admin/roles/[^/]+")) {
            return "/admin/roles";
        }
        if (apiPath.matches("/api/admin/user-roles/[^/]+")) {
            return "/admin/user-roles";
        }
        if (apiPath.matches("/api/admin/system-settings/base-years/[^/]+/standards-preparation")) {
            return "/admin/base-years";
        }
        if (apiPath.equals("/api/admin/batch-definitions")) {
            return "/admin/batch-definitions";
        }
        if (apiPath.equals("/api/admin/batch-executions") || apiPath.matches("/api/admin/batch-executions/[^/]+/(status|rerun)")) {
            return "/admin/batch-executions";
        }
        if (apiPath.equals("/api/admin/batch-results") || apiPath.matches("/api/admin/batch-results/[^/]+/log")) {
            return "/admin/batch-results";
        }
        if (apiPath.equals("/api/admin/batch-retries") || apiPath.equals("/api/admin/batch-retries/targets")) {
            return "/admin/batch-retries";
        }
        return switch (apiPath) {
            case "/api/admin/organizations" -> "/admin/organizations";
            case "/api/admin/roles" -> "/admin/roles";
            case "/api/admin/user-roles" -> "/admin/user-roles";
            case "/api/admin/menu-permissions" -> "/admin/menu-permissions";
            case "/api/admin/menu-structure" -> "/admin/menu-structure";
            case "/api/admin/menu-info" -> "/admin/menu-info";
            case "/api/admin/code-groups" -> "/admin/code-groups";
            case "/api/admin/detail-codes" -> "/admin/detail-codes";
            case "/api/admin/system-settings/common", "/api/admin/system-settings/common-values" -> "/admin/common-settings";
            case "/api/admin/system-settings/base-years", "/api/admin/system-settings/base-year-current" -> "/admin/base-years";
            default -> apiPath;
        };
    }

    private String extractSession(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> AuthController.SESSION_COOKIE.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private void writeError(HttpServletResponse response, int status, ApiError error) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.fail(error));
    }
}
