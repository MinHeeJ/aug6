package kr.ac.knue.commonfoundation.health;

import java.util.Map;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Provides the unauthenticated deployment health probe while retaining its request identifier
 * in the standard API envelope for operational traceability.
 */
@RestController
public class HealthController {
    @GetMapping("/api/health")
    public ApiResponse<Map<String, String>> health(
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        return ApiResponse.ok(Map.of("status", "UP", "service", "common-foundation"), requestId);
    }

    /**
     * Retains the programmatic health representation used by existing application verification
     * code; HTTP callers should use the mapped overload so their request identifier is echoed.
     */
    public ApiResponse<Map<String, String>> health() {
        return health(null);
    }
}
