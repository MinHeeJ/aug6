package kr.ac.knue.commonfoundation.signup;

import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
public class SignupController {
    private final SignupService signupService;

    public SignupController(SignupService signupService) {
        this.signupService = signupService;
    }

    @GetMapping("/api/auth/signup/check-login-id")
    public ApiResponse<SignupAvailabilityResponse> checkLoginId(@RequestParam String loginId) {
        return ApiResponse.ok(signupService.checkLoginId(loginId));
    }

    @GetMapping("/api/auth/signup/check-email")
    public ApiResponse<SignupAvailabilityResponse> checkEmail(@RequestParam String email) {
        return ApiResponse.ok(signupService.checkEmail(email));
    }

    @PostMapping("/api/auth/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SignupResponse> createSignup(@RequestBody SignupRequest request) {
        return ApiResponse.ok(signupService.createSignup(request));
    }

    @PostMapping("/api/auth/resend-verification")
    public ApiResponse<ResendVerificationResponse> resendVerification(@RequestBody ResendVerificationRequest request) {
        return ApiResponse.ok(signupService.resendVerificationEmail(request));
    }

    @GetMapping("/api/auth/verify-email")
    public RedirectView verifyEmail(@RequestParam(required = false) String token) {
        EmailVerificationResult result = signupService.verifyEmail(token);
        RedirectView redirectView = new RedirectView("/email-verification/result?status=" + result.status(), true);
        redirectView.setExposeModelAttributes(false);
        return redirectView;
    }
}
