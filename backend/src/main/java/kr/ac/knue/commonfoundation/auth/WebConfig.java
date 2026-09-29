package kr.ac.knue.commonfoundation.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.ac.knue.commonfoundation.common.api.RequestIdentifierFilter;
import kr.ac.knue.commonfoundation.permissions.EffectivePermissionService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfig {
    /** Registers request correlation before authentication so success and error responses share one identifier. */
    @Bean
    FilterRegistrationBean<RequestIdentifierFilter> requestIdentifierFilter() {
        FilterRegistrationBean<RequestIdentifierFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RequestIdentifierFilter());
        registration.addUrlPatterns("/api/*");
        registration.setOrder(0);
        return registration;
    }

    @Bean
    FilterRegistrationBean<AuthenticationFilter> authenticationFilter(AuthService authService, EffectivePermissionService permissionService, ObjectMapper objectMapper) {
        FilterRegistrationBean<AuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new AuthenticationFilter(authService, permissionService, objectMapper));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1);
        return registration;
    }
}
