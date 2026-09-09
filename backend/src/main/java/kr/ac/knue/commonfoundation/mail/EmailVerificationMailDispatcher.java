package kr.ac.knue.commonfoundation.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailVerificationMailDispatcher {
    private static final String MAIL_TYPE = "EMAIL_VERIFICATION";

    private final JavaMailSender mailSender;
    private final VerificationMailProperties properties;
    private final MailSendAttemptMapper attemptMapper;

    public EmailVerificationMailDispatcher(JavaMailSender mailSender,
                                           VerificationMailProperties properties,
                                           MailSendAttemptMapper attemptMapper) {
        this.mailSender = mailSender;
        this.properties = properties;
        this.attemptMapper = attemptMapper;
    }

    @Async("mailTaskExecutor")
    public void sendVerificationMail(VerificationMailCommand command) {
        String email = command.email().trim().toLowerCase(Locale.ROOT);
        String requestId = MailTraceContext.normalizeRequestId(command.requestId());
        long attemptId = attemptMapper.insertRequested(command.userId(), email, MAIL_TYPE, requestId);
        try {
            if (!properties.isAllowedVerificationBaseUrl()) {
                throw new IllegalStateException("verification baseUrl must use https except localhost development profile");
            }
            mailSender.send(buildMessage(command, email));
            attemptMapper.markSent(attemptId);
        } catch (RuntimeException | MessagingException exception) {
            attemptMapper.markFailed(attemptId, safeFailureReason(exception));
        }
    }

    private MimeMessage buildMessage(VerificationMailCommand command, String email) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        helper.setFrom(properties.getFrom());
        helper.setTo(email);
        helper.setSubject(properties.getSubject());
        helper.setText(plainText(command), html(command));
        return message;
    }

    private String plainText(VerificationMailCommand command) {
        return "가입 아이디 " + command.loginId() + "의 이메일 인증을 완료해주세요. 인증 링크는 24시간 동안 유효합니다. "
                + verificationUrl(command) + " 본인이 가입하지 않았다면 이 메일을 무시해주세요.";
    }

    private String html(VerificationMailCommand command) {
        String loginId = escape(command.loginId());
        String verificationUrl = escape(verificationUrl(command));
        return "<html><body>"
                + "<h1>이메일 인증을 완료해주세요</h1>"
                + "<p>가입 아이디: <strong>" + loginId + "</strong></p>"
                + "<p>아래 버튼을 눌러 이메일 인증을 완료해주세요. 인증 링크는 24시간 동안 유효합니다.</p>"
                + "<p><a href=\"" + verificationUrl + "\" style=\"display:inline-block;padding:12px 18px;background:#1d4ed8;color:#fff;text-decoration:none;border-radius:6px\">이메일 인증하기</a></p>"
                + "<p>본인이 가입하지 않았다면 이 메일을 무시해주세요.</p>"
                + "</body></html>";
    }

    private String verificationUrl(VerificationMailCommand command) {
        String baseUrl = properties.getBaseUrl().replaceAll("/+$", "");
        return baseUrl + "/api/auth/verify-email?token=" + command.verificationToken();
    }

    private String safeFailureReason(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
