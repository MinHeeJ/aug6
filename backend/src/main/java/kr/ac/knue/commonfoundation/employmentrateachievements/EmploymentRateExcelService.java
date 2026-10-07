package kr.ac.knue.commonfoundation.employmentrateachievements;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import kr.ac.knue.commonfoundation.excel.ExcelDownloadFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

/** D6: validation retains diagnostics independently; confirmed commit revalidates all rows before writing. */
@Service
public class EmploymentRateExcelService {
    public static final List<String> COLUMNS = List.of("교번", "관리항목코드", "업적발생일", "실적명", "첨부참조");
    private static final Logger LOG = LoggerFactory.getLogger(EmploymentRateExcelService.class);
    private final EmploymentRateExcelRepository repository;
    private final EducationAchievementGuardMapper guards;
    private final EmploymentRateAchievementService achievements;
    private final ObjectMapper json;
    private final FileStoragePort storage;
    private final EmploymentRateWorkbookCodec codec = new EmploymentRateWorkbookCodec();
    private final TransactionTemplate transaction;

    /** Both stage and commit own their REQUIRES_NEW boundary, so returned results survive outer rollback. */
    public EmploymentRateExcelService(
            EmploymentRateExcelRepository repository,
            EducationAchievementGuardMapper guards,
            EmploymentRateAchievementService achievements,
            ObjectMapper json,
            FileStoragePort storage,
            PlatformTransactionManager manager) {
        this.repository = repository;
        this.guards = guards;
        this.achievements = achievements;
        this.json = json;
        this.storage = storage;
        transaction = new TransactionTemplate(manager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public record Error(int rowNumber, String columnName, String inputValue, String errorCode,
                        String errorReason, String correctionGuide) { }
    public record UploadResult(String uploadId, String originalFileName, int totalCount, int successCount,
                               int errorCount, int savedCount, List<Error> errors, List<Error> warnings,
                               String errorDownloadUrl) { }
    public record CommitResult(String uploadId, int savedCount, List<Error> warnings) { }
    private record Input(int rowNumber, List<String> cells) { }
    private record Checked(Input input, EmploymentRateExcelRepository.Target target,
                           String year, LocalDate date) { }
    private record Validation(List<Checked> checked, List<Error> errors, List<Error> warnings) { }

    public ExcelDownloadFile downloadExcelTemplate(CurrentUser user) {
        requireRole(user);
        return transaction.execute(status -> {
            var template = repository.currentTemplate();
            return new ExcelDownloadFile("employment-rate-" + template.templateVersion() + ".xlsx",
                    EmploymentRateWorkbookCodec.MIME, codec.write(List.of(metadata(template), template.columns())));
        });
    }

    public UploadResult validateExcelUpload(MultipartFile file, CurrentUser user) {
        return validateExcelUpload(file, user, UUID.randomUUID().toString());
    }

    /** No domain service is invoked here, including on a completely normal upload. */
    public UploadResult validateExcelUpload(MultipartFile file, CurrentUser user, String requestId) {
        requireRole(user);
        if (file == null || file.isEmpty() || file.getSize() > EmploymentRateWorkbookCodec.MAX_INPUT_BYTES
                || file.getOriginalFilename() == null
                || !file.getOriginalFilename().toLowerCase(java.util.Locale.ROOT).endsWith(".xlsx")) {
            throw new BusinessValidationException("XLSX 파일을 선택하세요.",
                    List.of(new ValidationError("file", "16MB 이하의 현행 XLSX 양식이 필요합니다.")));
        }
        final byte[] bytes;
        try (var stream = file.getInputStream()) {
            bytes = stream.readNBytes(EmploymentRateWorkbookCodec.MAX_INPUT_BYTES + 1);
            if (bytes.length > EmploymentRateWorkbookCodec.MAX_INPUT_BYTES) throw new IllegalArgumentException();
        } catch (Exception exception) {
            throw new BusinessValidationException("파일을 읽지 못했습니다.",
                    List.of(new ValidationError("file", "파일 형식과 크기를 확인하세요.")));
        }
        return transaction.execute(status -> stage(bytes, filename(file.getOriginalFilename()), user, requestId));
    }

    private UploadResult stage(byte[] bytes, String filename, CurrentUser user, String requestId) {
        var template = repository.currentTemplate();
        List<Input> inputs = new ArrayList<>();
        List<Error> fileErrors = new ArrayList<>();
        try {
            List<List<String>> grid = codec.read(bytes);
            if (grid.size() < 2 || !grid.get(0).equals(metadata(template)) || !grid.get(1).equals(template.columns())) {
                fileErrors.add(error(1, "file", "", "INVALID_TEMPLATE", "현행 양식 ID·버전·공통열이 다릅니다."));
            } else {
                for (int i = 2; i < grid.size(); i++) {
                    if (grid.get(i).stream().allMatch(String::isBlank)) continue;
                    inputs.add(new Input(i + 1, grid.get(i)));
                }
                if (inputs.isEmpty()) fileErrors.add(error(1, "file", "", "EMPTY_FILE", "등록할 데이터 행이 없습니다."));
            }
        } catch (IllegalArgumentException exception) {
            fileErrors.add(error(1, "file", "", "INVALID_XLSX", "안전한 XLSX 구조와 ISO 문자열 날짜를 확인하세요."));
        }
        Validation validation = validate(inputs, user);
        List<Error> errors = new ArrayList<>(fileErrors);
        errors.addAll(validation.errors());
        Set<Integer> invalid = new HashSet<>();
        validation.errors().forEach(e -> invalid.add(e.rowNumber()));
        String id = "ER-UP-" + UUID.randomUUID();
        String original = saveRetained(user.userId(), "original", bytes, requestId);
        repository.insertUpload(id, template.templateId(), original, filename,
                errors.isEmpty() ? "VALIDATED" : "REJECTED", user.userId());
        for (Checked checked : validation.checked()) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("cells", checked.input().cells());
            payload.put("templateId", template.templateId());
            payload.put("templateVersion", template.templateVersion());
            payload.put("evaluationYear", checked.year());
            if (checked.target() != null) {
                payload.put("teacherUserId", checked.target().userId());
                payload.put("organizationCode", checked.target().organizationCode());
            }
            repository.insertStaging(id, checked.input().rowNumber(), encode(payload),
                    invalid.contains(checked.input().rowNumber()) ? "ERROR" : "NORMAL");
        }
        for (Error error : errors) repository.insertError(id, error);
        int total = inputs.size();
        repository.insertHistory(id, total, total - invalid.size(), invalid.size(), 0, user.userId());
        String download = null;
        if (!errors.isEmpty()) {
            List<List<String>> report = new ArrayList<>();
            report.add(List.of("행", "열", "입력값", "오류코드", "오류사유", "수정안내"));
            for (Error error : errors) {
                report.add(List.of(String.valueOf(error.rowNumber()), error.columnName(), error.inputValue(),
                        error.errorCode(), error.errorReason(), error.correctionGuide()));
            }
            String token = saveRetained(user.userId(), "errors", codec.write(report), requestId);
            repository.insertErrorDownload(id, token, user.userId());
            download = "/api/business/employment-rate-achievements/excel-uploads/" + id + "/errors/download";
        }
        return new UploadResult(id, filename, total, total - invalid.size(), invalid.size(), 0,
                List.copyOf(errors), validation.warnings(), download);
    }

    /** The explicit confirmation must be true; diagnostics and staging survive any failed commit. */
    public CommitResult commitExcelUpload(String uploadId, CurrentUser user, boolean confirmed, String requestId) {
        requireRole(user);
        if (!confirmed) throw new ConflictException("CONFIRMATION_REQUIRED: 검증결과 확인 후 반영을 승인하세요.");
        String id = id(uploadId);
        return transaction.execute(status -> commit(id, user, requestId));
    }

    private CommitResult commit(String id, CurrentUser user, String requestId) {
        repository.lockGuards();
        var upload = owned(id, user, true);
        if (!"VALIDATED".equals(upload.validationStatus()) || upload.savedCount() != 0
                || upload.errorCount() != 0 || !repository.errors(id).isEmpty()) {
            throw new ConflictException("오류 또는 이미 반영된 업로드는 전체 반영할 수 없습니다.");
        }
        var template = repository.currentTemplate();
        if (!template.templateId().equals(upload.templateId())) throw new ConflictException("INVALID_TEMPLATE: 현행 양식이 변경되었습니다.");
        List<EmploymentRateExcelRepository.Staged> staged = repository.staging(id);
        if (staged.size() != upload.totalCount() || staged.isEmpty()
                || upload.successCount() != staged.size()
                || staged.stream().anyMatch(s -> !"NORMAL".equals(s.validationStatus()))) {
            throw new ConflictException("INCOMPLETE_STAGING: 검증된 전체 행이 아닙니다.");
        }
        List<Input> inputs = new ArrayList<>();
        List<JsonNode> snapshots = new ArrayList<>();
        Set<Integer> rowNumbers = new HashSet<>();
        for (var row : staged) {
            JsonNode payload = decode(row.payload());
            JsonNode cells = payload.path("cells");
            if (row.rowNumber() < 3 || row.rowNumber() > EmploymentRateWorkbookCodec.MAX_ROWS
                    || !rowNumbers.add(row.rowNumber()) || !cells.isArray() || cells.size() > COLUMNS.size()
                    || !template.templateId().equals(payload.path("templateId").asText())
                    || !template.templateVersion().equals(payload.path("templateVersion").asText())) {
                throw new ConflictException("INVALID_STAGING: 양식 또는 원본 행 구조가 변경되었습니다.");
            }
            List<String> values = new ArrayList<>();
            for (JsonNode cell : cells) {
                if (!cell.isTextual()) throw new ConflictException("INVALID_STAGING: 입력값은 문자열이어야 합니다.");
                values.add(cell.asText());
            }
            inputs.add(new Input(row.rowNumber(), List.copyOf(values)));
            snapshots.add(payload);
        }
        Validation validation = validate(inputs, user);
        if (!validation.errors().isEmpty()) {
            Error first = validation.errors().get(0);
            throw new ConflictException(first.errorCode() + ": 재검증 오류로 전체 반영을 차단했습니다. " + first.errorReason());
        }
        for (int i = 0; i < validation.checked().size(); i++) {
            Checked row = validation.checked().get(i);
            JsonNode snapshot = snapshots.get(i);
            if (row.target().userId() != snapshot.path("teacherUserId").asLong(-1)
                    || !row.target().organizationCode().equals(snapshot.path("organizationCode").asText())
                    || !row.year().equals(snapshot.path("evaluationYear").asText())) {
                throw new ConflictException("TARGET_CHANGED: 대상 또는 평가연도가 변경되었습니다.");
            }
        }
        // All parsing, scope, period, finalization, item, attachment and duplicate checks precede writes.
        for (Checked row : validation.checked()) {
            achievements.createImported(user, new EmploymentRateAchievementRequest(
                    value(row.input(), 1), row.date(), value(row.input(), 3), null,
                    blankToNull(value(row.input(), 4))), row.target().userId(), row.year(), requestId);
        }
        repository.finish(id, user.userId(), staged.size());
        return new CommitResult(id, staged.size(), validation.warnings());
    }

    public List<Error> listExcelErrors(String uploadId, CurrentUser user) {
        requireRole(user);
        return transaction.execute(status -> {
            owned(id(uploadId), user, false);
            return repository.errors(id(uploadId));
        });
    }

    public List<Map<String, Object>> listExcelHistories(CurrentUser user) {
        requireRole(user);
        return transaction.execute(status -> repository.histories(user.userId()));
    }

    /** Downloads actual retained XLSX bytes rather than a synthesized filename or regenerated CSV. */
    public ExcelDownloadFile downloadExcelErrors(String uploadId, CurrentUser user) {
        requireRole(user);
        return transaction.execute(status -> {
            String id = id(uploadId);
            owned(id, user, false);
            String token = repository.errorFileToken(id, user.userId());
            if (token == null) throw new NotFoundException("보존된 오류 파일이 없습니다.");
            return new ExcelDownloadFile("employment-rate-errors.xlsx", EmploymentRateWorkbookCodec.MIME,
                    storage.read(user.userId(), token));
        });
    }

    private Validation validate(List<Input> inputs, CurrentUser user) {
        Map<String, Error> errors = new LinkedHashMap<>();
        List<Error> warnings = new ArrayList<>();
        List<Checked> checked = new ArrayList<>();
        Set<String> duplicates = new HashSet<>();
        for (Input input : inputs) {
            if (input.cells().size() > COLUMNS.size()) {
                add(errors, error(input.rowNumber(), "file", "", "INVALID_COLUMNS", "공통열 외 입력은 허용하지 않습니다."));
            }
            String employee = value(input, 0);
            String code = value(input, 1);
            String dateText = value(input, 2);
            String name = value(input, 3);
            String attachment = value(input, 4);
            var target = employee.isBlank() ? null : repository.findTarget(employee, user.userId());
            if (target == null) add(errors, error(input.rowNumber(), COLUMNS.get(0), employee,
                    "TARGET_OUT_OF_SCOPE", "존재하는 교번과 담당자 허용 소속을 확인하세요."));
            if (code.isBlank() || code.length() > 50) add(errors, error(input.rowNumber(), COLUMNS.get(1), code,
                    "INVALID_ITEM", "관리항목코드(50자 이하)가 필요합니다."));
            if (name.isBlank() || name.length() > 500) add(errors, error(input.rowNumber(), COLUMNS.get(3), name,
                    "INVALID_NAME", "실적명은 필수이며 500자 이하입니다."));
            LocalDate date = null;
            String year = null;
            try {
                if (!dateText.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) throw new IllegalArgumentException();
                date = LocalDate.parse(dateText);
                year = dateText.substring(0, 4);
                if (date.getYear() < 1) throw new IllegalArgumentException();
            } catch (Exception exception) {
                date = null;
                year = null;
                add(errors, error(input.rowNumber(), COLUMNS.get(2), dateText, "INVALID_DATE",
                        "업적발생일은 YYYY-MM-DD 문자열이어야 합니다. Excel 숫자 날짜는 지원하지 않습니다."));
            }
            if (attachment.length() > 300 || !attachment.isBlank()
                    && target != null && !storage.exists(target.userId(), attachment)) {
                add(errors, error(input.rowNumber(), COLUMNS.get(4), attachment, "INVALID_ATTACHMENT",
                        "대상 교원에게 속한 기존 첨부 참조만 사용할 수 있습니다."));
            }
            if (date != null) {
                if (!code.isBlank() && code.length() <= 50 && repository.countItem(code, year, date) != 1) {
                    add(errors, error(input.rowNumber(), COLUMNS.get(1), code, "INVALID_ITEM",
                            "평가연도 FR-032의 활성·확정 관리항목이 유일하게 일치해야 합니다."));
                }
                if (target != null) {
                    if (guards.countActiveInputPeriods(year, target.userId()) == 0) {
                        add(errors, error(input.rowNumber(), COLUMNS.get(2), dateText, "PERIOD_NOT_ACTIVE", "활성 입력기간이 아닙니다."));
                    }
                    if (guards.countEvaluationConfirmations(target.userId(), year) > 0) {
                        add(errors, error(input.rowNumber(), COLUMNS.get(0), employee, "CONFIRMED_DATA_LOCKED", "평가확정된 대상입니다."));
                    }
                    if (guards.countEvaluationDatePeriods(year, target.userId(), date) == 0) {
                        warnings.add(error(input.rowNumber(), COLUMNS.get(2), dateText,
                                "OUTSIDE_EVALUATION_PERIOD", "평가대상 기간 밖이지만 저장을 허용합니다."));
                    }
                    String key = target.userId() + "|" + year + "|" + code + "|" + date;
                    if (!duplicates.add(key) || repository.countDuplicate(target.userId(), year, code, date) > 0) {
                        add(errors, error(input.rowNumber(), COLUMNS.get(1), code, "DUPLICATE", "중복 실적은 자동 갱신하지 않습니다."));
                    }
                }
            }
            checked.add(new Checked(input, target, year, date));
        }
        return new Validation(List.copyOf(checked), List.copyOf(errors.values()), List.copyOf(warnings));
    }

    private String saveRetained(long owner, String purpose, byte[] bytes, String requestId) {
        String token = storage.save(owner, purpose, bytes);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_ROLLED_BACK) return; // Unknown commit outcomes need reconciliation, not deletion.
                try { storage.delete(owner, token); }
                catch (RuntimeException exception) {
                    LOG.error("Employment Excel orphan cleanup failed owner={} token={} requestId={}",
                            owner, token, requestId, exception);
                }
            }
        });
        return token;
    }

    private EmploymentRateExcelRepository.Upload owned(String id, CurrentUser user, boolean lock) {
        var upload = repository.findUpload(id, user.userId(), lock);
        if (upload == null) throw new NotFoundException("본인 업무 업로드를 찾을 수 없습니다.");
        return upload;
    }

    private void requireRole(CurrentUser user) {
        if (user == null || user.userId() == null) throw new UnauthenticatedException();
        if (user.roles() == null || !user.roles().contains("R07")) throw new ForbiddenException();
    }

    private List<String> metadata(EmploymentRateExcelRepository.Template template) {
        return List.of("templateId", template.templateId(), "templateVersion", template.templateVersion());
    }

    private Error error(int row, String column, String value, String code, String reason) {
        return new Error(row, column, value, code, reason, "현행 양식 및 해당 열 입력값을 수정하고 다시 업로드하세요.");
    }

    private void add(Map<String, Error> errors, Error error) {
        errors.putIfAbsent(error.rowNumber() + ":" + error.columnName(), error);
    }

    private String value(Input input, int column) {
        return input.cells().size() > column ? input.cells().get(column).trim() : "";
    }

    private String blankToNull(String value) { return value.isBlank() ? null : value; }
    private String encode(Object value) {
        try { return json.writeValueAsString(value); }
        catch (Exception exception) { throw new IllegalStateException("업로드 행 직렬화 실패", exception); }
    }
    private JsonNode decode(String value) {
        try {
            JsonNode node = json.readTree(value);
            if (node == null || !node.isObject()) throw new IllegalArgumentException();
            return node;
        } catch (Exception exception) { throw new ConflictException("INVALID_STAGING: 보존된 검증행을 읽지 못했습니다."); }
    }
    private String id(String id) {
        if (id == null || id.isBlank() || id.length() > 100) throw new NotFoundException("업로드 ID를 확인하세요.");
        return id.trim();
    }
    private String filename(String name) {
        String safe = name.replace('\\', '/');
        safe = safe.substring(safe.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "");
        return safe.length() > 255 ? safe.substring(0, 250) + ".xlsx" : safe;
    }
}
