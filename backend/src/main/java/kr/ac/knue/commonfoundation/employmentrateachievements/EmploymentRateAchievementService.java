package kr.ac.knue.commonfoundation.employmentrateachievements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementMutationContext;
import kr.ac.knue.commonfoundation.basic81.OccurredDateValidation;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Implements individual employment-rate achievements, atomic upload validation,
 * and the intentionally deferred bulk-job policy boundary for BASIC-83.
 */
@Service
public class EmploymentRateAchievementService {
    private static final Set<String> ACHIEVEMENT_ROLES = Set.of("R01", "R02", "R04");
    private final EmploymentRateAchievementMapper mapper;
    private final EducationAchievementGuardService guardService;
    private final ObjectMapper objectMapper;

    public EmploymentRateAchievementService(
            EmploymentRateAchievementMapper mapper,
            EducationAchievementGuardService guardService,
            ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.guardService = guardService;
        this.objectMapper = objectMapper;
    }

    /** Lists only records visible through the existing education-achievement data scope. */
    @Transactional(readOnly = true)
    public EmploymentRateAchievementSearchResponse list(
            int page,
            int pageSize,
            CurrentUser requester) {
        requireAchievementRole(requester);
        validatePageSize(pageSize);
        int safePage = Math.max(0, page);
        return new EmploymentRateAchievementSearchResponse(
                mapper.list(requester.userId(), requester.roles(), pageSize, safePage * pageSize)
                        .stream()
                        .map(this::row)
                        .toList(),
                safePage,
                pageSize,
                mapper.count(requester.userId(), requester.roles()));
    }

    /** Retrieves one achievement after applying the same role/data-scope rules as the list. */
    @Transactional(readOnly = true)
    public EmploymentRateAchievementRow get(Long achievementId, CurrentUser requester) {
        requireAchievementRole(requester);
        EmploymentRateAchievementRow found = requireFound(achievementId);
        requireReadScope(found, requester);
        return found;
    }

    /** Persists a new DRAFT row and its status/audit records atomically. */
    @Transactional
    public EmploymentRateAchievementSaveResponse create(
            EmploymentRateAchievementRequest request,
            CurrentUser requester,
            String requestId) {
        requireCreateRole(requester);
        validateRequest(request);
        OccurredDateValidation dateValidation = guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        requester.userId(),
                        String.valueOf(Year.from(request.achievementDate())),
                        request.achievementDate()));
        Long achievementId = mapper.insertAchievement(new EmploymentRateAchievementInsert(
                requester.userId(),
                String.valueOf(Year.from(request.achievementDate())),
                request.managementItemCode().trim(),
                request.achievementDate(),
                blankToNull(request.achievementName()),
                attachmentJson(request.attachmentIds()),
                null));
        if (achievementId == null) {
            throw new ConflictException("활성 소속 정보가 없어 취업률 실적을 저장할 수 없습니다.");
        }
        mapper.insertStatusHistory(achievementId, "취업률 실적 최초 입력", requester.userId());
        mapper.insertChangeHistory(
                achievementId,
                "CREATE",
                null,
                serialize(request),
                requester.userId(),
                "취업률 실적 저장",
                requestId);
        return new EmploymentRateAchievementSaveResponse(
                requireFound(achievementId),
                dateValidation.warning(),
                dateValidation.message());
    }

    /** Updates a caller-owned non-finalized row after all shared lifecycle guards pass. */
    @Transactional
    public EmploymentRateAchievementSaveResponse update(
            Long achievementId,
            EmploymentRateAchievementRequest request,
            CurrentUser requester,
            String requestId) {
        requireCreateRole(requester);
        validateRequest(request);
        EmploymentRateAchievementRow existing = requireFound(achievementId);
        if (!requester.userId().equals(existing.teacherUserId())) {
            throw new ForbiddenException();
        }
        if ("EVALUATION_CONFIRMED".equals(existing.achievementStatus())) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 데이터는 수정할 수 없습니다.");
        }
        OccurredDateValidation dateValidation = guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        existing.teacherUserId(),
                        String.valueOf(Year.from(request.achievementDate())),
                        request.achievementDate()));
        int changed = mapper.updateAchievement(new EmploymentRateAchievementUpdate(
                achievementId,
                request.managementItemCode().trim(),
                request.achievementDate(),
                blankToNull(request.achievementName()),
                attachmentJson(request.attachmentIds()),
                requester.userId()));
        if (changed != 1) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 데이터는 수정할 수 없습니다.");
        }
        mapper.insertChangeHistory(
                achievementId,
                "UPDATE",
                serialize(existing),
                serialize(request),
                requester.userId(),
                "취업률 실적 수정",
                requestId);
        return new EmploymentRateAchievementSaveResponse(
                requireFound(achievementId),
                dateValidation.warning(),
                dateValidation.message());
    }

    /** Produces a spreadsheet-compatible CSV export for the caller's permitted result set. */
    @Transactional(readOnly = true)
    public byte[] download(int page, int pageSize, CurrentUser requester) {
        if (requester == null || requester.roles() == null
                || requester.roles().stream().noneMatch(role ->
                ACHIEVEMENT_ROLES.contains(role) || "R07".equals(role))) {
            throw new ForbiddenException();
        }
        validatePageSize(pageSize);
        int safePage = Math.max(0, page);
        List<EmploymentRateAchievementRow> rows = mapper.list(
                requester.userId(), requester.roles(), pageSize, safePage * pageSize)
                .stream()
                .map(this::row)
                .toList();
        StringBuilder csv = new StringBuilder(
                "achievementId,managementItemCode,achievementDate,achievementName,status\n");
        for (EmploymentRateAchievementRow item : rows) {
            csv.append(item.achievementId()).append(',')
                    .append(csv(item.managementItemCode())).append(',')
                    .append(item.achievementDate()).append(',')
                    .append(csv(item.achievementName())).append(',')
                    .append(item.achievementStatus()).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Validates every uploaded CSV row before persisting any domain row. Upload
     * audit/error data is retained even when one or more rows are rejected.
     */
    @Transactional
    public EmploymentRateExcelUploadResult upload(
            MultipartFile file,
            CurrentUser requester,
            String requestId) {
        requireExcelRole(requester);
        if (file == null || file.isEmpty()) {
            throw validation("file", "취업률 실적 CSV 파일을 선택하세요.");
        }
        List<EmploymentRateCsvRow> rows = parseCsv(file);
        List<EmploymentRateExcelErrorRow> errors = validateRows(rows);
        String uploadId = "ER-UP-" + UUID.randomUUID();
        String fileName = safeFilename(file.getOriginalFilename());
        mapper.insertUploadFile(
                uploadId,
                "employment-rate-" + uploadId,
                fileName,
                errors.isEmpty() ? "COMMITTED" : "REJECTED",
                requester.userId());
        for (EmploymentRateExcelErrorRow error : errors) {
            mapper.insertUploadError(
                    "ER-ERR-" + UUID.randomUUID(),
                    uploadId,
                    error.rowNumber(),
                    error.columnName(),
                    error.inputValue(),
                    error.errorCode(),
                    error.errorReason(),
                    "현행 양식과 입력값을 확인하세요.");
        }
        if (errors.isEmpty()) {
            for (EmploymentRateCsvRow csvRow : rows) {
                EmploymentRateAchievementRequest request = new EmploymentRateAchievementRequest(
                        csvRow.managementItemCode(),
                        csvRow.achievementDate(),
                        csvRow.achievementName(),
                        csvRow.attachmentIds());
                saveImported(request, requester, requestId);
            }
        }
        mapper.insertUploadHistory(
                uploadId,
                rows.size(),
                errors.isEmpty() ? rows.size() : 0,
                errors.size(),
                errors.isEmpty() ? rows.size() : 0,
                requester.userId());
        return new EmploymentRateExcelUploadResult(
                uploadId,
                fileName,
                rows.size(),
                errors.isEmpty() ? rows.size() : 0,
                errors.size(),
                errors);
    }

    /**
     * Rejects bulk execution until OQ-83-01 establishes a durable eligibility
     * policy; this avoids silently generating or deleting faculty records.
     */
    @Transactional
    public EmploymentRateBulkJobResponse createBulkJob(
            EmploymentRateBulkJobRequest request,
            CurrentUser requester,
            String requestId) {
        requireExcelRole(requester);
        validateBulkRequest(request);
        throw new ConflictException("OQ-83-01 실행조건과 삭제 허용 상태가 확정되지 않아 일괄 작업을 접수할 수 없습니다.");
    }

    /** Returns the persisted result for an R07 batch operation. */
    @Transactional(readOnly = true)
    public EmploymentRateBulkJobResponse getBulkJob(String batchJobId, CurrentUser requester) {
        requireExcelRole(requester);
        if (blankToNull(batchJobId) == null) {
            throw validation("jobId", "일괄 작업 ID를 입력하세요.");
        }
        Map<String, Object> result = mapper.findBatchJob(batchJobId.trim());
        if (result == null) {
            throw new NotFoundException("취업률 일괄 작업을 찾을 수 없습니다.");
        }
        return new EmploymentRateBulkJobResponse(
                string(result, "batchJobId"),
                string(result, "evaluationYear"),
                jsonMap(string(result, "targetConditionJson")),
                string(result, "actionType"),
                string(result, "jobStatus"),
                number(result, "totalCount"),
                number(result, "successCount"),
                number(result, "failureCount"),
                number(result, "unprocessedCount"),
                string(result, "requestId"));
    }

    private void saveImported(
            EmploymentRateAchievementRequest request,
            CurrentUser requester,
            String requestId) {
        validateRequest(request);
        Long achievementId = mapper.insertAchievement(new EmploymentRateAchievementInsert(
                requester.userId(),
                String.valueOf(Year.from(request.achievementDate())),
                request.managementItemCode().trim(),
                request.achievementDate(),
                blankToNull(request.achievementName()),
                attachmentJson(request.attachmentIds()),
                null));
        if (achievementId == null) {
            throw new ConflictException("활성 소속 정보가 없어 Excel 행을 반영할 수 없습니다.");
        }
        mapper.insertStatusHistory(achievementId, "취업률 실적 Excel 반영", requester.userId());
        mapper.insertChangeHistory(
                achievementId,
                "CREATE",
                null,
                serialize(request),
                requester.userId(),
                "취업률 실적 Excel 반영",
                requestId);
    }

    private List<EmploymentRateCsvRow> parseCsv(MultipartFile file) {
        try {
            String text = new String(file.getBytes(), StandardCharsets.UTF_8);
            List<String> lines = text.lines().filter(line -> !line.isBlank()).toList();
            if (lines.isEmpty()
                    || !lines.get(0).equals("managementItemCode,achievementDate,achievementName,attachmentIds")) {
                throw validation(
                        "file",
                        "managementItemCode, achievementDate, achievementName, attachmentIds 열이 필요합니다.");
            }
            List<EmploymentRateCsvRow> rows = new ArrayList<>();
            for (int index = 1; index < lines.size(); index++) {
                String[] values = lines.get(index).split(",", -1);
                rows.add(new EmploymentRateCsvRow(
                        index + 1,
                        value(values, 0),
                        parseDate(value(values, 1)),
                        value(values, 2),
                        splitAttachments(value(values, 3))));
            }
            return rows;
        } catch (BusinessValidationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw validation("file", "취업률 실적 CSV 파일을 읽을 수 없습니다.");
        }
    }

    private List<EmploymentRateExcelErrorRow> validateRows(List<EmploymentRateCsvRow> rows) {
        List<EmploymentRateExcelErrorRow> errors = new ArrayList<>();
        Set<String> keys = new java.util.HashSet<>();
        for (EmploymentRateCsvRow row : rows) {
            if (blankToNull(row.managementItemCode()) == null) {
                errors.add(error(row, "managementItemCode", "REQUIRED", "관리항목은 필수입니다."));
            }
            if (row.achievementDate() == null) {
                errors.add(error(row, "achievementDate", "INVALID_DATE", "업적발생일 형식을 확인하세요."));
            }
            String key = row.managementItemCode() + "|" + row.achievementDate() + "|" + row.achievementName();
            if (!keys.add(key)) {
                errors.add(error(row, "achievementName", "DUPLICATE", "중복 행은 자동으로 갱신하지 않습니다."));
            }
        }
        return errors;
    }

    private EmploymentRateExcelErrorRow error(
            EmploymentRateCsvRow row,
            String column,
            String code,
            String reason) {
        String value = switch (column) {
            case "managementItemCode" -> row.managementItemCode();
            case "achievementDate" -> row.achievementDate() == null ? "" : row.achievementDate().toString();
            default -> row.achievementName();
        };
        return new EmploymentRateExcelErrorRow(row.rowNumber(), column, value, code, reason);
    }

    private EmploymentRateAchievementRow requireFound(Long achievementId) {
        if (achievementId == null || achievementId <= 0) {
            throw new NotFoundException("취업률 실적을 찾을 수 없습니다.");
        }
        Map<String, Object> found = mapper.find(achievementId);
        if (found == null) {
            throw new NotFoundException("취업률 실적을 찾을 수 없습니다.");
        }
        return row(found);
    }

    private void requireReadScope(EmploymentRateAchievementRow row, CurrentUser requester) {
        if (requester.roles().contains("R01") && requester.userId().equals(row.teacherUserId())) {
            return;
        }
        if (requester.roles().contains("R02") || requester.roles().contains("R04")) {
            List<EmploymentRateAchievementRow> visible = mapper.list(
                    requester.userId(), requester.roles(), 100, 0).stream().map(this::row).toList();
            if (visible.stream().anyMatch(item -> item.achievementId().equals(row.achievementId()))) {
                return;
            }
        }
        throw new ForbiddenException();
    }

    private EmploymentRateAchievementRow row(Map<String, Object> source) {
        return new EmploymentRateAchievementRow(
                ((Number) source.get("achievementId")).longValue(),
                ((Number) source.get("teacherUserId")).longValue(),
                string(source, "teacherLoginId"),
                string(source, "evaluationYear"),
                string(source, "managementItemCode"),
                (LocalDate) source.get("achievementDate"),
                string(source, "achievementName"),
                jsonStrings(string(source, "attachmentIdsJson")),
                string(source, "achievementStatus"),
                source.get("createdAt") == null ? null : source.get("createdAt").toString(),
                source.get("updatedAt") == null ? null : source.get("updatedAt").toString());
    }

    private void validateRequest(EmploymentRateAchievementRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        if (request == null) {
            errors.add(new ValidationError("body", "취업률 실적 입력값이 필요합니다."));
        } else {
            if (blankToNull(request.managementItemCode()) == null) {
                errors.add(new ValidationError("managementItemCode", "관리항목은 필수입니다."));
            }
            if (request.achievementDate() == null) {
                errors.add(new ValidationError("achievementDate", "업적발생일은 필수입니다."));
            }
        }
        if (!errors.isEmpty()) {
            throw new BusinessValidationException("취업률 실적 요청이 올바르지 않습니다.", errors);
        }
    }

    private void validateBulkRequest(EmploymentRateBulkJobRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        if (request == null || blankToNull(request.evaluationYear()) == null) {
            errors.add(new ValidationError("evaluationYear", "평가연도는 필수입니다."));
        }
        if (request == null || !("GENERATE".equals(request.actionType()) || "DELETE".equals(request.actionType()))) {
            errors.add(new ValidationError("actionType", "GENERATE 또는 DELETE를 선택하세요."));
        }
        if (!errors.isEmpty()) {
            throw new BusinessValidationException("일괄 작업 요청이 올바르지 않습니다.", errors);
        }
    }

    private void validatePageSize(int pageSize) {
        if (!(pageSize == 20 || pageSize == 50 || pageSize == 100)) {
            throw validation("pageSize", "20, 50, 100건 중 하나를 선택하세요.");
        }
    }

    private void requireAchievementRole(CurrentUser user) {
        if (user == null || user.roles() == null || user.roles().stream().noneMatch(ACHIEVEMENT_ROLES::contains)) {
            throw new ForbiddenException();
        }
    }

    private void requireCreateRole(CurrentUser user) {
        if (user == null || user.roles() == null || !user.roles().contains("R01")) {
            throw new ForbiddenException();
        }
    }

    private void requireExcelRole(CurrentUser user) {
        if (user == null || user.roles() == null || !user.roles().contains("R07")) {
            throw new ForbiddenException();
        }
    }

    private BusinessValidationException validation(String field, String message) {
        return new BusinessValidationException(message, List.of(new ValidationError(field, message)));
    }

    private String attachmentJson(List<String> attachmentIds) {
        try {
            return objectMapper.writeValueAsString(attachmentIds == null ? List.of() : attachmentIds);
        } catch (JsonProcessingException exception) {
            throw validation("attachmentIds", "첨부 식별자 형식을 확인하세요.");
        }
    }

    private List<String> jsonStrings(String json) {
        try {
            return json == null ? List.of() : objectMapper.readValue(json, new TypeReference<List<String>>() { });
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private Map<String, Object> jsonMap(String json) {
        try {
            return json == null
                    ? Map.of()
                    : objectMapper.readValue(
                            json,
                            new TypeReference<LinkedHashMap<String, Object>>() { });
        } catch (JsonProcessingException exception) {
            return Map.of();
        }
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("감사 이력 직렬화에 실패했습니다.");
        }
    }

    private LocalDate parseDate(String value) {
        try {
            return blankToNull(value) == null ? null : LocalDate.parse(value.trim());
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private List<String> splitAttachments(String value) {
        return blankToNull(value) == null ? List.of() : List.of(value.split("\\|"));
    }

    private String value(String[] values, int index) {
        return index < values.length ? values[index].trim() : "";
    }

    private String safeFilename(String name) {
        return blankToNull(name) == null ? "employment-rate-achievements.csv" : name.replace("/", "").replace("\\", "");
    }

    private String csv(String value) {
        return '"' + (value == null ? "" : value.replace("\"", "\"\"")) + '"';
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String string(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value == null ? null : value.toString();
    }

    private int number(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value instanceof Number number ? number.intValue() : 0;
    }
}

record EmploymentRateAchievementRequest(
        @NotBlank(message = "관리항목은 필수입니다.") String managementItemCode,
        @NotNull(message = "업적발생일은 필수입니다.") LocalDate achievementDate,
        String achievementName,
        List<String> attachmentIds) {
}

record EmploymentRateAchievementRow(
        Long achievementId,
        Long teacherUserId,
        String teacherLoginId,
        String evaluationYear,
        String managementItemCode,
        LocalDate achievementDate,
        String achievementName,
        List<String> attachmentIds,
        String achievementStatus,
        String createdAt,
        String updatedAt) {
}

record EmploymentRateAchievementSearchResponse(
        List<EmploymentRateAchievementRow> achievements,
        int page,
        int pageSize,
        long totalElements) {
}

record EmploymentRateAchievementSaveResponse(
        EmploymentRateAchievementRow achievement,
        boolean occurredDateWarning,
        String warningMessage) {
}

record EmploymentRateExcelUploadResult(
        String uploadId,
        String originalFileName,
        int totalCount,
        int successCount,
        int errorCount,
        List<EmploymentRateExcelErrorRow> errors) {
}

record EmploymentRateExcelErrorRow(
        int rowNumber,
        String columnName,
        String inputValue,
        String errorCode,
        String errorReason) {
}

record EmploymentRateBulkJobRequest(
        String evaluationYear,
        String actionType,
        Map<String, Object> targetCondition) {
}

record EmploymentRateBulkJobResponse(
        String batchJobId,
        String evaluationYear,
        Map<String, Object> targetCondition,
        String actionType,
        String jobStatus,
        int totalCount,
        int successCount,
        int failureCount,
        int unprocessedCount,
        String requestId) {
}

record EmploymentRateCsvRow(
        int rowNumber,
        String managementItemCode,
        LocalDate achievementDate,
        String achievementName,
        List<String> attachmentIds) {
}
