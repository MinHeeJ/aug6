package kr.ac.knue.commonfoundation.signup;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import kr.ac.knue.commonfoundation.common.api.CodedResponseException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import kr.ac.knue.commonfoundation.common.request.RequestIdFilter;
import kr.ac.knue.commonfoundation.mail.EmailVerificationMailDispatcher;
import kr.ac.knue.commonfoundation.mail.VerificationMailCommand;
import org.slf4j.MDC;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class SignupService {
    private static final String RESEND_ACCEPTED_MESSAGE = "인증 메일 재발송 요청이 접수되었습니다.";
    private static final String ALREADY_VERIFIED_MESSAGE = "이미 인증이 완료된 계정입니다.";

    private final SignupBoundaryPolicy boundaryPolicy;
    private final SignupMapper signupMapper;
    private final PasswordHashService passwordHashService;
    private final SignupTokenService tokenService;
    private final EmailVerificationMailDispatcher mailDispatcher;

    public SignupService(SignupBoundaryPolicy boundaryPolicy, SignupMapper signupMapper, PasswordHashService passwordHashService,
                         SignupTokenService tokenService, EmailVerificationMailDispatcher mailDispatcher) {
        this.boundaryPolicy = boundaryPolicy;
        this.signupMapper = signupMapper;
        this.passwordHashService = passwordHashService;
        this.tokenService = tokenService;
        this.mailDispatcher = mailDispatcher;
    }

    public SignupAvailabilityResponse checkLoginId(String loginId) {
        SignupBoundaryPolicy.ValidationResult format = boundaryPolicy.validateLoginId(loginId);
        if (!format.valid()) {
            throw badRequest(format);
        }
        if (signupMapper.countByLoginId(loginId.trim()) > 0) {
            throw new CodedResponseException(HttpStatus.CONFLICT, "DUPLICATE_USER_ID", "이미 사용 중인 아이디입니다.",
                    List.of(new ValidationError("loginId", "이미 사용 중인 아이디입니다.")));
        }
        return new SignupAvailabilityResponse(true, "사용 가능한 아이디입니다.");
    }

    public SignupAvailabilityResponse checkEmail(String email) {
        SignupBoundaryPolicy.ValidationResult format = boundaryPolicy.validateEmail(email);
        if (!format.valid()) {
            throw badRequest(format);
        }
        String normalized = boundaryPolicy.normalizeEmail(email);
        if (signupMapper.countByEmail(normalized) > 0) {
            throw new CodedResponseException(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "이미 등록된 이메일입니다.",
                    List.of(new ValidationError("email", "이미 등록된 이메일입니다.")));
        }
        return new SignupAvailabilityResponse(true, "사용 가능한 이메일입니다.");
    }

    @Transactional
    public SignupResponse createSignup(SignupRequest request) {
        SignupBoundaryPolicy.ValidationResult validation = boundaryPolicy.validateSignupBoundary(
                request.getLoginId(), request.getPassword(), request.getPasswordConfirm(), request.getEmail());
        if (!validation.valid()) {
            throw badRequest(validation);
        }
        String loginId = request.getLoginId().trim();
        String email = boundaryPolicy.normalizeEmail(request.getEmail());
        if (signupMapper.countByLoginId(loginId) > 0) {
            throw new CodedResponseException(HttpStatus.CONFLICT, "DUPLICATE_USER_ID", "이미 사용 중인 아이디입니다.",
                    List.of(new ValidationError("loginId", "이미 사용 중인 아이디입니다.")));
        }
        if (signupMapper.countByEmail(email) > 0) {
            throw new CodedResponseException(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "이미 등록된 이메일입니다.",
                    List.of(new ValidationError("email", "이미 등록된 이메일입니다.")));
        }
        String rawToken = tokenService.generateRawToken();
        try {
            Long userId = signupMapper.insertPendingUser(loginId, passwordHashService.hash(request.getPassword()), email);
            signupMapper.insertDefaultRole(userId);
            signupMapper.insertEmailVerificationToken(userId, tokenService.sha256Hex(rawToken));
            VerificationMailCommand mailCommand = new VerificationMailCommand(userId, loginId, email, rawToken, currentRequestId());
            dispatchAfterCommit(mailCommand);
        } catch (DuplicateKeyException exception) {
            throw new CodedResponseException(HttpStatus.CONFLICT, "DUPLICATE_SIGNUP", "이미 사용 중인 아이디 또는 이메일입니다.");
        }
        return new SignupResponse(loginId, email, "PENDING_EMAIL", "인증 메일이 발송되었습니다. 이메일을 확인해주세요.", true);
    }

    @Transactional
    public EmailVerificationResult verifyEmail(String rawToken) {
        if (rawToken == null || rawToken.trim().isBlank()) {
            throw invalidToken();
        }
        SignupMapper.EmailVerificationTokenRow token = signupMapper.findEmailVerificationToken(
                tokenService.sha256Hex(rawToken.trim()));
        if (token == null || "Y".equals(token.usedYn()) || token.usedDt() != null) {
            throw invalidToken();
        }
        if (token.expireDt() == null || token.expireDt().isBefore(LocalDateTime.now())) {
            throw expiredToken();
        }
        if (!"PENDING_EMAIL".equals(token.accountStatus()) || !"N".equals(token.emailVerifiedYn())) {
            throw invalidToken();
        }
        int activated = signupMapper.activateVerifiedUser(token.userId());
        int markedUsed = signupMapper.markEmailVerificationTokenUsed(token.tokenId());
        if (activated != 1 || markedUsed != 1) {
            throw invalidToken();
        }
        return new EmailVerificationResult("success", "이메일 인증이 완료되었습니다. 로그인해주세요.");
    }

    @Transactional(noRollbackFor = CodedResponseException.class)
    public ResendVerificationResponse resendVerificationEmail(ResendVerificationRequest request) {
        String email = request == null ? null : request.getEmail();
        SignupBoundaryPolicy.ValidationResult validation = boundaryPolicy.validateEmail(email);
        if (!validation.valid()) {
            throw badRequest(validation);
        }
        String normalizedEmail = boundaryPolicy.normalizeEmail(email);
        String resendRequestId = resendRequestId();
        SignupMapper.SignupUserRow user = signupMapper.findUserByEmail(normalizedEmail);
        if (user == null) {
            signupMapper.insertMailAttemptStatus(null, normalizedEmail, "SKIPPED_UNKNOWN", resendRequestId);
            return new ResendVerificationResponse(RESEND_ACCEPTED_MESSAGE);
        }
        if ("ACTIVE".equals(user.accountStatus()) && "Y".equals(user.emailVerifiedYn())) {
            signupMapper.insertMailAttemptStatus(user.userId(), normalizedEmail, "SKIPPED_ACTIVE", resendRequestId);
            return new ResendVerificationResponse(ALREADY_VERIFIED_MESSAGE);
        }
        if (!"PENDING_EMAIL".equals(user.accountStatus()) || !"N".equals(user.emailVerifiedYn())) {
            signupMapper.insertMailAttemptStatus(user.userId(), normalizedEmail, "SKIPPED_UNKNOWN", resendRequestId);
            return new ResendVerificationResponse(RESEND_ACCEPTED_MESSAGE);
        }
        if (signupMapper.countRecentResendAttempts(normalizedEmail) > 0) {
            signupMapper.insertMailAttemptStatus(user.userId(), normalizedEmail, "RATE_LIMITED", resendRequestId);
            throw new CodedResponseException(HttpStatus.TOO_MANY_REQUESTS, "RESEND_RATE_LIMITED",
                    "인증 메일은 1분 후 다시 요청할 수 있습니다.",
                    List.of(new ValidationError("email", "인증 메일은 1분 후 다시 요청할 수 있습니다.")));
        }
        String rawToken = tokenService.generateRawToken();
        signupMapper.invalidatePendingEmailVerificationTokens(user.userId());
        signupMapper.insertEmailVerificationToken(user.userId(), tokenService.sha256Hex(rawToken));
        dispatchAfterCommit(new VerificationMailCommand(user.userId(), user.loginId(), normalizedEmail, rawToken, resendRequestId));
        return new ResendVerificationResponse(RESEND_ACCEPTED_MESSAGE);
    }

    private CodedResponseException invalidToken() {
        return new CodedResponseException(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", "유효하지 않은 인증 링크입니다.",
                List.of(new ValidationError("token", "유효하지 않은 인증 링크입니다.")));
    }

    private CodedResponseException expiredToken() {
        return new CodedResponseException(HttpStatus.GONE, "TOKEN_EXPIRED", "인증 링크가 만료되었습니다. 인증 메일 재발송을 요청해주세요.",
                List.of(new ValidationError("token", "인증 링크가 만료되었습니다. 인증 메일 재발송을 요청해주세요.")));
    }

    private CodedResponseException badRequest(SignupBoundaryPolicy.ValidationResult validation) {
        return new CodedResponseException(HttpStatus.BAD_REQUEST, validation.errorCode(), validation.message(),
                List.of(new ValidationError(validation.field(), validation.message())));
    }

    private void dispatchAfterCommit(VerificationMailCommand mailCommand) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            mailDispatcher.sendVerificationMail(mailCommand);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                mailDispatcher.sendVerificationMail(mailCommand);
            }
        });
    }

    private String currentRequestId() {
        String requestId = MDC.get(RequestIdFilter.MDC_KEY);
        return requestId == null || requestId.isBlank() ? "signup-request" : requestId;
    }

    private String resendRequestId() {
        return "RESEND:" + currentRequestId().trim().toLowerCase(Locale.ROOT);
    }
}
