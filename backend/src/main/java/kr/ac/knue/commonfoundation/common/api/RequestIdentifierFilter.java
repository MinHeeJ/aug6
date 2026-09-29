package kr.ac.knue.commonfoundation.common.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Assigns one request identifier to each API request so callers and server-side diagnostics can
 * correlate a response without accepting an unbounded or malformed client supplied value.
 */
public class RequestIdentifierFilter extends OncePerRequestFilter {
    public static final String HEADER_NAME = "X-Request-Id";
    public static final String ATTRIBUTE_NAME = "requestId";
    private static final int MAX_REQUEST_ID_LENGTH = 128;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String requestId = normalizedRequestId(request.getHeader(HEADER_NAME));
        request.setAttribute(ATTRIBUTE_NAME, requestId);
        response.setHeader(HEADER_NAME, requestId);
        filterChain.doFilter(request, response);
    }

    /** Returns the identifier assigned by the filter for inclusion in the API response envelope. */
    public static String requestId(HttpServletRequest request) {
        Object requestId = request.getAttribute(ATTRIBUTE_NAME);
        return requestId instanceof String value ? value : null;
    }

    private String normalizedRequestId(String suppliedRequestId) {
        if (suppliedRequestId != null && suppliedRequestId.matches("[A-Za-z0-9._-]{1," + MAX_REQUEST_ID_LENGTH + "}")) {
            return suppliedRequestId;
        }
        return UUID.randomUUID().toString();
    }
}
