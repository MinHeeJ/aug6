package kr.ac.knue.commonfoundation.emailverification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression("'${MAIL_USERNAME:}' == '' || '${MAIL_PASSWORD:}' == ''")
public class NotConfiguredVerificationMailSender implements VerificationMailSender {
    @Override
    public void sendVerificationMail(String normalizedEmail, String rawToken) {
        throw new VerificationMailDeliveryException(
                "FAILED_OTHER",
                "CONFIGURATION",
                "Gmail SMTP 발송 설정이 완료되지 않았습니다.",
                true
        );
    }
}
