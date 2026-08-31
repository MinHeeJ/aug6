package kr.ac.knue.commonfoundation.basic29;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class Basic29TraceabilityValidationTest {

    @Test
    void designDocChangeSetIsMaterializedAsDurableCiInputForReq622() throws Exception {
        String changeSet = readTraceFixture("design-doc-change-set.json");

        assertThat(changeSet).contains("\"documents\"");
        for (String document : List.of(
                "data-model.md",
                "contracts/openapi.yaml",
                "research.md",
                "quickstart.md",
                "architecture.md",
                "ui-design.md")) {
            assertThat(changeSet)
                    .as(document + " must remain connected to BASIC-29 design validation")
                    .contains("\"path\": \"" + document + "\"", "\"status\": \"changed\"", "\"patch_ref\"");
        }
    }

    @Test
    void requirementTraceSidecarConnectsPhaseOneTasksToCiGuardsWithoutRunnerOwnedInputRuntimeReference() throws Exception {
        String sidecar = readTraceFixture("requirement-trace-sidecar.json");

        assertThat(sidecar)
                .contains("basic29.requirement-trace-sidecar.v1")
                .contains("\"taskId\": \"T001\"", "\"taskId\": \"T002\"", "\"taskId\": \"T003\"")
                .contains("REQ-622", "REQ-690", "REQ-693", "REQ-696", "REQ-703")
                .contains("REQ-644", "REQ-645", "REQ-661", "REQ-662")
                .contains("Basic29TraceabilityValidationTest")
                .contains("Basic29SchemaMigrationRedTest")
                .contains("Basic29FoundationContractRedTest");
    }

    private String readTraceFixture(String filename) throws Exception {
        return new ClassPathResource("requirements/basic29/" + filename)
                .getContentAsString(StandardCharsets.UTF_8);
    }
}
