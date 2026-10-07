package kr.ac.knue.commonfoundation.employmentrateachievements;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.common.api.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Keeps feature error codes compatible while retaining correlation IDs and suppressing internal details. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {EmploymentRateAchievementController.class, EmploymentRateExcelController.class})
public class EmploymentRateExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handle(Exception exception, HttpServletRequest request) {
        int status = 500;
        String code = "INTERNAL_ERROR";
        String message = "오류가 발생했습니다. 관리자에게 문의하세요.";
        List<ValidationError> fields = List.of();
        if (exception instanceof MethodArgumentNotValidException validation) {
            status = 400;
            code = "VALIDATION_ERROR";
            message = "입력값을 확인하세요.";
            fields = validation.getBindingResult().getFieldErrors().stream()
                    .map(e -> new ValidationError(e.getField(), e.getDefaultMessage())).toList();
        } else if (exception instanceof HttpMessageNotReadableException) {
            status = 400;
            code = "VALIDATION_ERROR";
            message = "요청 형식과 업적발생일 YYYY-MM-DD를 확인하세요.";
            fields = List.of(new ValidationError("achievementDate", message));
        } else if (exception instanceof BusinessValidationException validation) {
            status = 400;
            code = "VALIDATION_ERROR";
            message = validation.getMessage();
            fields = validation.fields();
        } else if (exception instanceof UnauthenticatedException) {
            status = 401; code = "UNAUTHENTICATED"; message = "인증이 필요합니다.";
        } else if (exception instanceof ForbiddenException) {
            status = 403; code = "FORBIDDEN"; message = "접근 권한이 없습니다.";
        } else if (exception instanceof NotFoundException) {
            status = 404; code = "NOT_FOUND"; message = exception.getMessage();
        } else if (exception instanceof ConflictException || exception instanceof org.springframework.dao.DuplicateKeyException) {
            status = 409; code = "CONFLICT";
            message = exception instanceof ConflictException ? exception.getMessage() : "DUPLICATE: 중복 실적입니다.";
        } else if (exception instanceof IllegalArgumentException
                || exception instanceof org.springframework.web.multipart.support.MissingServletRequestPartException) {
            status = 400; code = "BAD_REQUEST"; message = "입력값과 파일을 확인하세요.";
        }
        String id = request.getAttribute("requestId") instanceof String trace ? trace : request.getHeader("X-Request-Id");
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        var meta = ApiResponse.ok(null, id).meta();
        return ResponseEntity.status(status).body(new ApiResponse<>(false, null, new ApiError(code, message, fields), meta));
    }
}
