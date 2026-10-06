package kr.ac.knue.commonfoundation.auth;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns anonymous registration validation and atomically creates an active user with the R01 role.
 */
@Service
public class SignupService {
    private static final Pattern USER_ID_PATTERN = Pattern.compile("^[a-z][a-z0-9]{3,19}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private final SignupMapper mapper;
    private final Argon2PasswordEncoder passwordEncoder;

    @Autowired
    public SignupService(SignupMapper mapper) {
        this(mapper, Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8());
    }

    SignupService(SignupMapper mapper, Argon2PasswordEncoder passwordEncoder) {
        this.mapper = mapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Checks a normalized identifier without exposing account details to an anonymous caller.
     */
    @Transactional(readOnly = true)
    public UserIdAvailabilityResponse checkUserIdAvailability(String userId) {
        String normalizedUserId = normalizeUserId(userId);
        if (normalizedUserId.isEmpty()) {
            return new UserIdAvailabilityResponse(false);
        }
        validateUserId(normalizedUserId);
        return new UserIdAvailabilityResponse(mapper.countByLoginId(normalizedUserId) == 0);
    }

    /**
     * Validates and persists the user plus the mandatory R01 assignment in one transaction.
     */
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        validateRequired(request);
        String userId = normalizeUserId(request.userId());
        String email = normalizeEmail(request.email());
        validateUserId(userId);
        if (mapper.countByLoginId(userId) > 0) {
            throw new ConflictException("이미 사용 중인 아이디입니다.");
        }
        validateEmail(email);
        if (mapper.countByEmail(email) > 0) {
            throw new ConflictException("이미 등록된 이메일입니다.");
        }
        validatePassword(request, userId);

        try {
            Long userIdKey = mapper.insertUser(userId, passwordEncoder.encode(request.password()), email);
            if (userIdKey == null) {
                throw new IllegalStateException("회원가입 사용자 식별자를 생성하지 못했습니다.");
            }
            if (mapper.insertDefaultRole(userIdKey) != 1) {
                throw new IllegalStateException("기본 역할을 부여하지 못했습니다.");
            }
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("이미 사용 중인 아이디 또는 이메일입니다.");
        }
        return new SignupResponse(userId, "가입이 완료되었습니다.");
    }

    private void validateRequired(SignupRequest request) {
        if (request == null) {
            throw validation("request", "회원가입 요청은 필수입니다.");
        }
        java.util.ArrayList<ValidationError> fields = new java.util.ArrayList<>();
        if (request.userId() == null || request.userId().isBlank()) {
            fields.add(new ValidationError("userId", "아이디는 필수입니다."));
        }
        if (request.password() == null || request.password().isBlank()) {
            fields.add(new ValidationError("password", "비밀번호는 필수입니다."));
        }
        if (request.passwordConfirm() == null || request.passwordConfirm().isBlank()) {
            fields.add(new ValidationError("passwordConfirm", "비밀번호 확인은 필수입니다."));
        }
        if (request.email() == null || request.email().isBlank()) {
            fields.add(new ValidationError("email", "이메일은 필수입니다."));
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("회원가입 요청이 올바르지 않습니다.", fields);
        }
    }

    private void validateUserId(String userId) {
        if (!USER_ID_PATTERN.matcher(userId).matches()) {
            throw validation("userId", "아이디는 영문 소문자로 시작하는 4~20자의 영문 소문자와 숫자만 사용할 수 있습니다.");
        }
    }

    private void validateEmail(String email) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw validation("email", "올바른 이메일 형식이 아닙니다.");
        }
    }

    private void validatePassword(SignupRequest request, String userId) {
        if (!request.password().equals(request.passwordConfirm())) {
            throw validation("passwordConfirm", "비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        }
        if (request.password().length() < 8
                || request.password().equals(userId)
                || passwordCategoryCount(request.password()) < 3) {
            throw validation("password", "비밀번호는 8자 이상, 영문·숫자·특수문자 중 3종 이상을 포함해야 합니다.");
        }
    }

    private int passwordCategoryCount(String password) {
        int categories = 0;
        if (password.chars().anyMatch(Character::isUpperCase)) {
            categories++;
        }
        if (password.chars().anyMatch(Character::isLowerCase)) {
            categories++;
        }
        if (password.chars().anyMatch(Character::isDigit)) {
            categories++;
        }
        if (password.chars().anyMatch(value -> !Character.isLetterOrDigit(value))) {
            categories++;
        }
        return categories;
    }

    private BusinessValidationException validation(String field, String message) {
        return new BusinessValidationException(message, List.of(new ValidationError(field, message)));
    }

    private String normalizeUserId(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeEmail(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
