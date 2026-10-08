package kr.ac.knue.commonfoundation.lectureimprovements;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import kr.ac.knue.commonfoundation.common.api.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Keeps the new object-shaped error fields local, preserving legacy global error contracts. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = LectureImprovementController.class)
public class LectureImprovementExceptionAdvice {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> error(Exception error, HttpServletRequest request) {
        int status = 500;
        String code = "INTERNAL_ERROR";
        String message = "오류가 발생했습니다. 관리자에게 문의하세요.";
        Map<String, String> fields = new LinkedHashMap<>();
        if (error instanceof MethodArgumentNotValidException validation) {
            status = 400;
            code = "VALIDATION_ERROR";
            message = "필수 입력값을 확인하세요.";
            validation.getBindingResult().getFieldErrors().forEach(e -> fields.put(e.getField(), e.getDefaultMessage()));
        } else if (error instanceof BusinessValidationException validation) {
            status = 400;
            code = "VALIDATION_ERROR";
            message = validation.getMessage();
            validation.fields().forEach(e -> fields.put(e.field(), e.message()));
        } else if (error instanceof UnauthenticatedException) {
            status = 401;
            code = "UNAUTHENTICATED";
            message = "인증이 필요합니다.";
        } else if (error instanceof ForbiddenException) {
            status = 403;
            code = "FORBIDDEN";
            message = "접근 권한이 없습니다.";
        } else if (error instanceof NotFoundException) {
            status = 404;
            code = "NOT_FOUND";
            message = "강의개선 실적을 찾을 수 없습니다.";
        } else if (error instanceof ConflictException) {
            status = 409;
            String prefix = error.getMessage().split(":", 2)[0];
            code = java.util.List.of("CONFIRMED_DATA_LOCKED", "PERIOD_NOT_ACTIVE", "INVALID_STATE_TRANSITION")
                    .contains(prefix) ? prefix : "CONFLICT";
            message = "현재 기간 또는 상태에서는 저장할 수 없습니다.";
        } else if (error instanceof DataIntegrityViolationException) {
            status = 409;
            code = "CONFLICT";
            message = "이미 등록된 실적이거나 입력 조건이 변경되었습니다.";
        } else if (error instanceof HttpMessageNotReadableException
                || error instanceof MethodArgumentTypeMismatchException || error instanceof IllegalArgumentException) {
            status = 400;
            code = "VALIDATION_ERROR";
            message = "요청 형식을 확인하세요.";
        }
        return ResponseEntity.status(status).body(Map.of(
                "success", false, "error", Map.of("code", code, "message", message, "fields", fields),
                "meta", Map.of("requestId", LectureImprovementController.trace(request))));
    }
}
