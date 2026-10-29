package kr.ac.knue.commonfoundation.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SecurityRemediationContractTest {
    @Test
    void authenticationAdapterUsesConstantTimeHashComparison() throws Exception {
        String adapterSource = readSource("auth/LocalAccountAuthenticationAdapter.java");

        assertThat(adapterSource)
                .contains("MessageDigest.isEqual(")
                .doesNotContain("(\"sha256:\" + sha256(rawPassword)).equals(storedHash)");
    }

    @Test
    void permissionMapperBindsEachRoleCodeWithMyBatisForeach() throws Exception {
        String mapperSource = readSource("permissions/PermissionMapper.java");

        assertThat(mapperSource)
                .contains("<script>")
                .contains("<foreach collection=\"roleCodes\" item=\"roleCode\"")
                .contains("#{roleCode}")
                .contains("@Param(\"roleCodes\") List<String> roleCodes")
                .doesNotContain("${roleCodesCsv}");
    }

    private String readSource(String relativePath) throws Exception {
        return Files.readString(
                Path.of("src", "main", "java", "kr", "ac", "knue", "commonfoundation").resolve(relativePath),
                StandardCharsets.UTF_8);
    }
}
