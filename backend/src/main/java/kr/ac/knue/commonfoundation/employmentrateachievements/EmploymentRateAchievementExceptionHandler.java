package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.util.List;
import kr.ac.knue.commonfoundation.common.api.ApiError;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

/** Translates malformed feature input to safe 400 errors without changing other controllers' advice. */
@RestControllerAdvice(assignableTypes = EmploymentRateAchievementController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EmploymentRateAchievementExceptionHandler {
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> malformedBody(HttpMessageNotReadableException exception) {
        String field = "body";
        if (exception.getCause() instanceof com.fasterxml.jackson.databind.JsonMappingException mapping
                && !mapping.getPath().isEmpty()) {
            String candidate = mapping.getPath().get(0).getFieldName();
            if (candidate != null) field = candidate;
        }
        return ResponseEntity.badRequest().body(ApiResponse.fail(ApiError.validation(
                List.of(new ValidationError(field, "날짜와 JSON 형식을 확인하세요.")))));
    }
}
