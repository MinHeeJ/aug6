package kr.ac.knue.commonfoundation.common.api;

import java.util.List;

public record ApiError(String code, String message, List<ValidationError> fields) {
    public static ApiError of(String code, String message) {
        return new ApiError(code, message, List.of());
    }

    public static ApiError validation(List<ValidationError> fields) {
        return new ApiError("VALIDATION_ERROR", "입력값을 확인해 주세요.", fields);
    }
}
