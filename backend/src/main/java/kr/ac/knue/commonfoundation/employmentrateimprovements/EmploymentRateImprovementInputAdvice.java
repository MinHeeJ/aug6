package kr.ac.knue.commonfoundation.employmentrateimprovements;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import kr.ac.knue.commonfoundation.common.api.ApiError;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.RequestIds;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Converts malformed feature inputs to safe 400s without changing legacy controllers' error policy. */
@RestControllerAdvice(assignableTypes = EmploymentRateImprovementController.class)
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class EmploymentRateImprovementInputAdvice {
    /** Parser details may contain untrusted content or internal classes; return only a bounded user-facing message. */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> malformed(Exception exception, HttpServletRequest request) {
        String field = exception instanceof MethodArgumentTypeMismatchException mismatch
                ? mismatch.getName() : "body";
        ApiResponse<Void> response = ApiResponse.fail(ApiError.validation(
                List.of(new ValidationError(field, "입력 형식과 날짜를 확인하세요."))));
        response.meta().put("requestId", RequestIds.resolve(request));
        return ResponseEntity.badRequest().body(response);
    }
}
