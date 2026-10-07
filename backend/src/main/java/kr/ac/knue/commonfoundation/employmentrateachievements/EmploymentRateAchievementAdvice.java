package kr.ac.knue.commonfoundation.employmentrateachievements;

import jakarta.servlet.http.HttpServletRequest;
import kr.ac.knue.commonfoundation.common.api.ApiError;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.RequestIds;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Limits malformed JSON/date/query handling to this controller without changing shared advice. */
@RestControllerAdvice(assignableTypes = EmploymentRateAchievementController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EmploymentRateAchievementAdvice {
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> malformed(HttpServletRequest request) {
        ApiResponse<Void> response = ApiResponse.fail(ApiError.of("VALIDATION_ERROR", "요청 필드 형식과 날짜를 확인하세요."));
        response.meta().put("requestId", RequestIds.resolve(request));
        return ResponseEntity.badRequest().body(response);
    }
}
