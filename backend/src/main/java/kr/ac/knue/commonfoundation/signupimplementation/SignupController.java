package kr.ac.knue.commonfoundation.signupimplementation;

import java.util.List;
import kr.ac.knue.commonfoundation.common.api.ApiError;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public signup transport with existing envelopes and no authentication/session side effects. */
@RestController
public class SignupController {
    private final SignupService service;

    public SignupController(SignupService service) {
        this.service = service;
    }

    /** Anonymous availability query; this does not reserve an identifier or create a session. */
    @GetMapping("/api/v1/auth/check-userid")
    public ApiResponse<UserIdAvailabilityResponse> checkUserIdAvailability(
            @RequestParam(name = "userId", required = false) String userId
    ) {
        return ApiResponse.ok(service.checkUserIdAvailability(userId));
    }

    /** Create the ACTIVE account and R01 assignment atomically, returning 201 without Set-Cookie. */
    @PostMapping("/api/v1/auth/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@RequestBody SignupRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.ok(service.signup(request)));
    }

    /** Local handling preserves field-aware conflicts without changing other modules' error contracts. */
    @ExceptionHandler(SignupFailure.class)
    public ResponseEntity<ApiResponse<Void>> failure(SignupFailure exception) {
        String code = switch (exception.status()) {
            case 400 -> "VALIDATION_ERROR";
            case 409 -> "CONFLICT";
            default -> "INTERNAL_ERROR";
        };
        return ResponseEntity.status(exception.status()).body(ApiResponse.fail(
                new ApiError(code, exception.getMessage(), exception.fields())));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> malformedRequest() {
        return ResponseEntity.badRequest().body(ApiResponse.fail(new ApiError(
                "VALIDATION_ERROR",
                "입력값을 확인해 주세요.",
                List.of(new ValidationError("userId", "회원가입 입력값이 올바르지 않습니다."))
        )));
    }
}
