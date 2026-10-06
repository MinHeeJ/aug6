package kr.ac.knue.commonfoundation.signupimplementation;

import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ordered server validation and one atomic users/R01 transaction for anonymous account creation. */
@Service
public class SignupService {
    private static final Pattern USER_ID = Pattern.compile("^[a-z][a-z0-9]{3,19}$");
    private static final Pattern EMAIL = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final String PASSWORD_RULE =
            "비밀번호는 8자 이상, 영문·숫자·특수문자 중 3종 이상을 포함해야 합니다.";
    private final SignupMapper mapper;
    private final SignupPasswordEncoder encoder;

    public SignupService(SignupMapper mapper, SignupPasswordEncoder encoder) {
        this.mapper = mapper;
        this.encoder = encoder;
    }

    /** Read-only occupancy lookup includes inactive/deleted accounts and does not establish a session. */
    @Transactional(readOnly = true)
    public UserIdAvailabilityResponse checkUserIdAvailability(String userId) {
        validateUserId(userId);
        return new UserIdAvailabilityResponse(!mapper.loginIdExists(userId));
    }

    /** Prechecks provide ordered errors; database uniqueness remains authoritative for concurrent requests. */
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        require(request.userId(), "userId");
        require(request.password(), "password");
        require(request.passwordConfirm(), "passwordConfirm");
        require(request.email(), "email");
        validateUserId(request.userId());
        if (mapper.loginIdExists(request.userId())) {
            throw duplicate("userId");
        }
        if (request.email().length() > 254 || !EMAIL.matcher(request.email()).matches()) {
            throw invalid("email", "올바른 이메일 형식이 아닙니다.");
        }
        String email = request.email().toLowerCase(Locale.ROOT);
        if (mapper.emailExists(email)) {
            throw duplicate("email");
        }
        if (!request.password().equals(request.passwordConfirm())) {
            throw invalid("passwordConfirm", "비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        }
        if (request.password().equals(request.userId()) || !validPassword(request.password())) {
            throw invalid("password", PASSWORD_RULE);
        }
        String hash = encoder.encode(request.password());
        try {
            Long key = mapper.insertAccount(request.userId(), hash, email);
            if (key == null || mapper.insertDefaultRole(key) != 1) {
                throw new SignupFailure(500, null, "가입 처리에 실패했습니다. 잠시 후 다시 시도해 주세요.");
            }
        } catch (DataIntegrityViolationException exception) {
            // Match known constraint identities only, never return/log the underlying SQL or bound values.
            String detail = exception.getMostSpecificCause().getMessage();
            if (detail != null && detail.contains("users_login_id_key")) {
                throw duplicate("userId");
            }
            if (detail != null && detail.contains("uq_users_signup_email")) {
                throw duplicate("email");
            }
            throw new SignupFailure(500, null, "가입 처리에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        } catch (DataAccessException exception) {
            throw new SignupFailure(500, null, "가입 처리에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }
        return new SignupResponse(request.userId(), "가입이 완료되었습니다.");
    }

    private void validateUserId(String userId) {
        require(userId, "userId");
        if (!USER_ID.matcher(userId).matches()) {
            throw invalid("userId", "아이디는 영문 소문자로 시작하는 영문 소문자·숫자 4~20자여야 합니다.");
        }
    }

    private void require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw invalid(field, field + " 필수값을 입력해 주세요.");
        }
    }

    private boolean validPassword(String value) {
        boolean upper = false;
        boolean lower = false;
        boolean digit = false;
        boolean punctuation = false;
        for (char c : value.toCharArray()) {
            upper |= c >= 'A' && c <= 'Z';
            lower |= c >= 'a' && c <= 'z';
            digit |= c >= '0' && c <= '9';
            punctuation |= c >= '!' && c <= '/' || c >= ':' && c <= '@'
                    || c >= '[' && c <= '`' || c >= '{' && c <= '~';
        }
        int categories = (upper ? 1 : 0) + (lower ? 1 : 0) + (digit ? 1 : 0) + (punctuation ? 1 : 0);
        return value.length() >= 8 && categories >= 3;
    }

    private SignupFailure invalid(String field, String message) {
        return new SignupFailure(400, field, message);
    }

    private SignupFailure duplicate(String field) {
        return new SignupFailure(409, field, "userId".equals(field)
                ? "이미 사용 중인 아이디입니다." : "이미 등록된 이메일입니다.");
    }
}
