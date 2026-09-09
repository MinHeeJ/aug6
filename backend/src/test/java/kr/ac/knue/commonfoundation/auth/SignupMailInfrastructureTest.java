package kr.ac.knue.commonfoundation.auth;

import static org.assertj.core.api.Assertions.assertThat;

import kr.ac.knue.commonfoundation.common.request.AsyncRequestContextConfig;
import kr.ac.knue.commonfoundation.common.request.RequestIdFilter;
import kr.ac.knue.commonfoundation.mail.EmailVerificationMailDispatcher;
import kr.ac.knue.commonfoundation.mail.MailConfiguration;
import kr.ac.knue.commonfoundation.mail.MailSendAttemptMapper;
import kr.ac.knue.commonfoundation.mail.MailTraceContext;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderValidatorAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.AsyncAnnotationBeanPostProcessor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        classes = {
                MailConfiguration.class,
                AsyncRequestContextConfig.class,
                EmailVerificationMailDispatcher.class,
                RequestIdFilter.class,
                SignupMailInfrastructureTest.AsyncTestConfiguration.class
        },
        properties = {
                "spring.mail.host=mailhog",
                "spring.mail.port=1025",
                "spring.mail.username=",
                "spring.mail.password=",
                "app.mail.verification.from=noreply@example.edu",
                "app.mail.verification.base-url=http://localhost:3000",
                "app.mail.verification.subject=이메일 인증을 완료해주세요"
        })
@ActiveProfiles("test")
@ImportAutoConfiguration({
        MailSenderAutoConfiguration.class,
        MailSenderValidatorAutoConfiguration.class
})
class SignupMailInfrastructureTest {
    @Autowired
    JavaMailSender mailSender;

    @Autowired
    MailProperties mailProperties;

    @Autowired
    Environment environment;

    @Autowired
    EmailVerificationMailDispatcher dispatcher;

    @MockBean
    MailSendAttemptMapper attemptMapper;

    @Autowired
    AsyncAnnotationBeanPostProcessor asyncProcessor;

    @Autowired
    RequestIdFilter requestIdFilter;

    @Test
    void testProfileUsesMailHogSmtpThroughJavaMailSender() {
        assertThat(mailSender).isNotNull();
        assertThat(mailProperties.getHost()).isEqualTo("mailhog");
        assertThat(mailProperties.getPort()).isEqualTo(1025);
        assertThat(environment.getProperty("spring.mail.username", "")).isBlank();
        assertThat(environment.getProperty("spring.mail.password", "")).isBlank();
    }

    @Test
    void verificationMailDispatcherIsAsyncAndPersistsRequestIdTrace() {
        assertThat(AopUtils.isAopProxy(dispatcher)).isTrue();
        assertThat(asyncProcessor).isNotNull();
        assertThat(attemptMapper).isNotNull();
        assertThat(requestIdFilter).isNotNull();
        assertThat(MailTraceContext.normalizeRequestId("  req-123  ")).isEqualTo("req-123");
        assertThat(MailTraceContext.normalizeRequestId(null)).isNotBlank();
    }

    @Configuration
    @EnableAsync
    static class AsyncTestConfiguration {
    }
}
