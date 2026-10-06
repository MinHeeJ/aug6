package kr.ac.knue.commonfoundation.signup;

import java.util.List;
import kr.ac.knue.commonfoundation.common.api.ApiError;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Signup-only errors retain the shared envelope while preventing credential/SQL diagnostics leakage. */
@Order(-1)
@RestControllerAdvice(assignableTypes = SignupController.class)
public class SignupExceptionHandler {
    @ExceptionHandler(SignupConflictException.class)
    public ResponseEntity<ApiResponse<Void>> conflict(SignupConflictException exception) {
        return ResponseEntity.status(409).body(ApiResponse.fail(new ApiError(
                "CONFLICT",
                exception.getMessage(),
                List.of(new ValidationError(exception.field(), exception.getMessage()))
        )));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> malformedBody() {
        return ResponseEntity.badRequest().body(ApiResponse.fail(
                ApiError.of("VALIDATION_ERROR", "올바른 회원가입 요청을 입력해 주세요.")
        ));
    }

    @ExceptionHandler(SignupPersistenceException.class)
    public ResponseEntity<ApiResponse<Void>> persistenceFailure() {
        return ResponseEntity.internalServerError().body(ApiResponse.fail(
                ApiError.of("INTERNAL_SERVER_ERROR", "회원가입 처리 중 오류가 발생했습니다.")
        ));
    }
}
