package kr.ac.knue.commonfoundation.signup;

import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Anonymous registration endpoint; login/session creation is deliberately outside this boundary. */
@RestController
public class SignupController {
    private final SignupService service;

    public SignupController(SignupService service) {
        this.service = service;
    }

    /** Checks a public login identifier without authentication or session creation. */
    @GetMapping("/api/v1/auth/check-userid")
    public ApiResponse<UserIdAvailabilityResponse> checkUserIdAvailability(
            @RequestParam(name = "userId", required = false) String userId
    ) {
        // Missing values reach ordered service validation so the error retains fields.userId.
        return ApiResponse.ok(service.checkUserIdAvailability(userId));
    }

    /** Creates an account and its default role without issuing a session cookie. */
    @PostMapping("/api/v1/auth/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SignupResponse> signup(@RequestBody SignupRequest request) {
        return ApiResponse.ok(service.signup(request));
    }
}
