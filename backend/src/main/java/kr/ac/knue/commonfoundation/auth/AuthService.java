package kr.ac.knue.commonfoundation.auth;

import java.util.List;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.permissions.EffectivePermissionService;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AuthenticationPort authenticationPort;
    private final AuthMapper authMapper;
    private final EffectivePermissionService permissionService;

    public AuthService(AuthenticationPort authenticationPort, AuthMapper authMapper, EffectivePermissionService permissionService) {
        this.authenticationPort = authenticationPort;
        this.authMapper = authMapper;
        this.permissionService = permissionService;
    }

    public AuthenticatedSession login(LoginRequest request) {
        return authenticationPort.authenticate(request);
    }

    public CurrentUser currentUser(String sessionId) {
        AuthMapper.SessionUserRow row = authMapper.findUserByActiveSession(sessionId);
        if (row == null) {
            throw new UnauthenticatedException();
        }
        authMapper.touchSession(sessionId);
        List<String> roles = authMapper.findActiveRoleCodes(row.userId());
        return new CurrentUser(row.userId(), row.loginId(), row.employeeNo(), row.name(), roles, permissionService.visibleMenus(row.userId(), roles));
    }

    public void logout(String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            authMapper.logout(sessionId);
        }
    }
}
