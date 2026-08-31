package kr.ac.knue.commonfoundation.basic32;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CommonReusePreconditionValidatorTest {
    private final CommonReusePreconditionValidator validator = new CommonReusePreconditionValidator();

    @Test
    void existingRepositoryUsesSingleBackendFrontendInfraComposeAndPostgresRuntimeForReq713() {
        CommonReusePreconditionValidator.PreconditionReport report = validator.validate(repositoryRoot());

        assertThat(report.status()).as("기존 공통 저장소 선행조건은 신규 프로젝트 없이 준비되어야 한다.").isEqualTo("READY");
        assertThat(report.missingContracts()).isEmpty();
        assertThat(report.violations()).isEmpty();
    }

    @Test
    void missingCommonFoundationReportsPreconditionFailureWithoutCreatingFallbackContractsForReq715(@TempDir Path tempDir) {
        CommonReusePreconditionValidator.PreconditionReport report = validator.validate(tempDir);

        assertThat(report.status()).isEqualTo("PRECONDITION_FAILED");
        assertThat(report.ready()).isFalse();
        assertThat(report.missingContracts())
                .contains("backend", "frontend", "infra/docker-compose.yml")
                .contains("backend/src/main/java/kr/ac/knue/commonfoundation/auth/AuthenticationPort.java")
                .contains("backend/src/main/java/kr/ac/knue/commonfoundation/permissions/EffectivePermissionService.java");
        assertThat(Files.exists(tempDir.resolve("backend"))).as("누락된 공통기능을 임의 생성하지 않는다.").isFalse();
        assertThat(Files.exists(tempDir.resolve("frontend"))).as("누락된 프론트 shell을 임의 생성하지 않는다.").isFalse();
        assertThat(Files.exists(tempDir.resolve("infra/docker-compose.yml"))).as("두 번째 compose를 임의 생성하지 않는다.").isFalse();
    }

    @Test
    void duplicateComposeIsReportedAsContractViolationInsteadOfStartingNewRuntimeForReq713(@TempDir Path tempDir) throws Exception {
        Files.createDirectories(tempDir.resolve("backend/src/main/java/kr/ac/knue/commonfoundation/auth"));
        Files.createDirectories(tempDir.resolve("backend/src/main/java/kr/ac/knue/commonfoundation/permissions"));
        Files.createDirectories(tempDir.resolve("backend/src/main/resources/db/migration"));
        Files.createDirectories(tempDir.resolve("frontend/src/app"));
        Files.createDirectories(tempDir.resolve("frontend/src/components/layout"));
        Files.createDirectories(tempDir.resolve("frontend/src/api"));
        Files.createDirectories(tempDir.resolve("infra"));
        Files.writeString(tempDir.resolve("infra/docker-compose.yml"), """
                services:
                  database:
                    image: postgres:16.4-alpine
                  backend:
                    image: common-foundation-backend:latest
                    pull_policy: build
                    build:
                      context: ../backend
                  frontend:
                    image: common-foundation-frontend:latest
                    pull_policy: build
                    build:
                      context: ../frontend
                """);
        Files.writeString(tempDir.resolve("docker-compose.yml"), "services: {}\n");
        for (String path : requiredFiles()) {
            Files.writeString(tempDir.resolve(path), "contract\n");
        }
        Files.writeString(tempDir.resolve("backend/src/main/resources/db/migration/V1__common_foundation_schema.sql"), "select 1;\n");

        CommonReusePreconditionValidator.PreconditionReport report = validator.validate(tempDir);

        assertThat(report.status()).isEqualTo("CONTRACT_VIOLATION");
        assertThat(report.violations()).anySatisfy(violation -> assertThat(violation).contains("두 번째 Docker Compose"));
    }

    private Path repositoryRoot() {
        Path workingDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        if (workingDirectory.getFileName() != null && "backend".equals(workingDirectory.getFileName().toString())) {
            return workingDirectory.getParent();
        }
        return workingDirectory;
    }

    private String[] requiredFiles() {
        return new String[] {
                "backend/src/main/java/kr/ac/knue/commonfoundation/auth/AuthenticationPort.java",
                "backend/src/main/java/kr/ac/knue/commonfoundation/auth/AuthenticationFilter.java",
                "backend/src/main/java/kr/ac/knue/commonfoundation/auth/AuthController.java",
                "backend/src/main/java/kr/ac/knue/commonfoundation/permissions/EffectivePermissionService.java",
                "backend/src/main/java/kr/ac/knue/commonfoundation/permissions/PermissionMapper.java",
                "frontend/package.json",
                "frontend/src/app/router.tsx",
                "frontend/src/components/layout/AdminShell.tsx",
                "frontend/src/api/apiClient.ts"
        };
    }
}
