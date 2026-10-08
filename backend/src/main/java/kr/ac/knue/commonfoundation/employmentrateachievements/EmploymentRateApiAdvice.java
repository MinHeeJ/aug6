package kr.ac.knue.commonfoundation.employmentrateachievements;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import kr.ac.knue.commonfoundation.common.api.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/** New-feature object-field errors without changing the legacy list-field API envelope. */
@Order(-10)
@RestControllerAdvice(assignableTypes = EmploymentRateAchievementController.class)
public class EmploymentRateApiAdvice {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> error(Exception exception, HttpServletRequest request) {
        int status = 500;
        String code = "INTERNAL_ERROR";
        String message = "오류가 발생했습니다. 잠시 후 다시 시도하세요.";
        Map<String, String> fields = new LinkedHashMap<>();
        if (exception instanceof MethodArgumentNotValidException invalid) {
            status = 400;
            code = "VALIDATION_ERROR";
            message = "입력값을 확인하세요.";
            invalid.getBindingResult().getFieldErrors().forEach(e -> fields.put(e.getField(), e.getDefaultMessage()));
        } else if (exception instanceof BusinessValidationException invalid) {
            status = 400;
            code = "VALIDATION_ERROR";
            message = invalid.getMessage();
            invalid.fields().forEach(e -> fields.put(e.field(), e.message()));
        } else if (exception instanceof HttpMessageNotReadableException) {
            status = 400;
            code = "VALIDATION_ERROR";
            message = "JSON 또는 날짜 형식을 확인하세요.";
            fields.put("achievementDate", "YYYY-MM-DD 형식으로 입력하세요.");
        } else if (exception instanceof IllegalArgumentException
                || exception instanceof MissingServletRequestPartException) {
            status = 400;
            code = "BAD_REQUEST";
            message = "입력값 또는 업로드 파일을 확인하세요.";
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
            message = "요청한 자료를 찾을 수 없습니다.";
        } else if (exception instanceof ConflictException) {
            status = 409;
            message = exception.getMessage();
            code = message.contains(":") ? message.substring(0, message.indexOf(':')) : "CONFLICT";
        } else if (exception instanceof org.springframework.dao.DataIntegrityViolationException) {
            status = 409;
            code = "DUPLICATE_DATA";
            message = "동일 실적 또는 데이터 충돌이 발생했습니다.";
        }
        return ResponseEntity.status(status).body(Map.of("success", false,
                "error", Map.of("code", code, "message", message, "fields", fields),
                "meta", Map.of("requestId", EmploymentRateAchievementController.trace(request))));
    }
}
