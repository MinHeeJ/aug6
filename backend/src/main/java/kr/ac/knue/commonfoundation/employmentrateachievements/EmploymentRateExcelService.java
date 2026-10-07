package kr.ac.knue.commonfoundation.employmentrateachievements;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import kr.ac.knue.commonfoundation.excel.ExcelUploadErrorRow;
import kr.ac.knue.commonfoundation.excel.ExcelUploadResult;
import kr.ac.knue.commonfoundation.storage.FileStoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

/** Whole-file XLSX workflow; domain commits and independently retained diagnostic transactions are explicit. */
@Service
public class EmploymentRateExcelService {
    private static final Logger log = LoggerFactory.getLogger(EmploymentRateExcelService.class);
    private final EmploymentRateAchievementMapper mapper;
    private final EmploymentRateAchievementService achievements;
    private final FileStoragePort storage;
    private final TransactionTemplate domain;
    private final TransactionTemplate diagnostics;

    public EmploymentRateExcelService(
            EmploymentRateAchievementMapper mapper,
            EmploymentRateAchievementService achievements,
            FileStoragePort storage,
            PlatformTransactionManager transactions) {
        this.mapper = mapper;
        this.achievements = achievements;
        this.storage = storage;
        this.domain = new TransactionTemplate(transactions);
        this.diagnostics = new TransactionTemplate(transactions);
        this.diagnostics.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Validates all rows twice around the atomic transaction; any failure leaves the ledger unchanged. */
    public ExcelUploadResult upload(MultipartFile file, CurrentUser user, String requestId) {
        EmploymentRateAchievementService.requireRole(user, "R07");
        if (file == null || file.isEmpty() || file.getSize() > 10 * 1024 * 1024) {
            EmploymentRateAchievementService.invalid("file", "10MB 이하의 XLSX 파일을 선택하세요.");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            EmploymentRateAchievementService.invalid("file", "파일을 읽지 못했습니다.");
            throw new IllegalStateException(exception);
        }
        String filename = file.getOriginalFilename() == null ? "upload.xlsx"
                : file.getOriginalFilename().replaceAll("[\\\\/\\r\\n]", "_");
        filename = filename.substring(0, Math.min(filename.length(), 255));
        String uploadId = "ERA-UP-" + UUID.randomUUID();
        List<List<String>> source = new ArrayList<>();
        List<ValidationError> errors = new ArrayList<>();
        List<Command> commands = new ArrayList<>();
        try {
            if (!filename.toLowerCase(java.util.Locale.ROOT).endsWith(".xlsx")) {
                throw new IllegalArgumentException(".xlsx 형식만 지원합니다. .xls와 CSV는 지원하지 않습니다.");
            }
            source.addAll(EmploymentRateXlsxCodec.read(bytes));
            List<String> columns = mapper.templateColumns();
            if (columns.size() != 5 || !source.get(0).equals(columns)) {
                throw new IllegalArgumentException("현재 업무 양식의 열 이름과 순서가 일치해야 합니다.");
            }
            if (source.size() < 2) {
                throw new IllegalArgumentException("반영할 행이 없습니다.");
            }
            Set<String> keys = new HashSet<>();
            for (int i = 1; i < source.size(); i++) {
                List<String> values = source.get(i);
                int number = i + 1;
                try {
                    if (values.size() > 5) {
                        throw new IllegalArgumentException("추가 열은 허용하지 않습니다.");
                    }
                    Long teacher = mapper.teacher(value(values, 0));
                    if (teacher == null) {
                        throw new IllegalArgumentException("존재하는 활성 교번이 필요합니다.");
                    }
                    List<String> attachments = value(values, 4).isBlank() ? List.of()
                            : List.of(value(values, 4).split(";", -1));
                    EmploymentRateAchievementRequest body = new EmploymentRateAchievementRequest(
                            value(values, 1), LocalDate.parse(value(values, 2)), value(values, 3), attachments);
                    Map<String, Object> prepared = achievements.prepare(body, teacher, user, null);
                    requireDepartment(prepared, user);
                    achievements.validatePeriods(prepared);
                    achievements.checkDuplicate(prepared);
                    String key = teacher + ":" + prepared.get("evaluationYear") + ":"
                            + body.managementItemCode() + ":" + body.achievementDate();
                    if (!keys.add(key)) {
                        throw new IllegalArgumentException("파일 내부 중복 행입니다.");
                    }
                    commands.add(new Command(number, teacher, body));
                } catch (RuntimeException exception) {
                    errors.add(new ValidationError("row." + number, safeMessage(exception)));
                }
            }
        } catch (IllegalArgumentException exception) {
            errors.add(new ValidationError("file", exception.getMessage()));
        }
        int total = Math.max(0, source.size() - 1);
        String originalToken = storage.save(bytes);
        String errorToken = errors.isEmpty() ? null : saveErrors(errors);
        String retainedFilename = filename;
        String initialError = errorToken;
        try {
            diagnostics.executeWithoutResult(status -> retain(
                    uploadId, retainedFilename, originalToken, initialError, source, errors, 0, user));
        } catch (RuntimeException exception) {
            cleanup(originalToken);
            if (initialError != null) {
                cleanup(initialError);
            }
            throw exception;
        }
        int saved = 0;
        if (errors.isEmpty()) {
            try {
                domain.executeWithoutResult(status -> {
                    // Last-row revalidation precedes the first write; unique conflicts roll back the entire file.
                    List<Map<String, Object>> rows = new ArrayList<>();
                    for (Command command : commands) {
                        Map<String, Object> row = achievements.prepare(command.body(), command.teacher(), user, null);
                        requireDepartment(row, user);
                        achievements.validatePeriods(row);
                        achievements.checkDuplicate(row);
                        row.put("requestId", requestId);
                        rows.add(row);
                    }
                    for (Map<String, Object> row : rows) {
                        achievements.persistNew(row);
                    }
                    finish(uploadId, total, commands.size(), List.of(), null, user);
                    mapper.clearStaging(uploadId);
                });
                saved = commands.size();
            } catch (RuntimeException exception) {
                errors.add(new ValidationError("rows", "반영 중 충돌로 전체 롤백되었습니다. " + safeMessage(exception)));
                errorToken = saveErrors(errors);
                String failureToken = errorToken;
                try {
                    diagnostics.executeWithoutResult(status -> finish(uploadId, total, 0, errors, failureToken, user));
                } catch (RuntimeException retentionFailure) {
                    cleanup(failureToken);
                    log.error("Excel failed-row diagnostic finalization pending for upload {}", uploadId);
                    throw retentionFailure;
                }
            }
        }
        String retainedError = errorToken;
        if (!errors.isEmpty()) {
            List<ValidationError> fields = new ArrayList<>(errors);
            fields.add(new ValidationError("uploadId", uploadId));
            fields.add(new ValidationError("errorFileToken", retainedError));
            fields.add(new ValidationError("savedCount", "0"));
            throw new BusinessValidationException("오류 또는 중복이 있어 전체 0건 반영했습니다.", fields);
        }
        return new ExcelUploadResult(uploadId, "EMPLOYMENT_RATE_ACHIEVEMENT", filename, "COMMITTED",
                total, saved, 0, 0, saved, List.of());
    }

    private void retain(
            String uploadId, String filename, String token, String errorToken,
            List<List<String>> rows, List<ValidationError> errors, int saved, CurrentUser user) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("uploadId", uploadId);
        entry.put("templateId", mapper.templateId());
        entry.put("fileToken", token);
        entry.put("filename", filename);
        entry.put("status", errors.isEmpty() ? "VALIDATED" : "REJECTED");
        entry.put("actorId", user.userId());
        mapper.upload(entry);
        Set<String> invalidRows = new HashSet<>();
        for (ValidationError error : errors) {
            invalidRows.add(error.field());
        }
        for (int i = 1; i < rows.size(); i++) {
            entry.put("id", "ERA-STG-" + UUID.randomUUID());
            entry.put("rowNumber", i + 1);
            entry.put("payload", achievements.serialize(Map.of("values", rows.get(i))));
            entry.put("status", invalidRows.contains("row." + (i + 1)) ? "ERROR" : "NORMAL");
            mapper.staging(entry);
        }
        for (ValidationError error : errors) {
            entry.put("id", "ERA-ERR-" + UUID.randomUUID());
            entry.put("rowNumber", error.field().startsWith("row.")
                    ? Integer.valueOf(error.field().substring(4)) : 0);
            entry.put("field", error.field());
            entry.put("message", error.message().substring(0, Math.min(500, error.message().length())));
            mapper.uploadError(entry);
        }
        int total = Math.max(0, rows.size() - 1);
        entry.put("total", total);
        entry.put("success", errors.isEmpty() ? saved : Math.max(0, total - errors.size()));
        entry.put("errors", errors.size());
        entry.put("saved", saved);
        mapper.history(entry);
        if (errorToken != null) {
            entry.put("downloadId", "ERA-ERROR-" + UUID.randomUUID());
            entry.put("errorToken", errorToken);
            entry.put("condition", achievements.serialize(Map.of("uploadId", uploadId)));
            mapper.errorFile(entry);
        }
    }

    private void requireDepartment(Map<String, Object> row, CurrentUser user) {
        if (!EmploymentRateAchievementService.isAdmin(user)
                && mapper.departmentScope(user.userId(), row.get("organizationCode").toString()) == 0) {
            throw new ForbiddenException();
        }
    }

    private String saveErrors(List<ValidationError> errors) {
        List<List<String>> report = new ArrayList<>();
        report.add(List.of("행/필드", "오류 사유"));
        for (ValidationError error : errors) {
            report.add(List.of(error.field(), error.message()));
        }
        return storage.save(EmploymentRateXlsxCodec.write(report));
    }

    private void finish(String uploadId, int total, int saved, List<ValidationError> errors,
            String errorToken, CurrentUser user) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("uploadId", uploadId);
        entry.put("actorId", user.userId());
        entry.put("status", errors.isEmpty() ? "COMMITTED" : "REJECTED");
        entry.put("total", total);
        entry.put("success", errors.isEmpty() ? saved : Math.max(0, total - errors.size()));
        entry.put("errors", errors.size());
        entry.put("saved", saved);
        mapper.uploadStatus(entry);
        mapper.history(entry);
        for (ValidationError error : errors) {
            entry.put("id", "ERA-ERR-" + UUID.randomUUID());
            entry.put("rowNumber", 0);
            entry.put("field", error.field());
            entry.put("message", error.message().substring(0, Math.min(500, error.message().length())));
            mapper.uploadError(entry);
        }
        if (errorToken != null) {
            entry.put("downloadId", "ERA-ERROR-" + UUID.randomUUID());
            entry.put("errorToken", errorToken);
            entry.put("condition", achievements.serialize(Map.of("uploadId", uploadId)));
            mapper.errorFile(entry);
        }
    }

    private void cleanup(String token) {
        try {
            storage.delete(token);
        } catch (RuntimeException exception) {
            log.warn("Orphan cleanup pending for opaque token {}", token);
        }
    }

    private static String safeMessage(RuntimeException exception) {
        if (exception instanceof BusinessValidationException validation) {
            return validation.fields().stream().map(ValidationError::message)
                    .collect(java.util.stream.Collectors.joining("; "));
        }
        if (exception instanceof ForbiddenException) {
            return "담당 부서 범위 밖의 교원입니다.";
        }
        if (exception instanceof ConflictException || exception instanceof IllegalArgumentException) {
            return exception.getMessage() == null ? "입력값을 확인하세요." : exception.getMessage();
        }
        return "서버 검증 중 오류가 발생했습니다. 관리자에게 문의하세요.";
    }

    private static String value(List<String> row, int index) {
        return index >= row.size() ? "" : row.get(index).trim();
    }

    private record Command(int rowNumber, Long teacher, EmploymentRateAchievementRequest body) {
    }
}
