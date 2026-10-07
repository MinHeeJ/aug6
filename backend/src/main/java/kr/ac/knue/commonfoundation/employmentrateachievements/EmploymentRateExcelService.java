package kr.ac.knue.commonfoundation.employmentrateachievements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardMapper;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import kr.ac.knue.commonfoundation.excel.ExcelBusinessWorkflow;
import kr.ac.knue.commonfoundation.excel.ExcelDownloadFile;
import kr.ac.knue.commonfoundation.excel.ExcelOperationsMapper;
import kr.ac.knue.commonfoundation.excel.ExcelTemplateRow;
import kr.ac.knue.commonfoundation.excel.ExcelTemplateSearchResponse;
import kr.ac.knue.commonfoundation.excel.ExcelUploadCommitResult;
import kr.ac.knue.commonfoundation.excel.ExcelUploadErrorRow;
import kr.ac.knue.commonfoundation.excel.ExcelUploadErrorSearchResponse;
import kr.ac.knue.commonfoundation.excel.ExcelUploadHistorySearchResponse;
import kr.ac.knue.commonfoundation.excel.ExcelUploadResult;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.RequestIds;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Owns real XLSX staging and all-or-nothing source materialization. */
@Service
public class EmploymentRateExcelService implements ExcelBusinessWorkflow {
    public static final List<String> HEADERS = List.of("교번", "관리항목코드", "업적발생일", "실적명", "첨부참조");
    private static final String BUSINESS_TYPE = "EMPLOYMENT_RATE_ACHIEVEMENT";
    private final ExcelOperationsMapper common;
    private final EmploymentRateExcelMapper domain;
    private final EducationAchievementGuardMapper guards;
    private final ObjectMapper json;
    private final EmploymentRateAchievementMapper achievements;
    private final EmploymentRateAchievementService authorization;

    public EmploymentRateExcelService(
            ExcelOperationsMapper common,
            EmploymentRateExcelMapper domain,
            EducationAchievementGuardMapper guards,
            ObjectMapper json,
            EmploymentRateAchievementMapper achievements,
            EmploymentRateAchievementService authorization) {
        this.common = common;
        this.domain = domain;
        this.guards = guards;
        this.json = json;
        this.achievements = achievements;
        this.authorization = authorization;
    }

    /**
     * R09 may validate all targets; R07 requires mapped certification scope, including self.
     * Row errors return ERROR/savedCount=0; malformed files/layouts retain diagnostics and raise 400.
     * A successful result is validation only (savedCount=0).
     * Commit revalidates every row and materializes atomically.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = BusinessValidationException.class)
    public ExcelUploadResult upload(MultipartFile file, CurrentUser user, String requestId) {
        requireUploader(user);
        authorization.authorizeUpload(user);
        if (file == null || file.isEmpty() || file.getSize() > 10 * 1024 * 1024
                || file.getOriginalFilename() == null
                || !file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw invalid(List.of(new ValidationError("file", "10MB 이하의 실제 XLSX 파일을 선택하세요.")));
        }
        if (requestId == null || requestId.isBlank() || requestId.length() > 100) {
            throw invalid(List.of(new ValidationError("requestId", "100자 이하 요청 식별자가 필요합니다.")));
        }
        long started = System.nanoTime();
        String uploadId = "ER-UP-" + UUID.randomUUID();
        String filename = filename(file.getOriginalFilename());
        List<ExcelUploadErrorRow> errors = new ArrayList<>();
        List<EmploymentRateWorkbook.Row> parsed;
        try {
            parsed = EmploymentRateWorkbook.read(file.getBytes());
        } catch (IOException | IllegalArgumentException exception) {
            errors.add(error(uploadId, 1, "file", "", "INVALID_WORKBOOK", "XLSX 구조와 셀 값을 확인하세요."));
            persist(uploadId, null, filename, user, List.of(), errors, started);
            throw rejected(uploadId, errors);
        }
        String templateId = domain.findTemplateId();
        List<kr.ac.knue.commonfoundation.excel.ExcelTemplateRuleRow> rules = templateId == null
                ? List.of() : common.listTemplateRules(templateId);
        List<String> expectedRefs = List.of("users.employee_no",
                "evaluation_element_management_item_settings.management_item_code",
                "ISO_DATE", "OPTIONAL_TEXT", "OPTIONAL_OWNED_FILE_TOKEN");
        boolean supportedTemplate = rules != null && rules.size() == HEADERS.size();
        if (supportedTemplate) {
            for (int index = 0; index < rules.size(); index++) {
                var rule = rules.get(index);
                supportedTemplate &= Integer.valueOf(index + 1).equals(rule.columnOrder())
                        && HEADERS.get(index).equals(rule.requiredColumn())
                        && expectedRefs.get(index).equals(rule.codeRuleRef());
            }
        }
        boolean validLayout = supportedTemplate && !parsed.isEmpty() && parsed.get(0).number() == 1
                && normalizedHeader(parsed.get(0).cells()).equals(HEADERS);
        if (templateId == null || !validLayout) {
            errors.add(error(uploadId, 1, "file", "", "INVALID_TEMPLATE",
                    "활성 v1.0 양식의 교번·관리항목코드·업적발생일·실적명·첨부참조 열을 사용하세요."));
        }
        List<StagedRow> staged = new ArrayList<>();
        Set<List<String>> identities = new HashSet<>();
        List<EmploymentRateWorkbook.Row> data = validLayout ? parsed.subList(1, parsed.size()) : parsed;
        if (data.isEmpty() && errors.isEmpty()) {
            errors.add(error(uploadId, 1, "file", "", "EMPTY_WORKBOOK", "실적 행을 한 개 이상 입력하세요."));
        }
        for (EmploymentRateWorkbook.Row row : data) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("rawCells", row.cells());
            payload.put("requestId", requestId);
            payload.put("uploaderUserId", user.userId());
            payload.put("templateVersion", "v1.0");
            if (validLayout && templateId != null) validateRow(uploadId, row, user, payload, errors, identities);
            else payload.put("invalidTemplate", true);
            staged.add(new StagedRow(row.number(), serialize(payload)));
        }
        persist(uploadId, templateId, filename, user, staged, errors, started);
        if (!validLayout) throw rejected(uploadId, errors);
        int failed = (int) staged.stream().filter(row -> errors.stream()
                .anyMatch(error -> error.rowNumber() == 1 || error.rowNumber() == row.number())).count();
        return new ExcelUploadResult(uploadId, BUSINESS_TYPE, filename, errors.isEmpty() ? "VALIDATED" : "ERROR",
                staged.size(), staged.size() - failed, failed, 0, 0, errors);
    }

    private void validateRow(
            String uploadId,
            EmploymentRateWorkbook.Row row,
            CurrentUser user,
            Map<String, Object> payload,
            List<ExcelUploadErrorRow> errors,
            Set<List<String>> identities) {
        String employee = value(row, 0);
        String item = value(row, 1);
        String dateText = value(row, 2);
        String name = value(row, 3);
        String attachment = value(row, 4);
        payload.put("employeeNo", employee);
        payload.put("managementItemCode", item);
        payload.put("achievementDate", dateText);
        payload.put("achievementName", name);
        payload.put("attachmentRefs", List.of());
        if (row.cells().size() > HEADERS.size()) {
            errors.add(error(uploadId, row.number(), "file", "", "INVALID_COLUMNS", "양식에 없는 열을 제거하세요."));
        }
        Long teacher = employee.isBlank() ? null : domain.findActiveTeacher(employee);
        boolean scoped = teacher != null && (user.roles().contains("R09")
                || guards.countCertificationScope(user.userId(), teacher) > 0);
        if (teacher == null) {
            errors.add(error(uploadId, row.number(), "교번", employee, "INVALID_TEACHER", "활성 교번을 확인하세요."));
        } else if (!scoped) {
            errors.add(error(uploadId, row.number(), "교번", employee, "OUT_OF_SCOPE", "허용된 조직의 대상자만 업로드하세요."));
        } else payload.put("teacherUserId", teacher);
        if (item.isBlank() || item.length() > 50 || domain.countActiveItems(item) != 1) {
            errors.add(error(uploadId, row.number(), "관리항목코드", item, "INVALID_MANAGEMENT_ITEM",
                    "유일한 활성 교육영역 관리항목코드를 입력하세요."));
        }
        LocalDate date = null;
        try {
            date = LocalDate.parse(dateText);
            if (!dateText.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}") || date.getYear() < 1) {
                throw new DateTimeParseException("Invalid year", dateText, 0);
            }
        } catch (DateTimeParseException exception) {
            errors.add(error(uploadId, row.number(), "업적발생일", dateText, "INVALID_DATE", "YYYY-MM-DD 날짜를 입력하세요."));
            date = null;
        }
        // No multipart evaluationYear is defined. This template derives it from the occurred date.
        if (date != null) {
            String year = dateText.substring(0, 4);
            payload.put("evaluationYear", year);
            if (scoped) {
                if (guards.countActiveInputPeriods(year, teacher) == 0) {
                    errors.add(error(uploadId, row.number(), "입력기간", year,
                            "PERIOD_NOT_ACTIVE", "활성 교육영역 입력기간을 확인하세요."));
                }
                if (guards.countEvaluationConfirmations(teacher, year) > 0) {
                    errors.add(error(uploadId, row.number(), "평가상태", year,
                            "CONFIRMED_DATA_LOCKED", "평가확정 자료는 반영할 수 없습니다."));
                }
                payload.put("occurredDateOutsideEvaluationPeriod",
                        guards.countEvaluationDatePeriods(year, teacher, date) == 0);
                List<String> identity = List.of(teacher.toString(), year, item, dateText, name);
                if (!identities.add(identity)
                        || domain.countDuplicates(teacher, year, item, dateText, name) > 0) {
                    errors.add(error(uploadId, row.number(), "중복", name, "DUPLICATE", "중복 실적은 자동 갱신하지 않습니다."));
                }
            }
        }
        // No real owned attachment-token resolver exists in this checkout. Never accept unverified references.
        if (!attachment.isBlank()) {
            errors.add(error(uploadId, row.number(), "첨부참조", attachment, "ATTACHMENT_NOT_VERIFIED",
                    "첨부 소유권 검증 어댑터가 없어 참조를 반영할 수 없습니다. 빈 값으로 업로드하세요."));
        }
    }

    private void persist(
            String uploadId,
            String templateId,
            String filename,
            CurrentUser user,
            List<StagedRow> staged,
            List<ExcelUploadErrorRow> errors,
            long started) {
        Set<Integer> invalidRows = new HashSet<>();
        boolean invalidFile = errors.stream().anyMatch(error -> error.rowNumber() == 1);
        for (ExcelUploadErrorRow error : errors) invalidRows.add(error.rowNumber());
        int failed = invalidFile ? staged.size() : (int) staged.stream()
                .filter(row -> invalidRows.contains(row.number())).count();
        // The token identifies persisted diagnostics/staging, not a fabricated downloadable original file.
        common.insertUploadFile(uploadId, BUSINESS_TYPE, templateId, "diagnostics-" + uploadId,
                filename, user.userId(), errors.isEmpty() ? "VALIDATED" : "REJECTED");
        for (ExcelUploadErrorRow error : errors) common.insertUploadError(error);
        for (StagedRow row : staged) {
            common.insertStagingRow("ER-STG-" + UUID.randomUUID(), uploadId, row.number(), row.payload(),
                    invalidFile || invalidRows.contains(row.number()) ? "ERROR" : "NORMAL");
        }
        common.upsertUploadHistory(uploadId, staged.size(), staged.size() - failed, failed, 0, 0,
                (System.nanoTime() - started) / 1_000_000, user.userId());
    }

    /** Exports already-authorized query rows using the upload template's five columns. Caller owns query scope. */
    public byte[] download(List<Map<String, Object>> rows) {
        List<List<String>> cells = new ArrayList<>();
        cells.add(HEADERS);
        for (Map<String, Object> row : rows) {
            cells.add(List.of(string(row, "employeeNo"), string(row, "managementItemCode"),
                    string(row, "achievementDate"), string(row, "achievementName"), string(row, "attachmentRef")));
        }
        return EmploymentRateWorkbook.write(cells);
    }

    @Override
    public String businessType() { return BUSINESS_TYPE; }

    @Override
    public boolean ownsTemplate(String templateId) {
        ExcelTemplateRow row = common.findUploadTemplate(templateId);
        return row != null && BUSINESS_TYPE.equals(row.businessType());
    }

    @Override
    public boolean ownsUpload(String uploadId) {
        return uploadId != null && domain.findUpload(uploadId, false) != null;
    }

    @Override
    @Transactional(readOnly = true)
    public ExcelTemplateSearchResponse templates(int page, int size, String effectiveDate, CurrentUser user) {
        requireUploader(user);
        int safePage = Math.max(0, page);
        int safeSize = pageSize(size);
        String activeId = domain.findTemplateId();
        ExcelTemplateRow row = activeId == null ? null : common.findUploadTemplate(activeId);
        if (row != null && effectiveDate != null && !effectiveDate.isBlank()
                && row.effectiveDate().isAfter(LocalDate.parse(effectiveDate))) row = null;
        List<ExcelTemplateRow> rows = row == null || safePage > 0 ? List.of()
                : List.of(row.withRules(common.listTemplateRules(activeId)));
        return new ExcelTemplateSearchResponse(rows, safePage, safeSize, row == null ? 0 : 1);
    }

    @Override
    @Transactional(readOnly = true)
    public ExcelDownloadFile template(String templateId, CurrentUser user) {
        requireUploader(user);
        if (templateId == null || !templateId.equals(domain.findTemplateId())) {
            throw new NotFoundException("활성 취업률 양식이 없습니다.");
        }
        return workbook("employment-rate-template.xlsx", List.of(HEADERS));
    }

    /**
     * Locks the upload and staged rows, validates the complete file before any source insert,
     * and commits generated-key audits, source rows and bookkeeping together. Validation conflicts
     * commit diagnostics only; any infrastructure/write failure rolls back all materialization.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = RetainedValidationConflict.class)
    public ExcelUploadCommitResult commit(String uploadId, CurrentUser user, String requestId) {
        requireUploader(user);
        Map<String, Object> upload = ownedUpload(uploadId, user, true);
        if (!"VALIDATED".equals(upload.get("validationStatus"))) {
            throw new ConflictException("검증완료 업로드만 한 번 반영할 수 있습니다.");
        }
        authorization.authorizeUpload(user);
        requestId = RequestIds.normalize(requestId);
        if (requestId.length() > 100) {
            throw invalid(List.of(new ValidationError("requestId", "100자 이하 식별자가 필요합니다.")));
        }
        long started = System.nanoTime();
        List<Map<String, Object>> staging = domain.stagedRows(uploadId);
        if (staging.isEmpty() || common.countUploadErrorsForCommit(uploadId) > 0) {
            throw new ConflictException("반영할 정상 검증자료가 없습니다.");
        }
        List<EmploymentRateWorkbook.Row> rawRows = new ArrayList<>();
        for (Map<String, Object> staged : staging) {
            try {
                Map<String, Object> payload = json.readValue(staged.get("payload").toString(),
                        new TypeReference<Map<String, Object>>() { });
                Object raw = payload.get("rawCells");
                if (!(raw instanceof List<?> cells) || cells.stream().anyMatch(cell -> !(cell instanceof String))) {
                    throw new IllegalStateException("Invalid stored Excel cells");
                }
                rawRows.add(new EmploymentRateWorkbook.Row(((Number) staged.get("rowNumber")).intValue(),
                        cells.stream().map(Object::toString).toList()));
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Invalid stored Excel payload", exception);
            }
        }
        // Serialize competing commits for the same target; deterministic order avoids deadlocks.
        rawRows.stream().map(row -> domain.findActiveTeacher(value(row, 0)))
                .filter(java.util.Objects::nonNull).distinct().sorted().forEach(domain::lockTeacher);
        List<ExcelUploadErrorRow> errors = new ArrayList<>();
        List<Map<String, Object>> materialized = new ArrayList<>();
        Set<List<String>> identities = new HashSet<>();
        for (EmploymentRateWorkbook.Row raw : rawRows) {
            Map<String, Object> row = new LinkedHashMap<>();
            validateRow(uploadId, raw, user, row, errors, identities);
            if (row.containsKey("teacherUserId") && row.containsKey("evaluationYear")) {
                Long teacher = ((Number) row.get("teacherUserId")).longValue();
                List<String> organizations = achievements.organizations(teacher, LocalDate.parse(value(raw, 2)));
                if (organizations.size() != 1) {
                    errors.add(error(uploadId, raw.number(), "조직", "", "INVALID_ORGANIZATION",
                            "발생일에 유효한 단일 조직이 필요합니다."));
                } else row.put("organizationCode", organizations.get(0));
            }
            materialized.add(row);
        }
        if (!errors.isEmpty()) {
            for (ExcelUploadErrorRow error : errors) common.insertUploadError(error);
            int failed = (int) errors.stream().map(ExcelUploadErrorRow::rowNumber).distinct().count();
            domain.reject(uploadId);
            common.upsertUploadHistory(uploadId, staging.size(), staging.size() - failed, failed, 0, 0,
                    (System.nanoTime() - started) / 1_000_000, user.userId());
            throw new RetainedValidationConflict();
        }
        for (Map<String, Object> row : materialized) {
            row.put("managementNo", "ER-" + UUID.randomUUID());
            row.put("achievementType", BUSINESS_TYPE);
            row.put("attachmentRef", "[]");
            row.put("achievementStatus", "DRAFT");
            row.put("deletedYn", "N");
            row.put("updatedBy", user.userId());
            if (achievements.insert(row) != 1 || row.get("achievementId") == null) {
                throw new IllegalStateException("Missing inserted achievement/generated key");
            }
            achievements.statusHistory(row);
            for (String field : List.of("managementNo", "achievementType", "teacherUserId", "organizationCode",
                    "evaluationYear", "managementItemCode", "achievementDate", "achievementName",
                    "attachmentRef", "achievementStatus", "deletedYn")) {
                Map<String, Object> audit = new LinkedHashMap<>();
                audit.put("targetKey", row.get("achievementId").toString());
                audit.put("changeType", "CREATE");
                audit.put("fieldName", field);
                audit.put("beforeValue", null);
                audit.put("afterValue", java.util.Objects.toString(row.get(field), null));
                audit.put("changedBy", user.userId());
                audit.put("requestId", requestId);
                audit.put("changeReason", "취업률 Excel 반영");
                if (achievements.history(audit) != 1) throw new IllegalStateException("Missing achievement audit");
            }
        }
        common.markUploadCommitted(uploadId);
        common.upsertUploadHistory(uploadId, staging.size(), staging.size(), 0, 0, staging.size(),
                (System.nanoTime() - started) / 1_000_000, user.userId());
        common.deleteNormalStagingRows(uploadId);
        return new ExcelUploadCommitResult(uploadId, staging.size());
    }

    @Override
    @Transactional(readOnly = true)
    public ExcelUploadErrorSearchResponse errors(int page, int size, String uploadId, CurrentUser user) {
        ownedUpload(uploadId, user, false);
        int safePage = Math.max(0, page);
        int safeSize = pageSize(size);
        return new ExcelUploadErrorSearchResponse(common.listExcelUploadErrors(uploadId, safeSize, safePage * safeSize),
                safePage, safeSize, common.countExcelUploadErrors(uploadId));
    }

    @Override
    @Transactional(readOnly = true)
    public ExcelDownloadFile errorFile(String uploadId, CurrentUser user) {
        ownedUpload(uploadId, user, false);
        List<List<String>> cells = new ArrayList<>();
        cells.add(List.of("행번호", "열명", "입력값", "오류코드", "오류사유", "수정안내"));
        // Read all errors, not just the first UI page.
        for (ExcelUploadErrorRow row : common.listExcelUploadErrors(uploadId, Integer.MAX_VALUE, 0)) {
            cells.add(List.of(row.rowNumber().toString(), row.columnName(),
                    java.util.Objects.toString(row.inputValue(), ""), row.errorCode(), row.errorReason(),
                    java.util.Objects.toString(row.correctionGuide(), "")));
        }
        return workbook("employment-rate-errors.xlsx", cells);
    }

    @Override
    @Transactional(readOnly = true)
    public ExcelUploadHistorySearchResponse histories(int page, int size, String uploadId,
            String originalFileName, CurrentUser user) {
        requireUploader(user);
        uploadId = blank(uploadId);
        originalFileName = blank(originalFileName);
        if (uploadId != null) ownedUpload(uploadId, user, false);
        int safePage = Math.max(0, page);
        int safeSize = pageSize(size);
        boolean admin = user.roles().contains("R09");
        return new ExcelUploadHistorySearchResponse(
                domain.histories(user.userId(), admin, uploadId, originalFileName, safeSize, safePage * safeSize),
                safePage, safeSize, domain.countHistories(user.userId(), admin, uploadId, originalFileName));
    }

    private Map<String, Object> ownedUpload(String uploadId, CurrentUser user, boolean lock) {
        requireUploader(user);
        Map<String, Object> row = domain.findUpload(uploadId, lock);
        if (row == null) throw new NotFoundException("취업률 업로드가 없습니다.");
        long owner = ((Number) row.get("uploaderUserId")).longValue();
        if (!user.roles().contains("R09") && owner != user.userId()) throw new ForbiddenException();
        return row;
    }

    private static ExcelDownloadFile workbook(String name, List<List<String>> cells) {
        return new ExcelDownloadFile(name, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                EmploymentRateWorkbook.write(cells));
    }

    private static int pageSize(int size) { return Set.of(20, 50, 100).contains(size) ? size : 20; }
    private static String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private static List<String> normalizedHeader(List<String> cells) {
        return cells.stream().map(String::trim).toList();
    }

    private static String string(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null || ("attachmentRef".equals(key) && "[]".equals(value.toString()))) return "";
        return value.toString();
    }

    private static String value(EmploymentRateWorkbook.Row row, int index) {
        return index < row.cells().size() ? row.cells().get(index).trim() : "";
    }

    private String serialize(Map<String, Object> payload) {
        try {
            return json.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            // Infrastructure failures must roll back the transaction, unlike retained validation diagnostics.
            throw new IllegalStateException("Excel staging 직렬화 실패", exception);
        }
    }

    private static ExcelUploadErrorRow error(
            String uploadId, int row, String column, String input, String code, String reason) {
        return new ExcelUploadErrorRow("ER-ERR-" + UUID.randomUUID(), uploadId, row, column, input,
                code, reason, "현행 양식과 기준정보를 확인한 뒤 수정하여 다시 업로드하세요.");
    }

    private static BusinessValidationException rejected(String uploadId, List<ExcelUploadErrorRow> errors) {
        List<ValidationError> fields = new ArrayList<>();
        fields.add(new ValidationError("uploadId", uploadId));
        for (ExcelUploadErrorRow error : errors) {
            fields.add(new ValidationError("rows[" + error.rowNumber() + "]." + error.columnName(),
                    error.errorCode() + ": " + error.errorReason()));
        }
        return invalid(fields);
    }

    private static BusinessValidationException invalid(List<ValidationError> fields) {
        return new BusinessValidationException("취업률 Excel 검증에 실패했습니다. 업무자료는 반영하지 않았습니다.", fields);
    }

    private static void requireUploader(CurrentUser user) {
        if (user == null || user.userId() == null) throw new UnauthenticatedException();
        if (user.roles() == null || (!user.roles().contains("R07") && !user.roles().contains("R09"))) {
            throw new ForbiddenException();
        }
    }

    private static String filename(String original) {
        String clean = original.replace('\\', '/');
        clean = clean.substring(clean.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "");
        return clean.length() <= 255 ? clean : clean.substring(clean.length() - 255);
    }

    private record StagedRow(int number, String payload) { }

    /** Only pre-write validation conflicts are allowed to commit retained diagnostics. */
    public static final class RetainedValidationConflict extends ConflictException {
        public RetainedValidationConflict() {
            super("재검증 오류로 전체 반영을 취소했습니다. 오류자료는 보존됩니다.");
        }
    }
}
