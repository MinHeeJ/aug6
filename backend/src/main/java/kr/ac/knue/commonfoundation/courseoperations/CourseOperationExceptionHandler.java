package kr.ac.knue.commonfoundation.courseoperations;

import jakarta.servlet.http.HttpServletRequest;
import kr.ac.knue.commonfoundation.common.api.ApiError;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.GlobalExceptionHandler;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Adds request correlation locally without changing existing shared error codes or exposing internal details. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = CourseOperationController.class)
public class CourseOperationExceptionHandler {
    private final GlobalExceptionHandler shared;

    public CourseOperationExceptionHandler(GlobalExceptionHandler shared) {
        this.shared = shared;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handle(Exception error, HttpServletRequest request) {
        ResponseEntity<ApiResponse<Void>> response;
        if (error instanceof MethodArgumentNotValidException validation) {
            response = shared.handleValidation(validation);
        } else if (error instanceof BusinessValidationException validation) {
            response = shared.handleBusinessValidation(validation);
        } else if (error instanceof UnauthenticatedException authentication) {
            response = shared.handleUnauthenticated(authentication);
        } else if (error instanceof ForbiddenException forbidden) {
            response = shared.handleForbidden(forbidden);
        } else if (error instanceof NotFoundException missing) {
            response = shared.handleNotFound(missing);
        } else if (error instanceof ConflictException conflict) {
            response = shared.handleConflict(conflict);
        } else if (error instanceof HttpMessageNotReadableException || error instanceof IllegalArgumentException) {
            response = ResponseEntity.badRequest().body(
                    ApiResponse.fail(ApiError.of("BAD_REQUEST", "입력 형식을 확인하세요.")));
        } else {
            response = shared.handleUnexpectedError(error);
        }
        ApiResponse<Void> body = response.getBody();
        body.meta().put("requestId", CourseOperationController.id(request));
        return ResponseEntity.status(response.getStatusCode()).body(body);
    }
}
