package kr.ac.knue.commonfoundation.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class NoOutOfScopeBusinessApiTest {
    private static final List<String> OUT_OF_SCOPE_API_TERMS = List.of(
            "achievement",
            "performance",
            "evaluation-rule",
            "evaluation-period",
            "approval",
            "evaluation-target",
            "score",
            "business-report",
            "application",
            "grant",
            "file",
            "files",
            "excel",
            "personal-information",
            "audit-log");

    @Test
    void openApiDoesNotExposeOutOfScopeBusinessApis() throws Exception {
        String contract = new String(new ClassPathResource("contracts/openapi.yaml").getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        Matcher pathMatcher = Pattern.compile("(?m)^  (/api/[^:]+):$").matcher(contract);

        while (pathMatcher.find()) {
            String path = pathMatcher.group(1).toLowerCase(Locale.ROOT);
            for (String forbidden : OUT_OF_SCOPE_API_TERMS) {
                assertThat(path)
                        .as("out-of-scope business API must not be generated: %s", path)
                        .doesNotContain(forbidden);
            }
        }
    }
}
