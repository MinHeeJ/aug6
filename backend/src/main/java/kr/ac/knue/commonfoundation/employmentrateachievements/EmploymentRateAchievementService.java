package kr.ac.knue.commonfoundation.employmentrateachievements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementGuardService;
import kr.ac.knue.commonfoundation.basic81.EducationAchievementMutationContext;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Implements the employment-rate achievement API against the BASIC-83 shared
 * header table. Writes retain audit history and reuse the existing education
 * guard before changing a caller-owned record.
 */
@Service
public class EmploymentRateAchievementService {
    private static final Set<String> READ_ROLES = Set.of("R01", "R02", "R04", "R07");
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

    /** Returns the R01/R02/R04 list view and the R07 all-scope bulk-target view. */
    @Transactional(readOnly = true)
    public EmploymentRateAchievementViews.SearchResponse list(int page, int pageSize, CurrentUser requester) {
        requireReadRole(requester);
        return search(page, pageSize, requester, requester.roles().contains("R07"));
    }

    /**
     * Returns the export view using the same R01/R02/R04 data scope as list;
     * R07 is an explicit operational export role and can export all active rows.
     */
    @Transactional(readOnly = true)
    public EmploymentRateAchievementViews.SearchResponse download(int page, int pageSize, CurrentUser requester) {
        requireDownloadRole(requester);
        return search(page, pageSize, requester, requester.roles().contains("R07"));
    }

    private EmploymentRateAchievementViews.SearchResponse search(
            int page,
            int pageSize,
            CurrentUser requester,
            boolean allScope) {
        boolean ownScope = requester.roles().contains("R01");
        boolean organizationScope = requester.roles().contains("R02");
        boolean certificationScope = requester.roles().contains("R04");
        List<EmploymentRateAchievementViews.Achievement> achievements = mapper.list(
                        pageSize,
                        page * pageSize,
                        requester.userId(),
                        ownScope,
                        organizationScope,
                        certificationScope,
                        allScope)
                .stream()
                .map(this::toAchievement)
                .toList();
        long total = mapper.count(
                requester.userId(),
                ownScope,
                organizationScope,
                certificationScope,
                allScope);
        return new EmploymentRateAchievementViews.SearchResponse(achievements, page, pageSize, total);
    }

    /** Returns one record only after the same caller-visible scope rule used by list has been applied. */
    @Transactional(readOnly = true)
    public EmploymentRateAchievementViews.Achievement get(Long achievementId, CurrentUser requester) {
        requireReadRole(requester);
        Map<String, Object> achievement = requireAchievement(achievementId);
        requireVisible(requester, achievement, requester.roles().contains("R07"));
        return toAchievement(achievement);
    }

    /** Creates a caller-owned DRAFT record and its audit entry in one transaction. */
    @Transactional
    public EmploymentRateAchievementViews.Achievement create(
            EmploymentRateAchievementRequest request,
            CurrentUser requester,
            String requestId) {
        requireWriter(requester);
        validateRequest(request);
        guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        requester.userId(),
                        String.valueOf(Year.from(request.getAchievementDate())),
                        request.getAchievementDate()));
        String managementNo = "ER-" + UUID.randomUUID();
        mapper.insert(
                managementNo,
                requester.userId(),
                String.valueOf(Year.from(request.getAchievementDate())),
                request.getManagementItemCode().trim(),
                request.getAchievementDate(),
                blankToNull(request.getAchievementName()),
                attachmentReference(request.getAttachmentIds()));
        Long achievementId = mapper.findIdByManagementNo(managementNo);
        if (achievementId == null) {
            throw new NotFoundException("저장한 취업률 실적을 찾을 수 없습니다.");
        }
        mapper.insertChangeHistory(
                String.valueOf(achievementId),
                "CREATE",
                null,
                auditPayload(request),
                requester.userId(),
                "취업률 실적 최초 입력",
                requestId);
        return toAchievement(requireAchievement(achievementId));
    }

    /** Updates only a DRAFT-or-in-progress record owned by the R01 caller and audits the replacement. */
    @Transactional
    public EmploymentRateAchievementViews.Achievement update(
            Long achievementId,
            EmploymentRateAchievementRequest request,
            CurrentUser requester,
            String requestId) {
        requireWriter(requester);
        validateRequest(request);
        Map<String, Object> existing = requireAchievement(achievementId);
        Long ownerId = asLong(existing.get("teacherUserId"));
        if (!requester.userId().equals(ownerId)) {
            throw new ForbiddenException();
        }
        if ("EVALUATION_CONFIRMED".equals(existing.get("achievementStatus"))) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 실적은 수정할 수 없습니다.");
        }
        guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(
                        ownerId,
                        String.valueOf(Year.from(request.getAchievementDate())),
                        request.getAchievementDate()));
        int changed = mapper.update(
                achievementId,
                request.getManagementItemCode().trim(),
                request.getAchievementDate(),
                blankToNull(request.getAchievementName()),
                attachmentReference(request.getAttachmentIds()),
                requester.userId());
        if (changed != 1) {
            throw new NotFoundException("수정할 취업률 실적을 찾을 수 없습니다.");
        }
        mapper.insertChangeHistory(
                String.valueOf(achievementId),
                "UPDATE",
                auditPayload(toAchievement(existing)),
                auditPayload(request),
                requester.userId(),
                "취업률 실적 수정",
                requestId);
        return toAchievement(requireAchievement(achievementId));
    }

    /**
     * Validates every supplied XLSX upload row before creating any
     * employment-rate record; invalid files retain only upload/error history.
     */
    @Transactional
    public EmploymentRateAchievementViews.UploadResult upload(
            MultipartFile file,
            CurrentUser requester,
            String requestId) {
        requireExcelRole(requester);
        List<UploadRow> rows = parseUpload(file);
        List<EmploymentRateAchievementViews.UploadError> errors = validateUploadRows(rows);
        String uploadId = "ER-UP-" + UUID.randomUUID();
        String filename = safeFilename(file == null ? null : file.getOriginalFilename());
        mapper.insertUpload(
                uploadId,
                "employment-rate-" + uploadId,
                filename,
                errors.isEmpty() ? "COMMITTED" : "REJECTED",
                requester.userId());
        for (EmploymentRateAchievementViews.UploadError error : errors) {
            mapper.insertUploadError(
                    "ER-ERR-" + UUID.randomUUID(),
                    uploadId,
                    error.rowNumber(),
                    error.columnName(),
                    error.inputValue(),
                    error.errorCode(),
                    error.errorReason());
        }
        if (!errors.isEmpty()) {
            mapper.insertUploadHistory(uploadId, rows.size(), 0, errors.size(), 0, requester.userId());
            return new EmploymentRateAchievementViews.UploadResult(
                    uploadId,
                    filename,
                    rows.size(),
                    0,
                    errors.size(),
                    errors);
        }
        for (UploadRow row : rows) {
            Long targetUserId = mapper.findActiveUserIdByEmployeeNo(row.employeeNo());
            if (targetUserId == null) {
                throw new IllegalStateException("검증된 Excel 대상 교원을 찾을 수 없습니다.");
            }
            String managementNo = "ER-" + UUID.randomUUID();
            mapper.insert(
                    managementNo,
                    targetUserId,
                    String.valueOf(Year.from(row.achievementDate())),
                    row.managementItemCode(),
                    row.achievementDate(),
                    blankToNull(row.achievementName()),
                    blankToNull(row.attachmentReference()));
            Long achievementId = mapper.findIdByManagementNo(managementNo);
            if (achievementId == null) {
                throw new NotFoundException("Excel 반영한 취업률 실적을 찾을 수 없습니다.");
            }
            mapper.insertChangeHistory(
                    String.valueOf(achievementId),
                    "CREATE",
                    null,
                    row.managementItemCode() + "|" + row.achievementDate() + "|" + row.achievementName(),
                    requester.userId(),
                    "취업률 실적 Excel 일괄 반영",
                    requestId);
        }
        mapper.insertUploadHistory(uploadId, rows.size(), rows.size(), 0, rows.size(), requester.userId());
        return new EmploymentRateAchievementViews.UploadResult(
                uploadId,
                filename,
                rows.size(),
                rows.size(),
                0,
                List.of());
    }

    /** Returns a persisted R07 bulk-job result without manufacturing unapproved execution outcomes. */
    @Transactional(readOnly = true)
    public EmploymentRateAchievementViews.BulkJob getBulkJob(String jobId, CurrentUser requester) {
        requireExcelRole(requester);
        Map<String, Object> job = mapper.findBulkJob(jobId, requester.userId());
        if (job == null) {
            throw new NotFoundException("일괄 작업을 찾을 수 없습니다.");
        }
        return new EmploymentRateAchievementViews.BulkJob(
                String.valueOf(job.get("jobId")),
                String.valueOf(job.get("evaluationYear")),
                String.valueOf(job.get("actionType")),
                String.valueOf(job.get("jobStatus")),
                asInt(job.get("totalCount")),
                asInt(job.get("processedCount")),
                asInt(job.get("successCount")),
                asInt(job.get("failureCount")),
                (LocalDateTime) job.get("requestedAt"),
                (LocalDateTime) job.get("completedAt"));
    }

    /** Rejects bulk-job admission until OQ-83-01 gives an approved preview and execution policy. */
    @Transactional
    public EmploymentRateAchievementViews.BulkJob requestBulkJob(
            EmploymentRateBulkJobRequest request,
            CurrentUser requester,
            String requestId) {
        requireExcelRole(requester);
        throw new ConflictException("OQ-83-01 미확정: 대상 미리보기와 실행 정책 승인 전에는 일괄 작업을 접수할 수 없습니다.");
    }

    private Map<String, Object> requireAchievement(Long achievementId) {
        if (achievementId == null || achievementId <= 0) {
            throw new NotFoundException("취업률 실적을 찾을 수 없습니다.");
        }
        Map<String, Object> result = mapper.find(achievementId);
        if (result == null) {
            throw new NotFoundException("취업률 실적을 찾을 수 없습니다.");
        }
        return result;
    }

    private EmploymentRateAchievementViews.Achievement toAchievement(Map<String, Object> row) {
        return new EmploymentRateAchievementViews.Achievement(
                asLong(row.get("achievementId")),
                stringValue(row.get("managementNo")),
                asLong(row.get("teacherUserId")),
                stringValue(row.get("teacherName")),
                stringValue(row.get("evaluationYear")),
                stringValue(row.get("managementItemCode")),
                (LocalDate) row.get("achievementDate"),
                stringValue(row.get("achievementName")),
                attachmentIds(stringValue(row.get("attachmentRef"))),
                stringValue(row.get("achievementStatus")),
                (LocalDateTime) row.get("createdAt"),
                (LocalDateTime) row.get("updatedAt"));
    }

    private List<UploadRow> parseUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessValidationException("업로드 파일을 선택하세요.", List.of(new ValidationError("file", "파일은 필수입니다.")));
        }
        String filename = safeFilename(file.getOriginalFilename());
        if (!filename.toLowerCase(java.util.Locale.ROOT).endsWith(".xlsx")) {
            throw new BusinessValidationException(
                    "취업률 실적 업로드 양식이 올바르지 않습니다.",
                    List.of(new ValidationError("file", "EMPLOYMENT_RATE_ACHIEVEMENT XLSX 양식을 선택하세요.")));
        }
        try {
            List<List<String>> worksheet = XlsxWorkbook.read(file.getBytes());
            List<String> header = worksheet.isEmpty() ? List.of() : worksheet.get(0);
            List<String> expected = List.of("교번", "관리항목코드", "업적발생일", "실적명", "첨부참조");
            if (!expected.equals(header)) {
                throw new BusinessValidationException(
                        "취업률 실적 업로드 양식이 올바르지 않습니다.",
                        List.of(new ValidationError("file", "교번, 관리항목코드, 업적발생일, 실적명, 첨부참조 열이 필요합니다.")));
            }
            List<UploadRow> rows = new ArrayList<>();
            for (int index = 1; index < worksheet.size(); index++) {
                List<String> values = worksheet.get(index);
                if (values.stream().allMatch(String::isBlank)) {
                    continue;
                }
                LocalDate date = null;
                try {
                    date = LocalDate.parse(value(values, 2));
                } catch (RuntimeException ignored) {
                    // Row-level validation records the malformed value after the complete workbook is inspected.
                }
                rows.add(new UploadRow(
                        index + 1,
                        value(values, 0),
                        value(values, 1),
                        date,
                        value(values, 3),
                        value(values, 4)));
            }
            return rows;
        } catch (BusinessValidationException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new BusinessValidationException(
                    "취업률 실적 Excel 파일을 읽지 못했습니다.",
                    List.of(new ValidationError("file", "XLSX 파일 형식과 양식을 확인하세요.")));
        }
    }

    private List<EmploymentRateAchievementViews.UploadError> validateUploadRows(List<UploadRow> rows) {
        List<EmploymentRateAchievementViews.UploadError> errors = new ArrayList<>();
        Set<String> identities = new java.util.HashSet<>();
        for (UploadRow row : rows) {
            if (blankToNull(row.employeeNo()) == null) {
                errors.add(uploadError(row, "employeeNo", "REQUIRED", "교번은 필수입니다."));
            } else if (mapper.findActiveUserIdByEmployeeNo(row.employeeNo()) == null) {
                errors.add(uploadError(row, "employeeNo", "INVALID_EMPLOYEE", "활성 교원을 찾을 수 없습니다."));
            }
            if (blankToNull(row.managementItemCode()) == null) {
                errors.add(uploadError(row, "managementItemCode", "REQUIRED", "관리항목은 필수입니다."));
            }
            if (row.achievementDate() == null) {
                errors.add(uploadError(row, "achievementDate", "INVALID_DATE", "업적발생일 형식을 확인하세요."));
            }
            String identity = row.managementItemCode() + "|" + row.achievementDate() + "|" + row.achievementName();
            if (!identities.add(identity)) {
                errors.add(uploadError(row, "achievementName", "DUPLICATE", "중복 행은 자동 갱신하지 않습니다."));
            } else if (row.achievementDate() != null
                    && blankToNull(row.managementItemCode()) != null
                    && mapper.countDuplicate(
                            row.managementItemCode(),
                            row.achievementDate(),
                            blankToNull(row.achievementName())) > 0) {
                errors.add(uploadError(row, "achievementName", "DUPLICATE", "기존 취업률 실적과 중복됩니다."));
            }
        }
        return errors;
    }

    private EmploymentRateAchievementViews.UploadError uploadError(
            UploadRow row,
            String columnName,
            String errorCode,
            String errorReason) {
        return new EmploymentRateAchievementViews.UploadError(
                row.rowNumber(),
                columnName,
                row.valueFor(columnName),
                errorCode,
                errorReason);
    }

    private void validateRequest(EmploymentRateAchievementRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        if (request == null) {
            errors.add(new ValidationError("body", "취업률 실적 정보를 입력하세요."));
        } else {
            if (blankToNull(request.getManagementItemCode()) == null) {
                errors.add(new ValidationError("managementItemCode", "관리항목을 입력하세요."));
            }
            if (request.getAchievementDate() == null) {
                errors.add(new ValidationError("achievementDate", "업적발생일을 입력하세요."));
            }
        }
        if (!errors.isEmpty()) {
            throw new BusinessValidationException("취업률 실적 저장 요청이 올바르지 않습니다.", errors);
        }
    }

    private void requireVisible(CurrentUser requester, Map<String, Object> achievement, boolean allowR07AllScope) {
        if (allowR07AllScope && requester.roles().contains("R07")) {
            return;
        }
        Long targetUserId = asLong(achievement.get("teacherUserId"));
        if (requester.roles().contains("R01") && requester.userId().equals(targetUserId)) {
            return;
        }
        if (requester.roles().contains("R02")
                && mapper.countSharedOrganization(requester.userId(), targetUserId) > 0) {
            return;
        }
        if (requester.roles().contains("R04")
                && mapper.countCertificationScope(requester.userId(), targetUserId) > 0) {
            return;
        }
        throw new ForbiddenException();
    }

    private void requireReadRole(CurrentUser requester) {
        if (requester == null || requester.roles() == null
                || requester.roles().stream().noneMatch(READ_ROLES::contains)) {
            throw new ForbiddenException();
        }
    }

    private void requireDownloadRole(CurrentUser requester) {
        if (requester == null || requester.roles() == null
                || requester.roles().stream().noneMatch(role -> READ_ROLES.contains(role) || "R07".equals(role))) {
            throw new ForbiddenException();
        }
    }

    private void requireWriter(CurrentUser requester) {
        if (requester == null || requester.roles() == null || !requester.roles().contains("R01")) {
            throw new ForbiddenException();
        }
    }

    private void requireExcelRole(CurrentUser requester) {
        if (requester == null || requester.roles() == null || !requester.roles().contains("R07")) {
            throw new ForbiddenException();
        }
    }

    private String attachmentReference(List<String> attachmentIds) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            return null;
        }
        return attachmentIds.stream().filter(value -> value != null && !value.isBlank()).map(String::trim)
                .reduce((left, right) -> left + "," + right).orElse(null);
    }

    private List<String> attachmentIds(String attachmentRef) {
        if (attachmentRef == null || attachmentRef.isBlank()) {
            return List.of();
        }
        return List.of(attachmentRef.split(","));
    }

    private String auditPayload(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("감사 정보를 만들지 못했습니다.");
        }
    }

    private String safeFilename(String filename) {
        String safe = blankToNull(filename);
        return safe == null ? "employment-rate-achievements.xlsx" : safe.replace("/", "").replace("\\", "");
    }

    private String value(List<String> values, int index) {
        return index < values.size() ? values.get(index).trim() : "";
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : Long.valueOf(String.valueOf(value));
    }

    private int asInt(Object value) {
        return value instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(value));
    }

    private record UploadRow(
            int rowNumber,
            String employeeNo,
            String managementItemCode,
            LocalDate achievementDate,
            String achievementName,
            String attachmentReference) {
        private String valueFor(String columnName) {
            return switch (columnName) {
                case "employeeNo" -> employeeNo;
                case "managementItemCode" -> managementItemCode;
                case "achievementDate" -> achievementDate == null ? "" : achievementDate.toString();
                case "achievementName" -> achievementName;
                case "attachmentRef" -> attachmentReference;
                default -> "";
            };
        }
    }
}
