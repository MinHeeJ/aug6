package kr.ac.knue.commonfoundation.basic32;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class CommonReusePreconditionValidator {
    private static final List<String> REQUIRED_ROOT_ITEMS = List.of(
            "backend",
            "frontend",
            "infra/docker-compose.yml");
    private static final List<String> REQUIRED_BACKEND_CONTRACTS = List.of(
            "backend/src/main/java/kr/ac/knue/commonfoundation/auth/AuthenticationPort.java",
            "backend/src/main/java/kr/ac/knue/commonfoundation/auth/AuthenticationFilter.java",
            "backend/src/main/java/kr/ac/knue/commonfoundation/auth/AuthController.java",
            "backend/src/main/java/kr/ac/knue/commonfoundation/permissions/EffectivePermissionService.java",
            "backend/src/main/java/kr/ac/knue/commonfoundation/permissions/PermissionMapper.java",
            "backend/src/main/resources/db/migration");
    private static final List<String> REQUIRED_FRONTEND_CONTRACTS = List.of(
            "frontend/package.json",
            "frontend/src/app/router.tsx",
            "frontend/src/components/layout/AdminShell.tsx",
            "frontend/src/api/apiClient.ts");
    private static final Pattern BASIC_32_MIGRATION_PATTERN = Pattern.compile("V2[1-6]__basic32.*\\.sql");

    public PreconditionReport validate(Path repositoryRoot) {
        Path root = repositoryRoot.toAbsolutePath().normalize();
        if (isBackendBuildContext(root)) {
            return validateBackendBuildContext(root);
        }
        List<String> missingContracts = new ArrayList<>();
        List<String> violations = new ArrayList<>();

        requireExisting(root, REQUIRED_ROOT_ITEMS, missingContracts);
        requireExisting(root, REQUIRED_BACKEND_CONTRACTS, missingContracts);
        requireExisting(root, REQUIRED_FRONTEND_CONTRACTS, missingContracts);
        validateSingleCompose(root, violations);
        validateComposeContract(root.resolve("infra/docker-compose.yml"), missingContracts, violations);
        validateMigrationBoundary(root.resolve("backend/src/main/resources/db/migration"), missingContracts, violations);

        if (!missingContracts.isEmpty()) {
            return new PreconditionReport("PRECONDITION_FAILED", List.copyOf(missingContracts), List.copyOf(violations));
        }
        if (!violations.isEmpty()) {
            return new PreconditionReport("CONTRACT_VIOLATION", List.copyOf(missingContracts), List.copyOf(violations));
        }
        return new PreconditionReport("READY", List.copyOf(missingContracts), List.copyOf(violations));
    }

    private boolean isBackendBuildContext(Path root) {
        return Files.exists(root.resolve("pom.xml"))
                && Files.isDirectory(root.resolve("src/main/java/kr/ac/knue/commonfoundation"))
                && !Files.exists(root.resolve("backend"));
    }

    private PreconditionReport validateBackendBuildContext(Path root) {
        List<String> missingContracts = new ArrayList<>();
        List<String> violations = new ArrayList<>();
        requireExisting(root, REQUIRED_BACKEND_CONTRACTS.stream()
                .map(path -> path.replaceFirst("^backend/", ""))
                .toList(), missingContracts);
        validateMigrationBoundary(root.resolve("src/main/resources/db/migration"), missingContracts, violations);
        if (!missingContracts.isEmpty()) {
            return new PreconditionReport("PRECONDITION_FAILED", List.copyOf(missingContracts), List.copyOf(violations));
        }
        if (!violations.isEmpty()) {
            return new PreconditionReport("CONTRACT_VIOLATION", List.copyOf(missingContracts), List.copyOf(violations));
        }
        return new PreconditionReport("READY", List.copyOf(missingContracts), List.copyOf(violations));
    }

    private void requireExisting(Path root, List<String> paths, List<String> missingContracts) {
        for (String path : paths) {
            if (!Files.exists(root.resolve(path))) {
                missingContracts.add(path);
            }
        }
    }

    private void validateSingleCompose(Path root, List<String> violations) {
        try (Stream<Path> stream = Files.walk(root, 4)) {
            List<String> composeFiles = stream
                    .filter(Files::isRegularFile)
                    .map(root::relativize)
                    .map(Path::toString)
                    .filter(this::isComposeFile)
                    .filter(path -> !path.startsWith(".git/"))
                    .sorted()
                    .toList();
            if (!composeFiles.equals(List.of("infra/docker-compose.yml"))) {
                violations.add("두 번째 Docker Compose 또는 잘못된 compose 위치 감지: " + composeFiles);
            }
        } catch (IOException exception) {
            violations.add("compose gate 확인 실패: " + exception.getMessage());
        }
    }

    private boolean isComposeFile(String path) {
        String normalized = path.replace('\\', '/').toLowerCase(Locale.ROOT);
        return normalized.endsWith("docker-compose.yml") || normalized.endsWith("docker-compose.yaml") || normalized.equals("compose.yml") || normalized.equals("compose.yaml");
    }

    private void validateComposeContract(Path composeFile, List<String> missingContracts, List<String> violations) {
        if (!Files.exists(composeFile)) {
            missingContracts.add("infra/docker-compose.yml");
            return;
        }
        try {
            String compose = Files.readString(composeFile);
            requireSnippet(compose, "services:", "compose services root", violations);
            requireSnippet(compose, "  backend:", "backend app service", violations);
            requireSnippet(compose, "  frontend:", "frontend app service", violations);
            requireSnippet(compose, "  database:", "single PostgreSQL database service", violations);
            requireSnippet(compose, "postgres:16", "PostgreSQL 16 image", violations);
            requireSnippet(compose, "context: ../backend", "static backend build context", violations);
            requireSnippet(compose, "context: ../frontend", "static frontend build context", violations);
            requireSnippet(compose, "pull_policy: build", "build-first pull policy", violations);
            if (compose.contains("${BACKEND_CONTEXT") || compose.contains("${FRONTEND_CONTEXT") || compose.contains("5432:5432")) {
                violations.add("compose는 build.context 보간식과 database host port publish를 금지한다.");
            }
        } catch (IOException exception) {
            violations.add("compose 읽기 실패: " + exception.getMessage());
        }
    }

    private void validateMigrationBoundary(Path migrationDirectory, List<String> missingContracts, List<String> violations) {
        if (!Files.isDirectory(migrationDirectory)) {
            missingContracts.add("backend/src/main/resources/db/migration");
            return;
        }
        try (Stream<Path> stream = Files.list(migrationDirectory)) {
            List<String> migrations = stream
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .sorted(Comparator.naturalOrder())
                    .toList();
            boolean hasPostgresBaseline = migrations.stream().anyMatch(name -> name.startsWith("V1__") && name.endsWith(".sql"));
            if (!hasPostgresBaseline) {
                violations.add("기존 PostgreSQL Flyway baseline migration이 필요하다.");
            }
            boolean invalidBasic32Name = migrations.stream()
                    .filter(name -> name.toLowerCase(Locale.ROOT).contains("basic32"))
                    .anyMatch(name -> !BASIC_32_MIGRATION_PATTERN.matcher(name).matches());
            if (invalidBasic32Name) {
                violations.add("BASIC-32 migration은 V21~V26__basic32... 증분 파일명만 허용한다.");
            }
        } catch (IOException exception) {
            violations.add("migration gate 확인 실패: " + exception.getMessage());
        }
    }

    private void requireSnippet(String content, String snippet, String label, List<String> violations) {
        if (!content.contains(snippet)) {
            violations.add(label + " 누락");
        }
    }

    public record PreconditionReport(String status, List<String> missingContracts, List<String> violations) {
        public PreconditionReport {
            Objects.requireNonNull(status, "status");
            missingContracts = List.copyOf(missingContracts);
            violations = List.copyOf(violations);
        }

        public boolean ready() {
            return "READY".equals(status);
        }
    }
}
