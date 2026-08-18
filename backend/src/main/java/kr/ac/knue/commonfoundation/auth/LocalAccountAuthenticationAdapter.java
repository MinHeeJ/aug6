package kr.ac.knue.commonfoundation.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.permissions.EffectivePermissionService;
import org.springframework.stereotype.Service;

@Service
public class LocalAccountAuthenticationAdapter implements AuthenticationPort {
    private final AuthMapper authMapper;
    private final EffectivePermissionService permissionService;

    public LocalAccountAuthenticationAdapter(AuthMapper authMapper, EffectivePermissionService permissionService) {
        this.authMapper = authMapper;
        this.permissionService = permissionService;
    }

    @Override
    public AuthenticatedSession authenticate(LoginRequest request) {
        AuthMapper.AccountRow account = authMapper.findAccountByLoginId(request.loginId());
        if (account == null || !matches(request.password(), account.passwordHash())) {
            throw new UnauthenticatedException();
        }
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        authMapper.insertSession(sessionId, account.userId(), LocalDateTime.now().plusHours(8));
        List<String> roles = authMapper.findActiveRoleCodes(account.userId());
        CurrentUser user = new CurrentUser(account.userId(), account.loginId(), account.employeeNo(), account.name(), roles,
                permissionService.visibleMenus(account.userId(), roles));
        return new AuthenticatedSession(sessionId, user);
    }

    private boolean matches(String rawPassword, String storedHash) {
        return ("sha256:" + sha256(rawPassword)).equals(storedHash);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest unavailable", exception);
        }
    }
}
