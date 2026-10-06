package kr.ac.knue.commonfoundation.employmentrateachievements;

import com.fasterxml.jackson.databind.JsonMappingException;
import java.util.List;
import kr.ac.knue.commonfoundation.common.api.ApiError;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Limits malformed-JSON/date error translation to this feature without exposing Jackson/SQL internals. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = EmploymentRateAchievementController.class)
public class EmploymentRateInputErrorHandler {
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> malformed(HttpMessageNotReadableException exception) {
        String field = "body";
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof BusinessValidationException validation) {
                return ResponseEntity.badRequest().body(ApiResponse.fail(ApiError.validation(validation.fields())));
            }
            if (cause instanceof JsonMappingException mapping && !mapping.getPath().isEmpty()) {
                String name = mapping.getPath().get(0).getFieldName();
                if (name != null) field = name;
            }
        }
        return ResponseEntity.badRequest().body(ApiResponse.fail(ApiError.validation(
                List.of(new ValidationError(field, "입력 형식 또는 수정 불가 항목을 확인하세요.")))));
    }
}
