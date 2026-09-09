package kr.ac.knue.commonfoundation.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class SignupMailFoundationMigrationTest {
    private static final Path MIGRATION = Path.of("src/main/resources/db/migration/V56__basic57_signup_mail_foundation.sql");

    @Test
    void migrationExtendsExistingUsersAndCreatesHashOnlyTokenAndMailAttemptTables() throws IOException {
        String sql = Files.readString(MIGRATION);

        assertThat(sql).contains("ALTER TABLE users ADD COLUMN IF NOT EXISTS email varchar(254)");
        assertThat(sql).contains("ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified_yn varchar(1)");
        assertThat(sql).contains("ALTER TABLE users ADD COLUMN IF NOT EXISTS account_status varchar(20)");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS email_verification_tokens");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS mail_send_attempts");
        assertThat(sql).contains("token_hash varchar(64) NOT NULL");
        assertThat(sql).doesNotContain(" raw_token", " plain_token", " token_value", " original_token");
        assertThat(sql).contains("CONSTRAINT fk_email_verification_tokens_user FOREIGN KEY (user_id) REFERENCES users(user_id)");
        assertThat(sql).contains("CONSTRAINT fk_mail_send_attempts_user FOREIGN KEY (user_id) REFERENCES users(user_id)");
    }

    @Test
    void migrationDefinesIdempotentConstraintsIndexesCommentsAndSeedFixtures() throws IOException {
        String sql = Files.readString(MIGRATION);

        assertThat(sql).contains("CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email_lower");
        assertThat(sql).contains("CREATE UNIQUE INDEX IF NOT EXISTS uq_email_verification_tokens_token_hash");
        assertThat(sql).contains("CREATE INDEX IF NOT EXISTS idx_mail_send_attempts_email_reg_dt");
        assertThat(sql).contains("COMMENT ON COLUMN users.account_status IS 'PENDING_EMAIL:이메일인증대기|ACTIVE:활성|SUSPENDED:정지'");
        assertThat(sql).contains("COMMENT ON COLUMN mail_send_attempts.send_status IS 'REQUESTED:요청됨|SENT:발송됨|FAILED:실패|RATE_LIMITED:재요청제한|SKIPPED_ACTIVE:인증완료스킵|SKIPPED_UNKNOWN:미존재스킵'");
        assertThat(countMatches(sql, "BASIC57_USER_")).isGreaterThanOrEqualTo(3);
        assertThat(countMatches(sql, "insert into email_verification_tokens")).isGreaterThanOrEqualTo(3);
        assertThat(countMatches(sql, "insert into mail_send_attempts")).isGreaterThanOrEqualTo(3);
        assertThat(sql).contains("test_active", "test_pending", "test_suspended", "BASIC57_TOKEN_USED", "BASIC57_TOKEN_EXPIRED", "BASIC57_MAIL_FAILED");
    }

    private static int countMatches(String input, String literal) {
        return (int) Pattern.compile(Pattern.quote(literal)).matcher(input).results().count();
    }
}
