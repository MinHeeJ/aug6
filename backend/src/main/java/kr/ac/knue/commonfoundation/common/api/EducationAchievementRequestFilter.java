package kr.ac.knue.commonfoundation.common.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.filter.OncePerRequestFilter;

/** Establishes one bounded request identifier before authentication and business processing. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class EducationAchievementRequestFilter extends OncePerRequestFilter {
    public static final String REQUEST_ID_ATTRIBUTE = "requestId";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !EducationAchievementRoutes.supports(request.getRequestURI());
    }

    /** Existing controllers reading the header also receive the generated identifier. */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        HttpServletRequest wrapped = new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                return REQUEST_ID_HEADER.equalsIgnoreCase(name) ? requestId : super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                return REQUEST_ID_HEADER.equalsIgnoreCase(name)
                        ? Collections.enumeration(Collections.singletonList(requestId))
                        : super.getHeaders(name);
            }

            @Override
            public Enumeration<String> getHeaderNames() {
                LinkedHashSet<String> names = new LinkedHashSet<>(Collections.list(super.getHeaderNames()));
                names.removeIf(REQUEST_ID_HEADER::equalsIgnoreCase);
                names.add(REQUEST_ID_HEADER);
                return Collections.enumeration(names);
            }
        };
        RequestAttributes previous = RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes attributes = new ServletRequestAttributes(wrapped, response);
        RequestContextHolder.setRequestAttributes(attributes);
        try {
            chain.doFilter(wrapped, response);
        } finally {
            attributes.requestCompleted();
            RequestContextHolder.setRequestAttributes(previous);
        }
    }

    /** Available to success/error envelopes, including filter-level authentication errors. */
    public static String currentRequestId() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                && EducationAchievementRoutes.supports(attributes.getRequest().getRequestURI())) {
            return resolveRequestId(attributes.getRequest());
        }
        return null;
    }

    private static String resolveRequestId(HttpServletRequest request) {
        Object existing = request.getAttribute(REQUEST_ID_ATTRIBUTE);
        if (existing instanceof String id && isSafe(id)) {
            return id;
        }
        String id = request.getHeader(REQUEST_ID_HEADER);
        if (id == null || !isSafe(id.trim())) {
            id = UUID.randomUUID().toString();
        } else {
            id = id.trim();
        }
        request.setAttribute(REQUEST_ID_ATTRIBUTE, id);
        return id;
    }

    private static boolean isSafe(String id) {
        return id.matches("[A-Za-z0-9._:-]{1,100}");
    }
}
