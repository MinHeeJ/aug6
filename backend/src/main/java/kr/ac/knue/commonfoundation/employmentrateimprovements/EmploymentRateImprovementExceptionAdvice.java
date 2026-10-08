package kr.ac.knue.commonfoundation.employmentrateimprovements;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import kr.ac.knue.commonfoundation.common.api.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Keeps approved object-valued fields local without changing legacy API error envelopes. */
@Order(-100)
@RestControllerAdvice(assignableTypes = EmploymentRateImprovementController.class)
public class EmploymentRateImprovementExceptionAdvice {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handle(Exception exception, HttpServletRequest request) {
        int status = 500;
        String code = "INTERNAL_ERROR";
        String message = "처리 중 오류가 발생했습니다. 관리자에게 문의하세요.";
        Map<String, String> fields = new LinkedHashMap<>();
        if (exception instanceof MethodArgumentNotValidException validation) {
            status = 400;
            code = "VALIDATION_ERROR";
            message = "입력값을 확인하세요.";
            validation.getBindingResult().getFieldErrors().forEach(
                    error -> fields.put(error.getField(), error.getDefaultMessage()));
        } else if (exception instanceof BusinessValidationException validation) {
            status = 400;
            code = "VALIDATION_ERROR";
            message = validation.getMessage();
            validation.fields().forEach(error -> fields.put(error.field(), error.message()));
        } else if (exception instanceof UnauthenticatedException) {
            status = 401;
            code = "UNAUTHENTICATED";
            message = "인증이 필요합니다.";
        } else if (exception instanceof ForbiddenException) {
            status = 403;
            code = "FORBIDDEN";
            message = "접근 권한이 없습니다.";
        } else if (exception instanceof NotFoundException) {
            status = 404;
            code = "NOT_FOUND";
            message = "실적을 찾을 수 없습니다.";
        } else if (exception instanceof ConflictException) {
            status = 409;
            message = exception.getMessage();
            code = "CONFLICT";
            for (String candidate : new String[]{"CONFIRMED_DATA_LOCKED", "PERIOD_NOT_ACTIVE",
                    "INVALID_STATE_TRANSITION"}) {
                if (message.startsWith(candidate + ":")) code = candidate;
            }
        } else if (exception instanceof org.springframework.dao.DataIntegrityViolationException) {
            status = 409;
            code = "CONFLICT";
            message = "중복되거나 변경할 수 없는 실적입니다.";
        } else if (exception instanceof org.springframework.http.converter.HttpMessageNotReadableException
                || exception instanceof org.springframework.web.method.annotation.MethodArgumentTypeMismatchException) {
            status = 400;
            code = "VALIDATION_ERROR";
            message = "입력 형식이 올바르지 않습니다.";
        }
        return ResponseEntity.status(status).body(Map.of(
                "success", false, "error", Map.of("code", code, "message", message, "fields", fields),
                "meta", Map.of("requestId", EmploymentRateImprovementController.trace(request))));
    }
}
