package kr.ac.knue.commonfoundation.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.common.api.CodedResponseException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.permissions.EffectivePermissionService;
import kr.ac.knue.commonfoundation.signup.PasswordHashService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class LocalAccountAuthenticationAdapter implements AuthenticationPort {
    private final AuthMapper authMapper;
    private final EffectivePermissionService permissionService;
    private final PasswordHashService passwordHashService;

    public LocalAccountAuthenticationAdapter(AuthMapper authMapper, EffectivePermissionService permissionService,
                                             PasswordHashService passwordHashService) {
        this.authMapper = authMapper;
        this.permissionService = permissionService;
        this.passwordHashService = passwordHashService;
    }

    @Override
    public AuthenticatedSession authenticate(LoginRequest request) {
        AuthMapper.AccountRow account = authMapper.findAccountByLoginId(request.loginId());
        if (account == null || !matches(request.password(), account.passwordHash())) {
            throw new UnauthenticatedException();
        }
        if ("PENDING_EMAIL".equals(account.accountStatus()) || "N".equals(account.emailVerifiedYn())) {
            throw new CodedResponseException(HttpStatus.FORBIDDEN, "EMAIL_VERIFICATION_REQUIRED",
                    "이메일 인증을 완료해주세요. 인증 메일 재발송 후 다시 시도할 수 있습니다.");
        }
        if ("SUSPENDED".equals(account.accountStatus())) {
            throw new CodedResponseException(HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED", "사용할 수 없는 계정입니다.");
        }
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        authMapper.insertSession(sessionId, account.userId(), LocalDateTime.now().plusHours(8));
        List<String> roles = authMapper.findActiveRoleCodes(account.userId());
        CurrentUser user = new CurrentUser(account.userId(), account.loginId(), account.employeeNo(), account.name(), roles,
                permissionService.visibleMenus(account.userId(), roles));
        return new AuthenticatedSession(sessionId, user);
    }

    private boolean matches(String rawPassword, String storedHash) {
        if (storedHash != null && storedHash.startsWith("$argon2")) {
            return passwordHashService.matches(rawPassword, storedHash);
        }
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
