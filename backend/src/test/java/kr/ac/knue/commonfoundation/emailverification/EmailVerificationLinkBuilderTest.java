package kr.ac.knue.commonfoundation.emailverification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailVerificationLinkBuilderTest {
    @Test
    void verificationLinkUsesConfiguredBaseUrlAndIgnoresArbitraryHostHeader() {
        EmailVerificationLinkBuilder builder = new EmailVerificationLinkBuilder("https://preview.example.ac.kr/app/");

        String link = builder.buildVerificationLink(
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                "attacker.example.com"
        );

        assertThat(link).isEqualTo("https://preview.example.ac.kr/app/email-verification?token=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        assertThat(link).doesNotContain("attacker.example.com");
    }
}
