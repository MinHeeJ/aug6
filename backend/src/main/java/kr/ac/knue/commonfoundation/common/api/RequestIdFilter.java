package kr.ac.knue.commonfoundation.common.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Establishes one safe request identifier before authentication and keeps it available in HTTP
 * headers, request attributes, and API response metadata.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {
    public static final String HEADER_NAME = "X-Request-Id";
    public static final String REQUEST_ATTRIBUTE = "requestId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String requestId = RequestIdResolver.resolve(request.getHeader(HEADER_NAME));
        RequestIdContext.set(requestId);
        response.setHeader(HEADER_NAME, requestId);

        try {
            filterChain.doFilter(new RequestIdRequestWrapper(request, requestId), response);
        } finally {
            RequestIdContext.clear();
        }
    }

    private static final class RequestIdRequestWrapper extends HttpServletRequestWrapper {
        private final String requestId;

        private RequestIdRequestWrapper(HttpServletRequest request, String requestId) {
            super(request);
            this.requestId = requestId;
            setAttribute(REQUEST_ATTRIBUTE, requestId);
        }

        @Override
        public String getHeader(String name) {
            if (HEADER_NAME.equalsIgnoreCase(name)) {
                return requestId;
            }
            return super.getHeader(name);
        }
    }
}
