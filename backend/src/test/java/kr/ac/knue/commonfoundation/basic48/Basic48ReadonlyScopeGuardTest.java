package kr.ac.knue.commonfoundation.basic48;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class Basic48ReadonlyScopeGuardTest {
    private static final List<String> BASIC48_PATHS = List.of(
            "/api/business/evaluation-snapshots",
            "/api/business/score-calculation-histories",
            "/api/business/score-adjustment-histories",
            "/api/business/score-recalculation-histories");
    private static final List<String> MUTATING_MAPPING_ANNOTATIONS = List.of(
            "@PostMapping", "@PutMapping", "@PatchMapping", "@DeleteMapping");
    private static final List<String> OUT_OF_SCOPE_CREATE_OR_EXECUTE_TERMS = List.of(
            "createScore", "updateScore", "deleteScore", "executeRecalculation", "confirmEvaluation",
            "saveFinalEvaluationConfirmationTransition", "createEvaluationMaterialGeneration",
            "createEvaluationMaterialDeletion", "createScoreRecalculation");

    @Test
    void basic48ControllersKeepOnlyReadonlyGetMappingsForReq1498Req1499Req1549Req1550() throws Exception {
        String controllerSource = Files.readString(Path.of("src/main/java/kr/ac/knue/commonfoundation/basic48/EvaluationSnapshotController.java"), StandardCharsets.UTF_8)
                + Files.readString(Path.of("src/main/java/kr/ac/knue/commonfoundation/basic48/ScoreCalculationHistoryController.java"), StandardCharsets.UTF_8)
                + Files.readString(Path.of("src/main/java/kr/ac/knue/commonfoundation/basic48/ScoreAdjustmentHistoryController.java"), StandardCharsets.UTF_8)
                + Files.readString(Path.of("src/main/java/kr/ac/knue/commonfoundation/basic48/ScoreRecalculationHistoryController.java"), StandardCharsets.UTF_8);
        for (String path : BASIC48_PATHS) {
            assertThat(controllerSource).as(path + " controller path must exist").contains(path);
        }
        for (String annotation : MUTATING_MAPPING_ANNOTATIONS) {
            assertThat(controllerSource).as("BASIC-48 is 조회 전용 and must not expose command endpoints")
                    .doesNotContain(annotation);
        }
    }

    @Test
    void basic48SourceDoesNotReferenceReadonlyInputDocsOrOutOfScopeMutatingBusinessFlowsForReq1505Req1549Req1550() throws Exception {
        for (Path source : sourceFiles(Path.of("src/main/java/kr/ac/knue/commonfoundation/basic48"))) {
            String content = Files.readString(source, StandardCharsets.UTF_8);
            for (String forbiddenTerm : OUT_OF_SCOPE_CREATE_OR_EXECUTE_TERMS) {
                assertThat(content).as(source + " must not implement excluded Group G command flow")
                        .doesNotContain(forbiddenTerm);
            }
        }
    }

    @Test
    void basic48MapperQueriesUseDynamicPredicatesInsteadOfNullBoundPostgresqlFiltersForReq1530() throws Exception {
        for (Path mapper : sourceFiles(Path.of("src/main/resources/mapper/basic48"))) {
            String content = Files.readString(mapper, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
            assertThat(content).as(mapper + " must avoid null-bound optional predicates")
                    .doesNotContain("is null or")
                    .doesNotContain("coalesce(");
            assertThat(content).as(mapper + " should dynamically add optional filters")
                    .contains("<if test=");
        }
    }

    private List<Path> sourceFiles(Path root) throws IOException {
        try (var files = Files.walk(root)) {
            return files.filter(Files::isRegularFile).sorted().toList();
        }
    }
}
