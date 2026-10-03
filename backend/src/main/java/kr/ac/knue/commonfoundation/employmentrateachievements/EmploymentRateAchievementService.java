package kr.ac.knue.commonfoundation.employmentrateachievements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
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

/** Coordinates guarded employment-rate source writes and R07 upload/bulk entry points. */
@Service
public class EmploymentRateAchievementService {
    private static final Set<String> READ_ROLES = Set.of("R01", "R02", "R04");
    private final EmploymentRateAchievementMapper mapper;
    private final EducationAchievementGuardService guardService;
    private final ObjectMapper objectMapper;

    public EmploymentRateAchievementService(EmploymentRateAchievementMapper mapper,
            EducationAchievementGuardService guardService, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.guardService = guardService;
        this.objectMapper = objectMapper;
    }

    /** Lists caller-scoped records using the supported page-size values. */
    @Transactional(readOnly = true)
    public EmploymentRateAchievementSearchResponse list(int page, int pageSize, CurrentUser requester) {
        if (requester == null || requester.roles() == null
                || requester.roles().stream().noneMatch(role -> READ_ROLES.contains(role) || role.equals("R07"))) {
            throw new ForbiddenException();
        }
        return new EmploymentRateAchievementSearchResponse(
                mapper.list(Math.max(0, page), pageSize, requester.userId(), requester.roles()),
                Math.max(0, page), pageSize, mapper.count(requester.userId(), requester.roles()));
    }

    /** Returns a source row only after applying the same ownership/data-scope policy as list. */
    @Transactional(readOnly = true)
    public EmploymentRateAchievementRow get(Long achievementId, CurrentUser requester) {
        requireReadRole(requester);
        EmploymentRateAchievementRow found = requireFound(achievementId);
        if (requester.roles()
        .contains("R01") &&
        !requester.userId()
        .equals(found.targetUserId())) throw new ForbiddenException();
        return found;
    }

    /** Creates an achievement and its change history in one transaction after server-side guards. */
    @Transactional
    public EmploymentRateAchievementRow create(EmploymentRateAchievementRequest request, CurrentUser requester,
            String requestId) {
        requireWriter(requester);
        validate(request);
        guardService.validateMutation(requester, new EducationAchievementMutationContext(requester.userId(),
                String.valueOf(Year.from(request.achievementDate())), request.achievementDate()));
        String managementNo = "ER-" + UUID.randomUUID();
        mapper.insert(managementNo, requester.userId(), String.valueOf(Year.from(request.achievementDate())),
                request.managementItemCode()
        .trim(),
        request.achievementDate(),
        blankToNull(request.achievementName()),
                attachments(request.attachmentIds()), requester.userId());
        EmploymentRateAchievementRow saved = mapper.findByManagementNo(managementNo);
        if (saved == null) throw new NotFoundException("저장한 취업률 실적을 찾을 수 없습니다.");
        mapper.insertChangeHistory(String.valueOf(saved.achievementId()), "CREATE", null, request.achievementName(),
                requester.userId(), requestId);
        return saved;
    }

    /** Updates only the owner's unlocked record and records the previous achievement name. */
    @Transactional
    public EmploymentRateAchievementRow update(Long achievementId, EmploymentRateAchievementRequest request,
            CurrentUser requester, String requestId) {
        requireWriter(requester);
        validate(request);
        EmploymentRateAchievementRow existing = requireFound(achievementId);
        if (!requester.userId().equals(existing.targetUserId())) throw new ForbiddenException();
        guardService.validateMutation(requester, new EducationAchievementMutationContext(existing.targetUserId(),
                existing.evaluationYear(), request.achievementDate()));
        mapper.update(achievementId, request.managementItemCode().trim(), request.achievementDate(),
                blankToNull(request.achievementName()), attachments(request.attachmentIds()), requester.userId());
        mapper.insertChangeHistory(String.valueOf(achievementId), "UPDATE", existing.achievementName(),
                request.achievementName(), requester.userId(), requestId);
        return requireFound(achievementId);
    }

    /** Validates all upload rows first; an invalid or duplicate row persists diagnostics but no source row. */
    @Transactional
    public EmploymentRateExcelUploadResult upload(MultipartFile file, CurrentUser requester) {
        requireExcelOperator(requester);
        if (file == null || file.isEmpty()) throw validation("file", "업로드 파일을 선택하세요.");
        List<EmploymentRateExcelError> errors = new ArrayList<>();
        List<String> lines;
        try { lines = new String(file.getBytes(),
        StandardCharsets.UTF_8)
        .lines()
        .filter(line -> !line.isBlank())
        .toList(); }
        catch (Exception exception) { throw validation("file", "파일을 읽을 수 없습니다."); }
        if (lines.isEmpty() ||
        !lines.get(0)
        .equals("employeeNo,managementItemCode,achievementDate,achievementName,attachmentRef")) {
            throw validation("file", "취업률 실적 현행 양식을 사용하세요.");
        }
        List<UploadRow> validRows = new ArrayList<>();
        for (int index = 1; index < lines.size(); index++) {
            String[] value = lines.get(index).split(",", -1);
            String employeeNo = cell(value, 0);
            String managementItemCode = cell(value, 1);
            String dateText = cell(value, 2);
            String achievementName = cell(value, 3);
            if (employeeNo.isBlank()) errors.add(error(index + 1,
        "employeeNo",
        employeeNo,
        "REQUIRED",
        "교번은 필수입니다."));
            if (managementItemCode.isBlank()) errors.add(error(index + 1,
        "managementItemCode",
        managementItemCode,
        "REQUIRED",
        "관리항목은 필수입니다."));
            try {
                LocalDate date = LocalDate.parse(dateText);
                Long targetUserId = mapper.findUserIdByEmployeeNo(employeeNo);
                if (targetUserId == null) errors.add(error(index + 1,
        "employeeNo",
        employeeNo,
        "NOT_FOUND",
        "교번에 해당하는 사용자가 없습니다."));
                else if (mapper.countDuplicate(targetUserId,
        managementItemCode,
        date,
        blankToNull(achievementName)) > 0) errors.add(error(index + 1,
        "achievementName",
        achievementName,
        "DUPLICATE",
        "중복 데이터는 자동 갱신하지 않습니다."));
                else validRows.add(new UploadRow(targetUserId,
        managementItemCode,
        date,
        blankToNull(achievementName),
        cell(value,
        4)));
            } catch (Exception exception) { errors.add(error(index + 1,
        "achievementDate",
        dateText,
        "INVALID_DATE",
        "업적발생일 형식을 확인하세요.")); }
        }
        String uploadId = "ER-UP-" + UUID.randomUUID();
        boolean accepted = errors.isEmpty();
        mapper.insertUpload(uploadId,
        safeFileName(file.getOriginalFilename()),
        requester.userId(),
        accepted ? "COMMITTED" : "REJECTED",
        lines.size() - 1,
        accepted ? validRows.size() : 0,
        errors.size());
        for (EmploymentRateExcelError error : errors) mapper.insertUploadError(uploadId, error);
        if (accepted) for (UploadRow row : validRows) mapper.insert("ER-" + UUID.randomUUID(),
        row.targetUserId(),
        String.valueOf(Year.from(row.date())),
        row.managementItemCode(),
        row.date(),
        row.achievementName(),
        attachments(List.of(row.attachmentRef())),
        requester.userId());
        return new EmploymentRateExcelUploadResult(uploadId,
        safeFileName(file.getOriginalFilename()),
        lines.size() - 1,
        accepted ? validRows.size() : 0,
        errors.size(),
        accepted ? validRows.size() : 0,
        errors);
    }

    /** Blocks unapproved bulk execution rather than inventing the OQ-83-01 policy. */
    @Transactional
    public void createBulkJob(EmploymentRateBulkJobRequest request, CurrentUser requester) {
        requireExcelOperator(requester);
        if (request == null ||
        blankToNull(request.evaluationYear()) == null) throw validation("evaluationYear",
        "평가연도를 입력하세요.");
        throw new ConflictException("OQ-83-01: 일괄 실행조건과 삭제 허용 상태가 확정되지 않았습니다.");
    }

    /** Returns only a bulk job owned by the requesting R07 operator. */
    @Transactional(readOnly = true)
    public EmploymentRateBulkJobResult getBulkJob(String jobId, CurrentUser requester) {
        requireExcelOperator(requester);
        EmploymentRateBulkJobResult result = mapper.findBulkJob(jobId, requester.userId());
        if (result == null) throw new NotFoundException("일괄 작업을 찾을 수 없습니다.");
        return result;
    }

    private EmploymentRateAchievementRow requireFound(Long id) {
        EmploymentRateAchievementRow found = mapper.find(id);
        if (found == null) throw new NotFoundException("취업률 실적을 찾을 수 없습니다.");
        return found;
    }
    private void validate(EmploymentRateAchievementRequest request) {
        if (request == null ||
        blankToNull(request.managementItemCode()) == null ||
        request.achievementDate() == null) {
            throw validation(request == null ||
        blankToNull(request == null ? null : request.managementItemCode()) == null
                    ? "managementItemCode" : "achievementDate", "관리항목과 업적발생일은 필수입니다.");
        }
    }
    private BusinessValidationException validation(String field,
        String message) { return new BusinessValidationException("취업률 실적 요청이 올바르지 않습니다.",
        List.of(new ValidationError(field,
        message))); }
    private void requireReadRole(CurrentUser user) { if (user == null ||
        user.roles() == null ||
        user.roles()
        .stream()
        .noneMatch(READ_ROLES::contains)) throw new ForbiddenException(); }
    private void requireWriter(CurrentUser user) { if (user == null ||
        user.roles() == null ||
        !user.roles()
        .contains("R01")) throw new ForbiddenException(); }
    private void requireExcelOperator(CurrentUser user) { if (user == null ||
        user.roles() == null ||
        !user.roles()
        .contains("R07")) throw new ForbiddenException(); }
    private String attachments(List<String> values) { try { return objectMapper.writeValueAsString(values == null ? List.of() : values); } catch (JsonProcessingException exception) { throw validation("attachmentIds",
        "첨부 참조 형식이 올바르지 않습니다."); } }
    private String cell(String[] values, int index) { return index < values.length ? values[index].trim() : ""; }
    private EmploymentRateExcelError error(int row,
        String column,
        String value,
        String code,
        String reason) { return new EmploymentRateExcelError(row,
        column,
        value,
        code,
        reason); }
    private String safeFileName(String value) { return blankToNull(value) == null ? "employment-rate.csv" : value.replace("/",
        "")
        .replace("\\",
        ""); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private record UploadRow(Long targetUserId,
        String managementItemCode,
        LocalDate date,
        String achievementName,
        String attachmentRef) { }
}
