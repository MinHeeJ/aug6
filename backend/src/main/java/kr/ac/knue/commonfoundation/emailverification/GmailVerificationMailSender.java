package kr.ac.knue.commonfoundation.emailverification;

import java.net.SocketTimeoutException;
import java.util.Locale;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@ConditionalOnExpression("'${MAIL_USERNAME:}' != '' && '${MAIL_PASSWORD:}' != ''")
public class GmailVerificationMailSender implements VerificationMailSender {
    private final GmailSmtpMailProperties properties;
    private final EmailVerificationLinkBuilder linkBuilder;
    private final JavaMailSenderImpl mailSender;

    public GmailVerificationMailSender(GmailSmtpMailProperties properties, EmailVerificationLinkBuilder linkBuilder) {
        this.properties = properties;
        this.linkBuilder = linkBuilder;
        this.mailSender = new JavaMailSenderImpl();
        this.properties.validate();
        this.mailSender.setHost(properties.getHost());
        this.mailSender.setPort(properties.getPort());
        this.mailSender.setUsername(properties.getUsername());
        this.mailSender.setPassword(properties.getPassword());
        this.mailSender.setJavaMailProperties(properties.toJavaMailProperties());
    }

    @Override
    public void sendVerificationMail(String normalizedEmail, String rawToken) {
        EmailAddressPolicy.validateSensitiveMailTarget(normalizedEmail);
        org.springframework.mail.SimpleMailMessage message = new org.springframework.mail.SimpleMailMessage();
        message.setFrom(properties.getFrom());
        message.setTo(EmailAddressPolicy.normalizeEmail(normalizedEmail));
        message.setSubject("회원가입 이메일 인증 안내");
        message.setText("아래 링크로 이메일 인증을 완료해 주세요.\n" + linkBuilder.buildVerificationLink(rawToken, null));
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw toDeliveryException(exception);
        }
    }

    static VerificationMailDeliveryException toDeliveryException(MailException exception) {
        String safeMessage = sanitizeMessage(exception.getMessage());
        if (exception instanceof MailAuthenticationException) {
            return new VerificationMailDeliveryException("FAILED_AUTH", "AUTH", safeMessage, true);
        }
        if (containsTimeout(exception)) {
            return new VerificationMailDeliveryException("FAILED_TIMEOUT", "TIMEOUT", safeMessage, true);
        }
        if (looksLikeRateLimit(safeMessage)) {
            return new VerificationMailDeliveryException("FAILED_RATE_LIMIT", "RATE_LIMIT", safeMessage, true);
        }
        return new VerificationMailDeliveryException("FAILED_OTHER", "UNKNOWN", safeMessage, true);
    }

    private static boolean containsTimeout(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean looksLikeRateLimit(String message) {
        String normalized = message == null ? "" : message.toLowerCase(Locale.ROOT);
        return normalized.contains("rate")
                || normalized.contains("limit")
                || normalized.contains("quota")
                || normalized.contains("454")
                || normalized.contains("421")
                || normalized.contains("too many");
    }

    private static String sanitizeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Gmail SMTP 발송 실패";
        }
        return message.replaceAll("(?i)(password|secret|token|authorization)=?[^\\s,;]*", "$1=REDACTED");
    }
}
