package kr.ac.knue.commonfoundation.lectureimprovements;

import jakarta.servlet.http.HttpServletRequest;
import kr.ac.knue.commonfoundation.common.api.ApiError;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Handles only typed lecture conflicts; existing global error behavior remains intact. */
@Order(-10)
@RestControllerAdvice(assignableTypes = LectureImprovementController.class)
public class LectureImprovementExceptionHandler {
    @ExceptionHandler(LectureImprovementConflictException.class)
    public ResponseEntity<ApiResponse<Void>> conflict(
            LectureImprovementConflictException exception, HttpServletRequest request) {
        ApiResponse<Void> result = ApiResponse.fail(ApiError.of(exception.code(), exception.getMessage()));
        result.meta().put("requestId", LectureImprovementController.requestId(request));
        return ResponseEntity.status(409).body(result);
    }
}
