package kr.ac.knue.commonfoundation.employmentrateachievements;

import static kr.ac.knue.commonfoundation.employmentrateachievements.EmploymentRateExcelModels.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import kr.ac.knue.commonfoundation.excel.ExcelDownloadFile;
import kr.ac.knue.commonfoundation.excel.FileStoragePort;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionEvaluateRequest;
import kr.ac.knue.commonfoundation.functionpermissions.FunctionPermissionService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Owner-scoped XLSX validation and atomic FR-032 materialization for R07 operators. */
@Service
public class EmploymentRateExcelService {
    private static final List<String> COMMON_COLUMNS = List.of(
            "교번", "관리항목코드", "업적발생일", "실적명", "첨부참조");
    private final EmploymentRateExcelRepository repository;
    private final EmploymentRateWorkbook workbook;
    private final EducationAchievementGuardMapper guards;
    private final FileStoragePort storage;
    private final ObjectMapper json;
    private final FunctionPermissionService permissions;

    public EmploymentRateExcelService(EmploymentRateExcelRepository repository,
            EmploymentRateWorkbook workbook, EducationAchievementGuardMapper guards,
            FileStoragePort storage, ObjectMapper json, FunctionPermissionService permissions) {
        this.repository = repository;
        this.workbook = workbook;
        this.guards = guards;
        this.storage = storage;
        this.json = json;
        this.permissions = permissions;
    }

    /** Generates the current persisted template without exposing storage paths. */
    @Transactional(readOnly = true)
    public ExcelDownloadFile downloadExcelTemplate(CurrentUser user) {
        authorize(user, "READ");
        Template template = template();
        return new ExcelDownloadFile("employment-rate-template.xlsx", EmploymentRateWorkbook.MIME,
                workbook.template(template.templateId(), template.version(), template.columns()));
    }

    /** Persists staging and diagnostics, but creates no achievements. */
    @Transactional
    public UploadResult validateExcelUpload(MultipartFile file, CurrentUser user) {
        authorize(user, "EXECUTE");
        long started = System.nanoTime();
        if (file == null || file.isEmpty() || file.getOriginalFilename() == null
                || !file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw invalid("file", "현행 XLSX 양식을 선택하세요.");
        }
        Template template = template();
        byte[] bytes;
        try { bytes = file.getBytes(); }
        catch (java.io.IOException ex) { throw invalid("file", "파일을 읽지 못했습니다."); }
        List<InputRow> rows = workbook.read(bytes, template.templateId(), template.version(), template.columns());
        Validation validation = validate(rows, user);
        String id = "ER-UP-" + UUID.randomUUID();
        String filename = filename(file.getOriginalFilename());
        String status = validation.errors().isEmpty() ? "VALIDATED" : "REJECTED";
        String token = storage.save(bytes, user.userId());
        repository.insertUpload(id, template, token, filename, user.userId(), status);
        Set<Integer> invalidRows = new HashSet<>();
        for (ErrorRow error : validation.errors()) {
            invalidRows.add(error.rowNumber());
            repository.error(id, error);
        }
        for (int index = 0; index < rows.size(); index++) {
            InputRow row = rows.get(index);
            repository.stage(id, row, serialize(new Staged(row, validation.targets().get(index), template.version())),
                    invalidRows.contains(row.rowNumber()) ? "ERROR" : "NORMAL");
        }
        int success = rows.size() - invalidRows.size();
        repository.history(id, rows.size(), success, invalidRows.size(), 0, elapsed(started), user.userId());
        String errorUrl = null;
        if (!validation.errors().isEmpty()) {
            String errorToken = storage.save(workbook.errors(validation.errors()), user.userId());
            repository.persistErrorFile(id, errorToken, user.userId());
            errorUrl = "/api/business/employment-rate-achievements/excel-uploads/" + id + "/errors/download";
        }
        return new UploadResult(id, filename, status, rows.size(), success, invalidRows.size(),
                0, 0, validation.errors(), validation.warnings(), errorUrl);
    }

    /** Locks the upload, rechecks every target and writes ledger/status/change/history atomically. */
    @Transactional
    public CommitResult commitExcelUpload(String uploadId, CurrentUser user, String requestId) {
        authorize(user, "EXECUTE");
        long started = System.nanoTime();
        Upload upload = repository.ownedUpload(uploadId, user.userId(), true);
        if (!"VALIDATED".equals(upload.validationStatus()) || !repository.errors(uploadId).isEmpty()) {
            throw new ConflictException("UPLOAD_NOT_COMMITTABLE: 오류 또는 이미 반영된 업로드입니다.");
        }
        repository.lockCommitGuards();
        Template current = template();
        if (!current.templateId().equals(upload.templateId())) {
            throw new ConflictException("STALE_TEMPLATE: 현행 양식으로 다시 업로드하세요.");
        }
        if (!repository.stagingComplete(uploadId)) {
            throw new ConflictException("INCOMPLETE_STAGING: 모든 검증 행을 다시 업로드하세요.");
        }
        List<Staged> staged = repository.staged(uploadId).stream().map(this::deserialize).toList();
        if (staged.isEmpty()) throw new ConflictException("EMPTY_STAGING: 검증 행이 없습니다.");
        if (staged.stream().anyMatch(row -> !validStaged(row))) {
            throw new ConflictException("INVALID_STAGING: 다시 업로드하세요.");
        }
        if (staged.stream().anyMatch(row -> !current.version().equals(row.templateVersion()))) {
            throw new ConflictException("STALE_TEMPLATE: 현행 양식으로 다시 업로드하세요.");
        }
        Validation validation = validate(staged.stream().map(Staged::row).toList(), user);
        if (!validation.errors().isEmpty()) {
            throw new ConflictException(validation.errors().get(0).errorCode()
                    + ": " + validation.errors().get(0).errorReason());
        }
        for (int index = 0; index < staged.size(); index++) {
            if (!validation.targets().get(index).equals(staged.get(index).target())) {
                throw new ConflictException("TARGET_CHANGED: 대상 교원 또는 소속이 변경되었습니다.");
            }
        }
        try {
            for (int index = 0; index < staged.size(); index++) {
                InputRow row = staged.get(index).row();
                repository.insertAchievement(validation.targets().get(index), row,
                        LocalDate.parse(row.achievementDate()),
                        serialize(row.attachmentRef().isBlank() ? List.of() : List.of(row.attachmentRef())),
                        user.userId(), requestId);
            }
        } catch (DuplicateKeyException ex) {
            throw new ConflictException("DUPLICATE: 동시 반영된 중복 실적이 있습니다.");
        }
        repository.committed(uploadId);
        repository.history(uploadId, staged.size(), staged.size(), 0, staged.size(),
                elapsed(started), user.userId());
        return new CommitResult(uploadId, staged.size(), validation.warnings());
    }

    /** Lists only the current operator's uploads using the existing list response shape. */
    @Transactional(readOnly = true)
    public List<HistoryRow> listExcelHistories(CurrentUser user) {
        authorize(user, "READ");
        return repository.histories(user.userId());
    }

    /** Reads durable validation diagnostics after checking upload ownership. */
    @Transactional(readOnly = true)
    public List<ErrorRow> listExcelErrors(String uploadId, CurrentUser user) {
        authorize(user, "READ");
        repository.ownedUpload(uploadId, user.userId(), false);
        return repository.errors(uploadId);
    }

    /** Downloads the owner's retained error workbook through opaque storage references. */
    @Transactional(readOnly = true)
    public ExcelDownloadFile downloadExcelErrors(String uploadId, CurrentUser user) {
        authorize(user, "READ");
        repository.ownedUpload(uploadId, user.userId(), false);
        return new ExcelDownloadFile("employment-rate-errors.xlsx", EmploymentRateWorkbook.MIME,
                storage.readOwned(repository.errorFile(uploadId, user.userId()), user.userId()));
    }

    private Validation validate(List<InputRow> rows, CurrentUser user) {
        List<ErrorRow> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<Target> targets = new ArrayList<>();
        Set<List<String>> identities = new HashSet<>();
        for (InputRow row : rows) {
            List<Target> matches = repository.targets(row.employeeNo());
            Target target = matches.size() == 1 ? matches.get(0) : null;
            targets.add(target);
            if (target == null) error(errors, row, "교번", row.employeeNo(), "INVALID_TARGET", "교번 또는 활성 소속을 확인하세요.");
            else if (!repository.inScope(user.userId(), target)) throw new ForbiddenException();
            LocalDate date = null;
            try {
                date = LocalDate.parse(row.achievementDate());
                if (date.getYear() < 1000 || date.getYear() > 9999) throw new IllegalArgumentException();
            } catch (Exception ex) {
                error(errors, row, "업적발생일", row.achievementDate(), "INVALID_DATE", "YYYY-MM-DD 날짜를 입력하세요.");
                date = null;
            }
            String year = date == null ? null : Integer.toString(date.getYear());
            if (target != null && date != null && guards.countActiveInputPeriods(year, target.userId()) == 0) {
                error(errors, row, "업적발생일", row.achievementDate(), "PERIOD_NOT_ACTIVE", "대상 교원의 활성 입력기간이 아닙니다.");
            } else if (target != null && date != null
                    && guards.countEvaluationConfirmations(target.userId(), year) > 0) {
                error(errors, row, "업적발생일", row.achievementDate(), "CONFIRMED_DATA_LOCKED", "평가확정 실적은 변경할 수 없습니다.");
            }
            if (target != null && date != null && guards.countEvaluationDatePeriods(year, target.userId(), date) == 0) {
                warnings.add("행 " + row.rowNumber() + ": 업적발생일이 평가대상기간 밖입니다.");
            }
            List<ItemRule> rules = date == null ? List.of() : repository.itemRules(row.managementItemCode(), year);
            if ((date != null && rules.size() != 1) || row.managementItemCode().isBlank()
                    || row.managementItemCode().length() > 50) {
                error(errors, row, "관리항목코드", row.managementItemCode(), "INVALID_ITEM", "활성 취업률 관리항목이 없거나 모호합니다.");
            } else if (date != null && !"Y".equals(rules.get(0).editableYn())) {
                error(errors, row, "관리항목코드", row.managementItemCode(), "ITEM_NOT_EDITABLE", "입력 가능한 관리항목이 아닙니다.");
            } else if (date != null && target != null && !validValue(rules.get(0), row, target)) {
                error(errors, row, "실적명", row.achievementName(), "INVALID_VALUE", "관리항목의 필수값과 자료형을 확인하세요.");
            }
            if (row.achievementName().length() > 500) {
                error(errors, row, "실적명", row.achievementName(), "VALUE_TOO_LONG", "실적명은 500자 이하입니다.");
            }
            if (target != null && !row.attachmentRef().isBlank()
                    && !storage.isOwned(row.attachmentRef(), target.userId())) {
                error(errors, row, "첨부참조", row.attachmentRef(), "UNOWNED_ATTACHMENT", "대상 교원 소유의 첨부참조를 입력하세요.");
            }
            if (target == null || date == null) continue;
            List<String> identity = List.of(Long.toString(target.userId()), row.managementItemCode(),
                    date.toString(), row.achievementName());
            if (!identities.add(identity) || repository.duplicate(target, row, date)) {
                error(errors, row, "실적명", row.achievementName(), "DUPLICATE", "중복 실적은 자동 갱신하지 않습니다.");
            }
        }
        return new Validation(errors, warnings, targets);
    }

    private boolean validValue(ItemRule rule, InputRow row, Target target) {
        String value = row.achievementName();
        if (value.isBlank()) return !"Y".equals(rule.requiredYn());
        try {
            return switch (rule.dataType()) {
                case "TEXT" -> true;
                case "NUMBER" -> { new java.math.BigDecimal(value); yield true; }
                case "DATE" -> { LocalDate.parse(value); yield true; }
                case "BOOLEAN" -> Set.of("true", "false", "Y", "N").contains(value);
                case "FILE" -> storage.isOwned(value, target.userId());
                // No code-group relationship is defined for this template: fail closed.
                default -> false;
            };
        } catch (RuntimeException ex) { return false; }
    }

    private void error(List<ErrorRow> errors, InputRow row, String column, String value, String code, String reason) {
        // V15 permits exactly one diagnostic per row/column; preserve the first guard failure.
        if (errors.stream().noneMatch(e -> e.rowNumber() == row.rowNumber() && e.columnName().equals(column))) {
            errors.add(new ErrorRow(row.rowNumber(), column, value, code, reason, "현행 양식과 입력값을 확인하여 다시 업로드하세요."));
        }
    }

    private void authorize(CurrentUser user, String function) {
        if (user == null) throw new UnauthenticatedException();
        if (user.roles() == null || !user.roles().contains("R07")) throw new ForbiddenException();
        permissions.evaluate(new FunctionPermissionEvaluateRequest(
                "SCR-EMPLOYMENT-RATE-ACHIEVEMENT", "R07", function, "DRAFT", null));
    }

    private Template template() {
        Template template = repository.currentTemplate();
        if (!COMMON_COLUMNS.equals(template.columns())) {
            throw new ConflictException("UNSUPPORTED_TEMPLATE: 승인된 공통열 양식이 아닙니다.");
        }
        return template;
    }

    private String serialize(Object value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException ex) {
            throw new IllegalStateException("Excel payload serialization failed", ex);
        }
    }

    private boolean validStaged(Staged staged) {
        if (staged == null || staged.row() == null || staged.target() == null) return false;
        InputRow row = staged.row();
        return row.rowNumber() >= 2 && row.employeeNo() != null && row.managementItemCode() != null
                && row.achievementDate() != null && row.achievementName() != null
                && row.attachmentRef() != null && staged.target().userId() > 0
                && staged.target().organizationCode() != null;
    }

    private Staged deserialize(String payload) {
        try { return json.readValue(payload, Staged.class); }
        catch (JsonProcessingException ex) { throw new ConflictException("INVALID_STAGING: 다시 업로드하세요."); }
    }

    private BusinessValidationException invalid(String field, String message) {
        return new BusinessValidationException("취업률 업로드 요청이 올바르지 않습니다.", List.of(new ValidationError(field, message)));
    }

    private String filename(String original) {
        String name = original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "");
        return name.length() <= 255 ? name : name.substring(name.length() - 255);
    }

    private long elapsed(long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }
    /** Durable validation snapshot used to reject template or target reassignment at commit. */
    public record Staged(InputRow row, Target target, String templateVersion) { }
    private record Validation(List<ErrorRow> errors, List<String> warnings, List<Target> targets) { }
}
