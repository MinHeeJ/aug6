package kr.ac.knue.commonfoundation.privacy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class DecryptionAuditRecorderWiringTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(DuplicateAuditRecorderConfiguration.class)
            .withBean(PrivacyCryptoProperties.class)
            .withBean(PrivacyCryptoService.class)
            .withBean(NoopDecryptionAuditRecorder.class);

    @Test
    void resolvesNoopAuditRecorderWhenLegacyAuditRecorderIsAlsoRegistered() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(DecryptionAuditRecorder.class))
                    .isInstanceOf(NoopDecryptionAuditRecorder.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class DuplicateAuditRecorderConfiguration {
        @Bean
        DecryptionAuditRecorder decryptionAuditRecorder() {
            return event -> {
            };
        }
    }
}
