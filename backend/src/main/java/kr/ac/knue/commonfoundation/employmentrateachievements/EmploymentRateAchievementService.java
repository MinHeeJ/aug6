package kr.ac.knue.commonfoundation.employmentrateachievements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashSet;
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
import kr.ac.knue.commonfoundation.excel.ExcelOperationsService;
import kr.ac.knue.commonfoundation.excel.ExcelUploadResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Owns employment-rate achievement writes and delegates upload history/error
 * preservation to the shared Excel service before atomically materializing rows.
 */
@Service
public class EmploymentRateAchievementService {
    private static final Set<String> READ_ROLES = Set.of("R01", "R02", "R04", "R07");
    private static final String EXCEL_BUSINESS_TYPE = "EMPLOYMENT_RATE_ACHIEVEMENT";
    private final EmploymentRateAchievementMapper mapper;
    private final EducationAchievementGuardService guardService;
    private final ExcelOperationsService excelOperationsService;
    private final ObjectMapper objectMapper;

    public EmploymentRateAchievementService(
            EmploymentRateAchievementMapper mapper,
            EducationAchievementGuardService guardService,
            ExcelOperationsService excelOperationsService,
            ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.guardService = guardService;
        this.excelOperationsService = excelOperationsService;
        this.objectMapper = objectMapper;
    }

    /** Lists caller-owned employment-rate rows using the approved 20/50/100 pagination. */
    @Transactional(readOnly = true)
    public EmploymentRateAchievementListResponse list(int page, int pageSize, CurrentUser user) {
        requireReadRole(user);
        int safePage = Math.max(0, page);
        int safePageSize = validatePageSize(pageSize);
        List<EmploymentRateAchievementRow> rows = mapper.list(
                user.userId(),
                safePageSize,
                safePage * safePageSize);
        return new EmploymentRateAchievementListResponse(
                rows,
                safePage,
                safePageSize,
                mapper.count(user.userId()));
    }

    /** Reads one row after applying the self-only data scope available in this slice. */
    @Transactional(readOnly = true)
    public EmploymentRateAchievementRow get(Long achievementId, CurrentUser user) {
        requireReadRole(user);
        EmploymentRateAchievementRow row = find(achievementId);
        requireOwnerOrReadScope(row, user);
        return row;
    }

    /** Creates an auditable draft after organization, period, and finalization guards succeed. */
    @Transactional
    public EmploymentRateAchievementRow create(
            EmploymentRateAchievementRequest request,
            CurrentUser user,
            String requestId) {
        requireWriter(user);
        validateRequest(request);
        return createForUser(request, user.userId(), user.userId(), requestId);
    }

    /** Updates only owner-controlled mutable fields after shared lifecycle guards succeed. */
    @Transactional
    public EmploymentRateAchievementRow update(
            Long achievementId,
            EmploymentRateAchievementRequest request,
            CurrentUser user,
            String requestId) {
        requireWriter(user);
        validateRequest(request);
        EmploymentRateAchievementRow existing = find(achievementId);
        if (!existing.teacherUserId().equals(user.userId())) {
            throw new ForbiddenException();
        }
        validateActiveOrganizationAndMutation(user, existing.teacherUserId(), request.achievementDate());
        if ("EVALUATION_CONFIRMED".equals(existing.achievementStatus())) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 실적은 수정할 수 없습니다.");
        }
        mapper.update(
                achievementId,
                user.userId(),
                request.managementItemCode().trim(),
                String.valueOf(Year.from(request.achievementDate())),
                request.achievementDate(),
                blankToNull(request.achievementName()),
                attachmentJson(request.attachmentIds()));
        EmploymentRateAchievementRow saved = find(achievementId);
        mapper.insertChangeHistory(
                String.valueOf(saved.achievementId()),
                "UPDATE",
                serialized(existing),
                serialized(saved),
                user.userId(),
                "취업률 실적 수정",
                requestId);
        return saved;
    }

    /**
     * Records shared Excel history and errors first. Valid CSV rows are only
     * materialized after all rows are parsed, de-duplicated, and scope checked.
     */
    @Transactional
    public EmploymentRateExcelUploadResponse upload(
            MultipartFile file,
            CurrentUser user,
            String requestId) {
        requireExcelRole(user);
        ExcelUploadResult upload = excelOperationsService.createExcelUpload(
                EXCEL_BUSINESS_TYPE,
                null,
                file,
                user.userId());
        if (upload.errorCount() > 0) {
            return new EmploymentRateExcelUploadResponse(
                    false,
                    0,
                    upload.errorCount(),
                    "오류 행이 있어 업무 데이터는 반영하지 않았습니다.",
                    upload.uploadId());
        }
        List<EmploymentRateAchievementRequest> rows = parseRows(file);
        Set<String> duplicateKeys = new LinkedHashSet<>();
        for (EmploymentRateAchievementRequest row : rows) {
            validateRequest(row);
            String key = row.managementItemCode().trim() + "|" + row.achievementDate() + "|"
                    + blankToNull(row.achievementName());
            if (!duplicateKeys.add(key)) {
                throw new BusinessValidationException(
                        "중복 행이 있어 업무 데이터는 반영하지 않았습니다.",
                        List.of(new ValidationError("file", "중복 실적 행을 제거하세요.")));
            }
        }
        for (EmploymentRateAchievementRequest row : rows) {
            createForUser(row, user.userId(), user.userId(), requestId);
        }
        excelOperationsService.commitExcelUpload(upload.uploadId(), user.userId());
        return new EmploymentRateExcelUploadResponse(
                true,
                rows.size(),
                0,
                "검증된 행을 모두 반영했습니다.",
                null);
    }

    /**
     * Persists a confirmed preview request and target result rows. Execution is
     * deliberately asynchronous; this method never invents generation/deletion policy.
     */
    @Transactional
    public EmploymentRateBulkJobResponse createBulkJob(
            EmploymentRateBulkJobRequest request,
            CurrentUser user,
            String requestId) {
        requireExcelRole(user);
        validateBulkRequest(request);
        List<Long> targetUserIds = targetUserIds(request.targetCondition());
        if (!Boolean.TRUE.equals(request.targetCondition().get("confirmed")) || targetUserIds.isEmpty()) {
            throw new ConflictException(
                    "BULK_POLICY_PENDING: 확인된 대상 미리보기 없이는 일괄 작업을 접수할 수 없습니다.");
        }
        String jobId = "ERB-" + UUID.randomUUID();
        mapper.insertBulkJob(
                jobId,
                request.evaluationYear().trim(),
                json(request.targetCondition()),
                request.actionType().trim(),
                targetUserIds.size(),
                requestId,
                user.userId());
        for (Long targetUserId : targetUserIds) {
            mapper.insertBulkJobItem(jobId, targetUserId, user.userId());
        }
        return requireBulkJob(jobId, user);
    }

    /** Retrieves only the requesting R07 user's persisted job and target-level outcomes. */
    @Transactional(readOnly = true)
    public EmploymentRateBulkJobResponse getBulkJob(String jobId, CurrentUser user) {
        requireExcelRole(user);
        return requireBulkJob(jobId, user);
    }

    private EmploymentRateBulkJobResponse requireBulkJob(String jobId, CurrentUser user) {
        EmploymentRateBulkJobRow job = mapper.findBulkJob(jobId, user.userId());
        if (job == null) {
            throw new NotFoundException("일괄 처리 작업을 찾을 수 없습니다.");
        }
        return new EmploymentRateBulkJobResponse(
                job.jobId(),
                job.evaluationYear(),
                job.actionType(),
                job.jobStatus(),
                job.totalCount(),
                job.processedCount(),
                job.unprocessedCount(),
                job.requestedAt(),
                mapper.findBulkJobItems(job.jobId()));
    }

    private EmploymentRateAchievementRow createForUser(
            EmploymentRateAchievementRequest request,
            Long targetUserId,
            Long actorUserId,
            String requestId) {
        CurrentUser targetUser = new CurrentUser(
                targetUserId,
                null,
                null,
                null,
                List.of("R01"),
                List.of());
        validateActiveOrganizationAndMutation(targetUser, targetUserId, request.achievementDate());
        String managementNo = "ERA-" + UUID.randomUUID();
        int inserted = mapper.insert(
                managementNo,
                targetUserId,
                String.valueOf(Year.from(request.achievementDate())),
                request.managementItemCode().trim(),
                request.achievementDate(),
                blankToNull(request.achievementName()),
                attachmentJson(request.attachmentIds()));
        if (inserted != 1) {
            throw new ConflictException("ACTIVE_ORGANIZATION_MAPPING_REQUIRED: 활성 조직 매핑이 필요합니다.");
        }
        EmploymentRateAchievementRow saved = findByManagementNo(managementNo);
        mapper.insertChangeHistory(
                String.valueOf(saved.achievementId()),
                "CREATE",
                null,
                serialized(saved),
                actorUserId,
                "취업률 실적 등록",
                requestId);
        return saved;
    }

    private void validateActiveOrganizationAndMutation(
            CurrentUser user,
            Long targetUserId,
            LocalDate achievementDate) {
        if (mapper.countActiveOrganizationMappings(targetUserId) == 0) {
            throw new ConflictException("ACTIVE_ORGANIZATION_MAPPING_REQUIRED: 활성 조직 매핑이 필요합니다.");
        }
        guardService.validateMutation(
                user,
                new EducationAchievementMutationContext(
                        targetUserId,
                        String.valueOf(Year.from(achievementDate)),
                        achievementDate));
    }

    private List<EmploymentRateAchievementRequest> parseRows(MultipartFile file) {
        try {
            String[] lines = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8).split("\\R");
            List<EmploymentRateAchievementRequest> rows = new ArrayList<>();
            for (int index = 1; index < lines.length; index++) {
                if (lines[index].isBlank()) {
                    continue;
                }
                String[] values = lines[index].split(",", -1);
                if (values.length < 4) {
                    throw new BusinessValidationException(
                            "엑셀 행 형식이 올바르지 않습니다.",
                            List.of(new ValidationError("file", "관리항목코드, 업적발생일, 실적명 열을 확인하세요.")));
                }
                rows.add(new EmploymentRateAchievementRequest(
                        values[1].trim(),
                        LocalDate.parse(values[2].trim()),
                        values[3].trim(),
                        List.of()));
            }
            if (rows.isEmpty()) {
                throw new BusinessValidationException(
                        "엑셀 업로드 요청이 올바르지 않습니다.",
                        List.of(new ValidationError("file", "반영할 실적 행이 없습니다.")));
            }
            return rows;
        } catch (BusinessValidationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessValidationException(
                    "엑셀 파일을 해석할 수 없습니다.",
                    List.of(new ValidationError("file", "CSV 양식과 날짜 형식을 확인하세요.")));
        }
    }

    private EmploymentRateAchievementRow find(Long achievementId) {
        EmploymentRateAchievementRow row = mapper.find(achievementId);
        if (row == null) {
            throw new NotFoundException("취업률 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    private EmploymentRateAchievementRow findByManagementNo(String managementNo) {
        EmploymentRateAchievementRow row = mapper.findByManagementNo(managementNo);
        if (row == null) {
            throw new NotFoundException("저장한 취업률 실적을 찾을 수 없습니다.");
        }
        return row;
    }

    private void requireReadRole(CurrentUser user) {
        if (user == null || user.userId() == null || user.roles() == null
                || user.roles().stream().noneMatch(READ_ROLES::contains)) {
            throw new ForbiddenException();
        }
    }

    private void requireWriter(CurrentUser user) {
        if (user == null || user.userId() == null || user.roles() == null || !user.roles().contains("R01")) {
            throw new ForbiddenException();
        }
    }

    private void requireExcelRole(CurrentUser user) {
        if (user == null || user.userId() == null || user.roles() == null || !user.roles().contains("R07")) {
            throw new ForbiddenException();
        }
    }

    private void requireOwnerOrReadScope(EmploymentRateAchievementRow row, CurrentUser user) {
        if (user.roles().contains("R01") && !row.teacherUserId().equals(user.userId())) {
            throw new ForbiddenException();
        }
    }

    private int validatePageSize(int pageSize) {
        if (pageSize != 20 && pageSize != 50 && pageSize != 100) {
            throw new BusinessValidationException(
                    "목록 표시 건수가 올바르지 않습니다.",
                    List.of(new ValidationError("pageSize", "20, 50, 100건 중 하나를 선택하세요.")));
        }
        return pageSize;
    }

    private void validateRequest(EmploymentRateAchievementRequest request) {
        if (request == null) {
            throw new BusinessValidationException(
                    "취업률 실적 정보를 입력하세요.",
                    List.of(new ValidationError("body", "요청 본문이 필요합니다.")));
        }
    }

    private void validateBulkRequest(EmploymentRateBulkJobRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        if (request == null || blankToNull(request.evaluationYear()) == null) {
            errors.add(new ValidationError("evaluationYear", "평가연도를 입력하세요."));
        }
        if (request == null || !Set.of("GENERATE", "DELETE").contains(blankToNull(request.actionType()))) {
            errors.add(new ValidationError("actionType", "처리유형은 GENERATE 또는 DELETE여야 합니다."));
        }
        if (request == null || request.targetCondition() == null) {
            errors.add(new ValidationError("targetCondition", "대상 미리보기 조건을 입력하세요."));
        }
        if (!errors.isEmpty()) {
            throw new BusinessValidationException("일괄 처리 요청이 올바르지 않습니다.", errors);
        }
    }

    private List<Long> targetUserIds(Map<String, Object> targetCondition) {
        Object raw = targetCondition.get("targetUserIds");
        if (!(raw instanceof List<?> values)) {
            return List.of();
        }
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        for (Object value : values) {
            if (value instanceof Number number && number.longValue() > 0) {
                ids.add(number.longValue());
            }
        }
        return List.copyOf(ids);
    }

    private String attachmentJson(List<String> attachmentIds) {
        return json(attachmentIds == null ? List.of() : attachmentIds);
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessValidationException(
                    "요청 값을 저장할 수 없습니다.",
                    List.of(new ValidationError("body", "요청 값을 확인하세요.")));
        }
    }

    private String serialized(EmploymentRateAchievementRow row) {
        return json(row);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
