package kr.ac.knue.commonfoundation.emailverification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;

class GmailVerificationMailSenderTest {
    @Test
    void javaMailPropertiesEnableStartTlsAndBoundedTimeoutsWithoutDisablingCertificateValidation() {
        GmailSmtpMailProperties properties = new GmailSmtpMailProperties();
        properties.setHost("smtp.gmail.com");
        properties.setPort(587);
        properties.setUsername("sender@gmail.com");
        properties.setPassword("app-password");
        properties.setFrom("sender@gmail.com");
        properties.setSmtpAuth(true);
        properties.setStarttlsEnable(true);
        properties.setConnectTimeout(Duration.ofSeconds(7));
        properties.setReadTimeout(Duration.ofSeconds(8));
        properties.setWriteTimeout(Duration.ofSeconds(9));

        Properties javaMail = properties.toJavaMailProperties();

        assertThat(javaMail.getProperty("mail.smtp.auth")).isEqualTo("true");
        assertThat(javaMail.getProperty("mail.smtp.starttls.enable")).isEqualTo("true");
        assertThat(javaMail.getProperty("mail.smtp.starttls.required")).isEqualTo("true");
        assertThat(javaMail.getProperty("mail.smtp.connectiontimeout")).isEqualTo("7000");
        assertThat(javaMail.getProperty("mail.smtp.timeout")).isEqualTo("8000");
        assertThat(javaMail.getProperty("mail.smtp.writetimeout")).isEqualTo("9000");
        assertThat(javaMail.stringPropertyNames()).noneMatch(name -> name.contains("ssl.trust") || name.contains("checkserveridentity"));
    }

    @Test
    void fromAliasDifferentFromGmailUsernameRequiresExplicitVerifiedAliasFlag() {
        GmailSmtpMailProperties properties = new GmailSmtpMailProperties();
        properties.setUsername("sender@gmail.com");
        properties.setPassword("app-password");
        properties.setFrom("alias@example.com");

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("검증된 Gmail 별칭");

        properties.setVerifiedFromAlias(true);
        properties.validate();
    }

    @Test
    void mailExceptionsMapToDistinctDeliveryDiagnostics() {
        assertThat(GmailVerificationMailSender.toDeliveryException(new MailAuthenticationException("auth")))
                .extracting(VerificationMailDeliveryException::deliveryStatus,
                        VerificationMailDeliveryException::diagnosticCategory)
                .containsExactly("FAILED_AUTH", "AUTH");

        assertThat(GmailVerificationMailSender.toDeliveryException(new MailSendException("timeout", new SocketTimeoutException("read timeout"))))
                .extracting(VerificationMailDeliveryException::deliveryStatus,
                        VerificationMailDeliveryException::diagnosticCategory)
                .containsExactly("FAILED_TIMEOUT", "TIMEOUT");

        assertThat(GmailVerificationMailSender.toDeliveryException(new MailSendException("454 rate limit")))
                .extracting(VerificationMailDeliveryException::deliveryStatus,
                        VerificationMailDeliveryException::diagnosticCategory)
                .containsExactly("FAILED_RATE_LIMIT", "RATE_LIMIT");
    }
}
