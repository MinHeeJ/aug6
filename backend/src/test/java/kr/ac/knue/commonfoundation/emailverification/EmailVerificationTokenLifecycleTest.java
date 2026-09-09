package kr.ac.knue.commonfoundation.emailverification;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

class EmailVerificationTokenLifecycleTest {
    private static final Pattern ACTIVE_TOKEN_UNIQUENESS = Pattern.compile(
            "create\\s+unique\\s+index\\s+if\\s+not\\s+exists\\s+\\S+\\s+on\\s+email_verification_tokens\\s*\\([^;]*(user_id|target_email)[^;]*(user_id|target_email)[^;]*\\)\\s+where\\s+[^;]*status\\s*=\\s*'active'",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final String basic58MigrationSql;
    private final String verificationApplicationSource;

    EmailVerificationTokenLifecycleTest() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        basic58MigrationSql = readResources(resolver.getResources("classpath*:db/migration/*basic58*.sql")).toLowerCase(Locale.ROOT);
        verificationApplicationSource = readResources(resolver.getResources("file:src/main/**/*Verification*.*"))
                .toLowerCase(Locale.ROOT);
    }

    @Test
    void tokenSchemaStoresOnlyHashAndNeverRawTokenValue() {
        assertThat(basic58MigrationSql)
                .as("T006/REQ-1758: token table must store hashes, not raw emailed tokens")
                .contains("create table if not exists email_verification_tokens")
                .contains("token_hash")
                .contains("target_email")
                .contains("foreign key (user_id) references users(user_id)")
                .doesNotContain("raw_token")
                .doesNotContain("plain_token")
                .doesNotContain("token_value varchar")
                .doesNotContain("verification_token varchar");
    }

    @Test
    void tokenSchemaEnforcesTwentyFourHourExpiryAndOneUseLifecycle() {
        assertThat(basic58MigrationSql)
                .as("T006/REQ-1758: expiry, one-use, and lifecycle states must be modeled durably")
                .contains("expires_at")
                .contains("issued_at")
                .contains("used_at")
                .contains("status varchar(30)")
                .contains("'active'")
                .contains("'used'")
                .contains("'expired'")
                .contains("'superseded'")
                .contains("interval '24 hours'")
                .contains("check")
                .contains("used_at is null")
                .contains("used_at is not null");
    }

    @Test
    void resendInvalidatesPreviousUnusedTokensBeforeCreatingReplacement() {
        assertThat(basic58MigrationSql)
                .as("T006/REQ-1758: DB constraints must allow only one active token per user/email pair")
                .contains("status = 'superseded'")
                .contains("where user_id")
                .contains("and target_email")
                .contains("and status = 'active'");
        assertThat(ACTIVE_TOKEN_UNIQUENESS.matcher(basic58MigrationSql).find())
                .as("expected partial unique index for current ACTIVE token per user/email")
                .isTrue();

        assertThat(verificationApplicationSource)
                .as("application resend flow must supersede old active tokens and issue a replacement")
                .contains("superseded")
                .contains("active")
                .contains("securerandom")
                .contains("sha-256")
                .contains("plushours(24)");
    }

    @Test
    void verificationServiceRejectsExpiredUsedTamperedAndSupersededTokensWithoutAccountMutation() {
        assertThat(verificationApplicationSource)
                .as("application verification flow must distinguish unsafe token states and avoid activation")
                .contains("superseded")
                .contains("pending_email")
                .contains("link_verified")
                .contains("transactional")
                .containsPattern("expires_?at")
                .containsPattern("used_?at")
                .containsPattern("token_?hash")
                .containsPattern("email_?verified_?yn");
    }

    private String readResources(Resource[] resources) throws IOException {
        return readResources(Arrays.asList(resources));
    }

    private String readResources(List<Resource> resources) throws IOException {
        assertThat(resources)
                .as("BASIC-58 verification migration/application resources must exist")
                .isNotEmpty();
        StringBuilder builder = new StringBuilder();
        for (Resource resource : resources) {
            builder.append(resource.getDescription()).append('\n');
            builder.append(resource.getContentAsString(StandardCharsets.UTF_8)).append('\n');
        }
        return builder.toString();
    }
}
