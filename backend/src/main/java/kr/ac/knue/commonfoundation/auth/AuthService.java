package kr.ac.knue.commonfoundation.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.time.LocalDateTime;
import java.util.regex.Pattern;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.TooManyRequestsException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import kr.ac.knue.commonfoundation.emailverification.EmailAddressPolicy;
import kr.ac.knue.commonfoundation.emailverification.EmailVerificationEventRepository;
import kr.ac.knue.commonfoundation.emailverification.EmailVerificationToken;
import kr.ac.knue.commonfoundation.emailverification.EmailVerificationTokenRepository;
import kr.ac.knue.commonfoundation.emailverification.MailDeliveryAttempt;
import kr.ac.knue.commonfoundation.emailverification.MailDeliveryAttemptRepository;
import kr.ac.knue.commonfoundation.emailverification.VerificationMailDeliveryException;
import kr.ac.knue.commonfoundation.emailverification.VerificationMailSender;
import kr.ac.knue.commonfoundation.permissions.EffectivePermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final Pattern PASSWORD_POLICY = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,255}$");

    private final AuthenticationPort authenticationPort;
    private final AuthMapper authMapper;
    private final EffectivePermissionService permissionService;
    private final SignupMapper signupMapper;
    private final EmailVerificationTokenRepository tokenRepository;
    private final MailDeliveryAttemptRepository mailDeliveryAttemptRepository;
    private final VerificationMailSender verificationMailSender;
    private final EmailVerificationEventRepository emailVerificationEventRepository;
    private final EmailVerificationResendProperties resendProperties;

    public AuthService(AuthenticationPort authenticationPort, AuthMapper authMapper, EffectivePermissionService permissionService) {
        this(authenticationPort, authMapper, permissionService, null, null, null, null, null, (EmailVerificationResendProperties) null);
    }

    @Autowired
    public AuthService(
            AuthenticationPort authenticationPort,
            AuthMapper authMapper,
            EffectivePermissionService permissionService,
            SignupMapper signupMapper,
            EmailVerificationTokenRepository tokenRepository,
            MailDeliveryAttemptRepository mailDeliveryAttemptRepository,
            VerificationMailSender verificationMailSender,
            EmailVerificationEventRepository emailVerificationEventRepository,
            EmailVerificationResendSettings resendSettings
    ) {
        this(
                authenticationPort,
                authMapper,
                permissionService,
                signupMapper,
                tokenRepository,
                mailDeliveryAttemptRepository,
                verificationMailSender,
                emailVerificationEventRepository,
                resendSettings == null ? null : resendSettings.toProperties()
        );
    }

    public AuthService(
            AuthenticationPort authenticationPort,
            AuthMapper authMapper,
            EffectivePermissionService permissionService,
            SignupMapper signupMapper,
            EmailVerificationTokenRepository tokenRepository,
            MailDeliveryAttemptRepository mailDeliveryAttemptRepository,
            VerificationMailSender verificationMailSender,
            EmailVerificationEventRepository emailVerificationEventRepository
    ) {
        this(authenticationPort, authMapper, permissionService, signupMapper, tokenRepository,
                mailDeliveryAttemptRepository, verificationMailSender, emailVerificationEventRepository, (EmailVerificationResendProperties) null);
    }

    AuthService(
            AuthenticationPort authenticationPort,
            AuthMapper authMapper,
            EffectivePermissionService permissionService,
            SignupMapper signupMapper,
            EmailVerificationTokenRepository tokenRepository,
            MailDeliveryAttemptRepository mailDeliveryAttemptRepository,
            VerificationMailSender verificationMailSender,
            EmailVerificationEventRepository emailVerificationEventRepository,
            EmailVerificationResendProperties resendProperties
    ) {
        this.authenticationPort = authenticationPort;
        this.authMapper = authMapper;
        this.permissionService = permissionService;
        this.signupMapper = signupMapper;
        this.tokenRepository = tokenRepository;
        this.mailDeliveryAttemptRepository = mailDeliveryAttemptRepository;
        this.verificationMailSender = verificationMailSender;
        this.emailVerificationEventRepository = emailVerificationEventRepository;
        this.resendProperties = resendProperties == null ? new EmailVerificationResendProperties(60, 5, 20) : resendProperties;
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

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        requireSignupDependencies();
        NormalizedSignup normalized = validateSignup(request);
        String passwordHash = "sha256:" + sha256(normalized.password());
        signupMapper.insertPendingSignupUser(new SignupMapper.NewSignupUser(
                normalized.loginId(),
                passwordHash,
                normalized.email()
        ));
        Long userId = signupMapper.findUserIdByLoginId(normalized.loginId());
        signupMapper.assignDefaultGeneralUserRole(userId);
        EmailVerificationTokenRepository.IssuedEmailVerificationToken token = tokenRepository.issueReplacementToken(userId, normalized.email());
        Long attemptId = mailDeliveryAttemptRepository.recordRequested(userId, normalized.email());
        try {
            verificationMailSender.sendVerificationMail(normalized.email(), token.rawToken());
            mailDeliveryAttemptRepository.markSent(attemptId);
        } catch (VerificationMailDeliveryException exception) {
            mailDeliveryAttemptRepository.markFailure(
                    attemptId,
                    exception.deliveryStatus(),
                    exception.diagnosticCategory(),
                    redactDiagnostic(exception.getMessage()),
                    exception.retryEligible()
            );
        } catch (RuntimeException exception) {
            mailDeliveryAttemptRepository.markFailure(
                    attemptId,
                    "FAILED_OTHER",
                    "UNKNOWN",
                    redactDiagnostic(exception.getMessage()),
                    true
            );
        }
        MailDeliveryAttempt attempt = mailDeliveryAttemptRepository.findById(attemptId);
        String deliveryStatus = attempt == null ? "REQUESTED" : attempt.deliveryStatus();
        return new SignupResponse(userId, normalized.loginId(), normalized.email(), "PENDING_EMAIL", "N", deliveryStatus);
    }

    @Transactional
    public EmailVerificationResendResponse createEmailVerificationResend(EmailVerificationResendRequest request, String requesterIp) {
        requireSignupDependencies();
        String email = trimToEmpty(request.email()).toLowerCase(Locale.ROOT);
        if (email.isBlank() || !EmailAddressPolicy.isValidEmailFormat(email)) {
            throw new BusinessValidationException("재발송 입력값을 확인해 주세요.",
                    List.of(new ValidationError("email", "올바른 이메일 주소를 입력하세요.")));
        }
        SignupMapper.ResendTargetUser target = signupMapper.findResendTargetByEmail(email);
        if (!isEligibleForVerificationResend(target, email)) {
            return EmailVerificationResendResponse.accepted();
        }
        if (mailDeliveryAttemptRepository.hasRecentAttemptInsideThrottleWindow(
                target.userId(),
                email,
                resendProperties.sameAccountEmailThrottleSeconds()
        )) {
            throw new TooManyRequestsException("인증 메일은 60초 후 다시 요청할 수 있습니다.");
        }
        if (mailDeliveryAttemptRepository.countAttemptsSince(email, null, 3600) >= resendProperties.maxAttemptsPerEmailPerHour()) {
            throw new TooManyRequestsException("이메일 기준 재발송 요청 한도를 초과했습니다. 잠시 후 다시 시도하세요.");
        }
        String normalizedIp = trimToNull(requesterIp);
        if (normalizedIp != null
                && mailDeliveryAttemptRepository.countAttemptsSince(null, normalizedIp, 3600) >= resendProperties.maxAttemptsPerIpPerHour()) {
            throw new TooManyRequestsException("요청 기준 재발송 한도를 초과했습니다. 잠시 후 다시 시도하세요.");
        }

        EmailVerificationTokenRepository.IssuedEmailVerificationToken token = tokenRepository.issueReplacementToken(target.userId(), email);
        Long attemptId = mailDeliveryAttemptRepository.recordRequested(target.userId(), email, normalizedIp);
        emailVerificationEventRepository.recordResendRequested(target.userId(), email);
        try {
            verificationMailSender.sendVerificationMail(email, token.rawToken());
            mailDeliveryAttemptRepository.markSent(attemptId);
        } catch (VerificationMailDeliveryException exception) {
            mailDeliveryAttemptRepository.markFailure(
                    attemptId,
                    exception.deliveryStatus(),
                    exception.diagnosticCategory(),
                    redactDiagnostic(exception.getMessage()),
                    exception.retryEligible()
            );
        } catch (RuntimeException exception) {
            mailDeliveryAttemptRepository.markFailure(
                    attemptId,
                    "FAILED_OTHER",
                    "UNKNOWN",
                    redactDiagnostic(exception.getMessage()),
                    true
            );
        }
        return EmailVerificationResendResponse.accepted();
    }

    private boolean isEligibleForVerificationResend(SignupMapper.ResendTargetUser target, String email) {
        if (target == null || EmailAddressPolicy.isSubstituteEmail(email)) {
            return false;
        }
        return "PENDING_EMAIL".equals(target.accountStatus())
                && "N".equals(target.emailVerifiedYn())
                && "N".equals(target.emailSubstituteYn())
                && "ACTIVE".equals(target.status());
    }

    @Transactional
    public EmailVerificationResponse updateEmailVerification(EmailVerificationRequest request) {
        requireVerificationDependencies();
        String rawToken = trimToEmpty(request.token());
        if (!rawToken.matches("^[A-Fa-f0-9]{64}$")) {
            throw new BusinessValidationException("인증 토큰 형식이 올바르지 않습니다.",
                    List.of(new ValidationError("token", "인증 토큰 형식이 올바르지 않습니다.")));
        }

        EmailVerificationToken token = tokenRepository.findByRawToken(rawToken);
        if (token == null) {
            throw new BusinessValidationException("유효하지 않은 인증 링크입니다.",
                    List.of(new ValidationError("token", "인증 링크를 다시 확인하거나 재발송을 요청하세요.")));
        }
        AuthMapper.VerificationUserRow user = authMapper.findVerificationUserById(token.userId());
        if (user == null) {
            recordRejected(token, "대상 계정을 찾을 수 없습니다.");
            throw new BusinessValidationException("유효하지 않은 인증 링크입니다.",
                    List.of(new ValidationError("token", "인증 링크를 다시 확인하거나 재발송을 요청하세요.")));
        }
        if (!"ACTIVE".equals(token.status())) {
            recordRejected(token, rejectedTokenMessage(token.status()));
            throw new BusinessValidationException(rejectedTokenMessage(token.status()),
                    List.of(new ValidationError("token", rejectedTokenMessage(token.status()))));
        }
        LocalDateTime now = LocalDateTime.now();
        if (!token.expiresAt().isAfter(now)) {
            recordRejected(token, "만료된 인증 링크입니다. 인증 메일 재발송을 요청하세요.");
            throw new BusinessValidationException("만료된 인증 링크입니다. 인증 메일 재발송을 요청하세요.",
                    List.of(new ValidationError("token", "만료된 인증 링크입니다.")));
        }
        String userEmail = EmailAddressPolicy.normalizeEmail(user.email());
        if (!token.targetEmail().equals(userEmail)) {
            recordRejected(token, "인증 링크의 대상 이메일이 계정 이메일과 일치하지 않습니다.");
            throw new BusinessValidationException("유효하지 않은 인증 링크입니다.",
                    List.of(new ValidationError("token", "인증 링크를 다시 확인하거나 재발송을 요청하세요.")));
        }
        if (!"ACTIVE".equals(user.status()) || !"PENDING_EMAIL".equals(user.accountStatus())) {
            recordRejected(token, "현재 계정 상태에서는 이메일 인증으로 활성화할 수 없습니다.");
            throw new ConflictException("현재 계정 상태에서는 이메일 인증으로 활성화할 수 없습니다.");
        }
        int used = tokenRepository.markUsedIfActive(token.tokenId());
        int activated = authMapper.activatePendingEmailUser(user.userId());
        if (used != 1 || activated != 1) {
            recordRejected(token, "인증 상태가 변경되어 링크를 사용할 수 없습니다.");
            throw new ConflictException("인증 상태가 변경되어 링크를 사용할 수 없습니다.");
        }
        emailVerificationEventRepository.recordLinkVerified(user.userId(), token.tokenId(), userEmail);
        return new EmailVerificationResponse(user.userId(), user.loginId(), userEmail, "ACTIVE", "Y");
    }

    private void recordRejected(EmailVerificationToken token, String reason) {
        if (emailVerificationEventRepository != null && token != null) {
            emailVerificationEventRepository.recordTokenRejected(token.userId(), token.tokenId(), token.targetEmail(), reason);
        }
    }

    private String rejectedTokenMessage(String status) {
        if ("USED".equals(status)) {
            return "이미 사용된 인증 링크입니다. 로그인하거나 인증 메일 재발송을 요청하세요.";
        }
        if ("SUPERSEDED".equals(status)) {
            return "새 인증 메일이 발급되어 이전 링크는 사용할 수 없습니다.";
        }
        if ("EXPIRED".equals(status)) {
            return "만료된 인증 링크입니다. 인증 메일 재발송을 요청하세요.";
        }
        return "유효하지 않은 인증 링크입니다.";
    }

    private void requireVerificationDependencies() {
        if (tokenRepository == null || emailVerificationEventRepository == null) {
            throw new IllegalStateException("Email verification dependencies are not configured");
        }
    }

    private NormalizedSignup validateSignup(SignupRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        String loginId = trimToEmpty(request.loginId());
        String password = trimToEmpty(request.password());
        String email = trimToEmpty(request.email()).toLowerCase(Locale.ROOT);

        if (loginId.isBlank()) {
            fields.add(new ValidationError("loginId", "로그인 ID를 입력하세요."));
        } else if (loginId.length() > 100) {
            fields.add(new ValidationError("loginId", "로그인 ID는 100자 이하로 입력하세요."));
        }
        if (!PASSWORD_POLICY.matcher(password).matches()) {
            fields.add(new ValidationError("password", "비밀번호는 8자 이상이며 영문과 숫자를 포함해야 합니다."));
        }
        if (email.isBlank() || !EmailAddressPolicy.isValidEmailFormat(email)) {
            fields.add(new ValidationError("email", "올바른 이메일 주소를 입력하세요."));
        } else if (EmailAddressPolicy.isSubstituteEmail(email)) {
            fields.add(new ValidationError("email", "admin@kndadmin.com은 신규 회원가입에 사용할 수 없습니다."));
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("회원가입 입력값을 확인해 주세요.", fields);
        }
        if (signupMapper.countByLoginId(loginId) > 0) {
            throw new BusinessValidationException("회원가입 입력값을 확인해 주세요.",
                    List.of(new ValidationError("loginId", "이미 사용 중인 로그인 ID입니다.")));
        }
        if (signupMapper.countNormalUsersByEmail(email) > 0) {
            throw new ConflictException("이미 사용 중인 이메일입니다.");
        }
        return new NormalizedSignup(loginId, password, email);
    }

    private void requireSignupDependencies() {
        if (signupMapper == null || tokenRepository == null || mailDeliveryAttemptRepository == null || verificationMailSender == null) {
            throw new IllegalStateException("Signup dependencies are not configured");
        }
    }

    private String redactDiagnostic(String value) {
        if (value == null || value.isBlank()) {
            return "인증 메일 발송에 실패했습니다.";
        }
        return value.replaceAll("(?i)(password|secret|token|authorization)=?[^\\s,;]*", "$1=REDACTED");
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimToNull(String value) {
        String trimmed = trimToEmpty(value);
        return trimmed.isBlank() ? null : trimmed;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest unavailable", exception);
        }
    }

    private record NormalizedSignup(String loginId, String password, String email) {
    }
}
