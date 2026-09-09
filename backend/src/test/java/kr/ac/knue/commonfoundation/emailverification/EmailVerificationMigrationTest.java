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

class EmailVerificationMigrationTest {
    private static final String SUBSTITUTE_EMAIL = "admin@kndadmin.com";
    private static final Pattern NORMAL_EMAIL_UNIQUE_INDEX = Pattern.compile(
            "create\\s+unique\\s+index\\s+if\\s+not\\s+exists\\s+\\S+\\s+on\\s+users\\s*\\([^;]*lower\\s*\\(\\s*email\\s*\\)[^;]*\\)\\s+where\\s+[^;]*(email_substitute_yn\\s*=\\s*'n'|coalesce\\s*\\(\\s*email_substitute_yn\\s*,\\s*'n'\\s*\\)\\s*=\\s*'n')",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final String allMigrationSql;
    private final String basic58MigrationSql;

    EmailVerificationMigrationTest() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        allMigrationSql = readSql(resolver.getResources("classpath*:db/migration/*.sql")).toLowerCase(Locale.ROOT);
        basic58MigrationSql = readSql(resolver.getResources("classpath*:db/migration/*basic58*.sql")).toLowerCase(Locale.ROOT);
    }

    @Test
    void existingAdminExemptionMarksVerifiedButPreservesActiveLoginEligibility() {
        assertThat(basic58MigrationSql)
                .as("T001/REQ-1743: a new BASIC-58 migration must explicitly exempt the baseline admin account")
                .contains("where login_id = 'admin'")
                .contains("email_verified_yn = 'y'")
                .contains("verification_origin = 'exempt_existing'")
                .contains("event_type")
                .contains("exempt_existing")
                .doesNotContain("status = 'active' where login_id = 'admin'")
                .doesNotContain("system_use_yn = 'y' where login_id = 'admin'");

        assertThat(allMigrationSql)
                .as("admin seed credentials/role remain the login baseline protected by exemption")
                .contains("values ('admin'")
                .contains("'r09'")
                .contains("'active'");
    }

    @Test
    void inactiveAndDeletedExistingUsersReceiveExemptionWithoutStatusReactivation() {
        assertThat(basic58MigrationSql)
                .as("T002/REQ-1744: exemption must cover inactive/suspended/deleted legacy rows without reactivation")
                .contains("email_verified_yn = 'y'")
                .contains("verification_origin = 'exempt_existing'")
                .contains("'active'")
                .contains("'inactive'")
                .contains("'deleted'")
                .doesNotContain("set status = 'active'")
                .doesNotContain("set system_use_yn = 'y'")
                .doesNotContain("where email_verified_yn = 'n'");
    }

    @Test
    void migrationPreservesCredentialsRolesPermissionsBusinessLinksAndExistingEmails() {
        assertThat(basic58MigrationSql)
                .as("T003/REQ-1745: BASIC-58 migration may add verification data but must not rewrite existing identity/authz/business links")
                .contains("alter table users")
                .contains("email")
                .contains("email_verified_yn")
                .contains("email_verification_events")
                .doesNotContain("update users set login_id")
                .doesNotContain("update users set password_hash")
                .doesNotContain("delete from user_roles")
                .doesNotContain("update user_roles")
                .doesNotContain("delete from menu_permissions")
                .doesNotContain("update menu_permissions")
                .doesNotContain("delete from organization_user_mappings")
                .doesNotContain("update organization_user_mappings")
                .doesNotContain("set email = '" + SUBSTITUTE_EMAIL + "'");
    }

    @Test
    void nullableEmailPathKeepsMissingLegacyEmailNullAndNotNullPathUsesMarkedSubstituteOnly() {
        assertThat(basic58MigrationSql)
                .as("T004/REQ-1749: migration documents and implements both null-allowed and not-null substitute compatibility paths")
                .contains("email_substitute_yn")
                .contains(SUBSTITUTE_EMAIL)
                .contains("where email is null")
                .contains("email_substitute_yn = 'y'")
                .contains("email_verified_yn = 'y'")
                .contains("verification_origin = 'exempt_existing'")
                .doesNotContain("coalesce(email, '" + SUBSTITUTE_EMAIL + "')")
                .doesNotContain("where login_id like '%admin%'");
    }

    @Test
    void substituteExceptionAndNormalCaseInsensitiveUniquenessAreEnforcedByDbAndApplicationContract() throws IOException {
        assertThat(basic58MigrationSql)
                .as("T005/REQ-1751,REQ-1752: DB uniqueness must be case-insensitive for normal email while excluding only marked substitutes")
                .contains("email_substitute_yn")
                .contains("lower(email)")
                .doesNotContain("unique (email)");
        assertThat(NORMAL_EMAIL_UNIQUE_INDEX.matcher(basic58MigrationSql).find())
                .as("expected partial unique index on lower(email) where email_substitute_yn is N")
                .isTrue();

        String signupSource = readApplicationSource("Signup").toLowerCase(Locale.ROOT);
        assertThat(signupSource)
                .as("application signup path must normalize email and reject duplicates/substitute before insert")
                .contains("trim")
                .contains("tolowercase")
                .contains(SUBSTITUTE_EMAIL)
                .contains("duplicate")
                .contains("email");
    }

    private String readSql(Resource[] resources) {
        return readSql(Arrays.asList(resources));
    }

    private String readSql(List<Resource> resources) {
        return resources.stream()
                .map(this::readResource)
                .reduce("", (left, right) -> left + "\n" + right);
    }

    private String readResource(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("migration SQL을 읽을 수 없습니다.", exception);
        }
    }

    private String readApplicationSource(String requiredNameFragment) throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        List<Resource> resources = Arrays.stream(resolver.getResources("file:src/main/**/*" + requiredNameFragment + "*.*"))
                .toList();
        assertThat(resources)
                .as("BASIC-58 application source containing %s must exist", requiredNameFragment)
                .isNotEmpty();
        return readSql(resources);
    }
}
