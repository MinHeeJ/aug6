package kr.ac.knue.commonfoundation.common.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/** Projects new education errors without changing the legacy ApiError Java or wire contract. */
@RestControllerAdvice
public class EducationAchievementResponseAdvice implements ResponseBodyAdvice<Object> {
    private static final Pattern BUSINESS_CODE = Pattern.compile("^([A-Z][A-Z0-9_]{1,79}):\\s*(.*)$", Pattern.DOTALL);

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    /** Field keys are object properties on these routes; status codes remain owned by exception advice. */
    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType contentType,
            Class<? extends HttpMessageConverter<?>> converterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {
        if (!EducationAchievementRoutes.supports(request.getURI().getPath())
                || !(body instanceof ApiResponse<?> envelope)
                || envelope.success()
                || envelope.error() == null) {
            return body;
        }
        ApiError error = envelope.error();
        String code = error.code();
        String message = error.message();
        // Existing domain guards communicate typed conflicts as CODE: message.
        // Do not extract internal/validation messages as business error codes.
        if ("CONFLICT".equals(code) && message != null) {
            Matcher matcher = BUSINESS_CODE.matcher(message);
            if (matcher.matches()) {
                code = matcher.group(1);
                message = matcher.group(2);
            }
        }
        Map<String, String> fields = new LinkedHashMap<>();
        if (error.fields() != null) {
            for (ValidationError field : error.fields()) {
                fields.putIfAbsent(field.field(), field.message());
            }
        }
        return new EducationErrorEnvelope(
                false,
                envelope.data(),
                new EducationError(code, message, fields),
                envelope.meta());
    }

    public record EducationError(String code, String message, Map<String, String> fields) {
    }

    public record EducationErrorEnvelope(
            boolean success,
            Object data,
            EducationError error,
            Map<String, Object> meta) {
    }
}
