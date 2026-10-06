package kr.ac.knue.commonfoundation.signup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Enforces ordered server validation and atomic anonymous account plus R01 creation. */
@Service
public class SignupService {
    private static final Pattern USER_ID = Pattern.compile("^[a-z][a-z0-9]{3,19}$");
    private static final Pattern EMAIL = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private final SignupMapper mapper;
    private final SignupPasswordHasher hasher;

    public SignupService(SignupMapper mapper, SignupPasswordHasher hasher) {
        this.mapper = mapper;
        this.hasher = hasher;
    }

    /** Checks all accounts, including inactive/deleted rows whose login identifiers remain reserved. */
    @Transactional(readOnly = true)
    public UserIdAvailabilityResponse checkUserIdAvailability(String userId) {
        if (userId == null || userId.isBlank()) {
            invalid("userId", "필수값을 입력해 주세요.");
        }
        if (!USER_ID.matcher(userId).matches()) {
            invalid("userId", "아이디는 영문 소문자로 시작하는 영문 소문자와 숫자 4~20자여야 합니다.");
        }
        try {
            return new UserIdAvailabilityResponse(!mapper.existsLoginId(userId));
        } catch (DataAccessException exception) {
            // Keep SQL diagnostics out of the public response and shared exception logs.
            throw new SignupPersistenceException();
        }
    }

    /** Validates before hashing; the generated identity connects both writes in one transaction. */
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        try {
            requireFields(request);
            if (!USER_ID.matcher(request.userId()).matches()) {
                invalid("userId", "아이디는 영문 소문자로 시작하는 영문 소문자와 숫자 4~20자여야 합니다.");
            }
            if (mapper.existsLoginId(request.userId())) {
                throw new SignupConflictException("userId");
            }
            if (request.email().length() > 254 || !EMAIL.matcher(request.email()).matches()) {
                invalid("email", "올바른 이메일 형식이 아닙니다.");
            }
            String email = request.email().toLowerCase(Locale.ROOT);
            if (mapper.existsEmail(email)) {
                throw new SignupConflictException("email");
            }
            if (!request.password().equals(request.passwordConfirm())) {
                invalid("passwordConfirm", "비밀번호와 비밀번호 확인이 일치하지 않습니다.");
            }
            if (!validPassword(request.password()) || request.password().equals(request.userId())) {
                invalid("password", "비밀번호는 8자 이상, 대문자·소문자·숫자·특수문자 중 3종 이상을 포함해야 합니다.");
            }
            Long userId = mapper.insertUser(request.userId(), email, hasher.hash(request.password()));
            // ON CONFLICT avoids an aborted PostgreSQL transaction and preserves field-specific race errors.
            if (userId == null) {
                throw new SignupConflictException(mapper.existsLoginId(request.userId()) ? "userId" : "email");
            }
            if (mapper.insertDefaultRole(userId) != 1) {
                throw new SignupPersistenceException();
            }
            return new SignupResponse(request.userId(), "가입이 완료되었습니다.");
        } catch (DataAccessException exception) {
            // Do not attach SQL exception causes: they may contain the password hash or bind values.
            throw new SignupPersistenceException();
        }
    }

    private static void requireFields(SignupRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        required(fields, "userId", request.userId());
        required(fields, "password", request.password());
        required(fields, "passwordConfirm", request.passwordConfirm());
        required(fields, "email", request.email());
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("필수값을 입력해 주세요.", fields);
        }
    }

    private static void required(List<ValidationError> fields, String field, String value) {
        if (value == null || value.isBlank()) {
            fields.add(new ValidationError(field, "필수값을 입력해 주세요."));
        }
    }

    private static void invalid(String field, String message) {
        throw new BusinessValidationException(message, List.of(new ValidationError(field, message)));
    }

    private static boolean validPassword(String value) {
        int categories = 0;
        categories += Pattern.compile("[A-Z]").matcher(value).find() ? 1 : 0;
        categories += Pattern.compile("[a-z]").matcher(value).find() ? 1 : 0;
        categories += Pattern.compile("[0-9]").matcher(value).find() ? 1 : 0;
        categories += Pattern.compile("[^a-zA-Z0-9\\s]").matcher(value).find() ? 1 : 0;
        return value.length() >= 8 && categories >= 3;
    }
}
