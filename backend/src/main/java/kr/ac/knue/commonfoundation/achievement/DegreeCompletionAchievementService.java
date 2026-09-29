package kr.ac.knue.commonfoundation.achievement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implements authorized, persistence-backed degree-completion listing and atomic header/student saves. */
@Service
public class DegreeCompletionAchievementService {
    private static final Set<String> ALLOWED_ROLES = Set.of("R01", "R02", "R04", "R09");
    private static final Set<Integer> PAGE_SIZES = Set.of(20, 50, 100);
    private final DegreeCompletionAchievementMapper mapper;
    private final ObjectMapper objectMapper;
    private final EducationAchievementValidationChain validationChain = new EducationAchievementValidationChain();

    public DegreeCompletionAchievementService(DegreeCompletionAchievementMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    /** Lists non-deleted degree completion achievements with only supplied filters bound into SQL. */
    @Transactional(readOnly = true)
    public DegreeCompletionAchievementSearchResponse list(CurrentUser user, String evaluationYear, String managementNo,
            String teacherName, String managementItemCode, String certificationStatus, int page, int pageSize) {
        requireRole(user);
        int safePage = Math.max(page, 0);
        int safePageSize = PAGE_SIZES.contains(pageSize) ? pageSize : 20;
        DegreeCompletionAchievementSearchCriteria criteria = new DegreeCompletionAchievementSearchCriteria(
                blankToNull(evaluationYear), blankToNull(managementNo), blankToNull(teacherName),
                blankToNull(managementItemCode), blankToNull(certificationStatus), safePageSize, safePage * safePageSize);
        return new DegreeCompletionAchievementSearchResponse(mapper.list(criteria), safePage, safePageSize, mapper.count(criteria));
    }

    /** Saves a header and all required supervised-student details in one transaction with audit history. */
    @Transactional
    public DegreeCompletionAchievementSaveResponse save(CurrentUser user, SaveDegreeCompletionAchievementRequest request,
            String requestId) {
        requireRole(user);
        validateSaveRequest(request);
        LocalDate occurredDate = request.occurredDate() == null ? request.students().get(0).degreeAwardedDate() : request.occurredDate();
        EducationAchievementValidationChain.ValidationResult validation = validationChain.validate(
                new EducationAchievementValidationChain.ValidationContext(user, true, true, false, occurredDate,
                        LocalDate.of(occurredDate.getYear(), 1, 1), LocalDate.of(occurredDate.getYear(), 12, 31)));
        String managementNo = "DC-" + UUID.randomUUID();
        mapper.insertAchievement(managementNo, Integer.toString(occurredDate.getYear()), user.userId(), user.name(),
                request.managementItemCode().trim(), occurredDate, serializeDetail(request), user.userId());
        DegreeCompletionAchievementRow saved = mapper.findByManagementNo(managementNo);
        for (SaveDegreeCompletionAchievementRequest.StudentInput student : request.students()) {
            mapper.insertStudent(saved.achievementId(), student, user.userId());
        }
        mapper.insertStatusHistory(saved.achievementId(), user.userId());
        mapper.insertChangeHistory(saved.achievementId(), user.userId(), requestId);
        return new DegreeCompletionAchievementSaveResponse(mapper.findByManagementNo(managementNo), validation.warnings());
    }

    private void requireRole(CurrentUser user) {
        if (user == null || user.roles() == null || user.roles().stream().noneMatch(ALLOWED_ROLES::contains)) throw new ForbiddenException();
    }

    private void validateSaveRequest(SaveDegreeCompletionAchievementRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request == null || blankToNull(request.managementItemCode()) == null) fields.add(new ValidationError("managementItemCode", "관리항목을 입력하세요."));
        if (request == null || request.students() == null || request.students().isEmpty()) fields.add(new ValidationError("students", "지도학생을 한 명 이상 입력하세요."));
        if (request != null && request.students() != null) for (SaveDegreeCompletionAchievementRequest.StudentInput student : request.students()) {
            if (student == null || blankToNull(student.degreeType()) == null) fields.add(new ValidationError("degreeType", "학위구분을 입력하세요."));
            if (student == null || blankToNull(student.studentName()) == null) fields.add(new ValidationError("studentName", "학생명을 입력하세요."));
            if (student == null || blankToNull(student.thesisTitle()) == null) fields.add(new ValidationError("thesisTitle", "논문제목을 입력하세요."));
            if (student == null || student.degreeAwardedDate() == null) fields.add(new ValidationError("degreeAwardedDate", "학위수여일을 입력하세요."));
            if (student != null && blankToNull(student.degreeType()) != null && !Set.of("MASTER", "DOCTOR").contains(student.degreeType().trim())) fields.add(new ValidationError("degreeType", "학위구분은 MASTER 또는 DOCTOR여야 합니다."));
        }
        if (!fields.isEmpty()) throw new BusinessValidationException("석·박사 배출 저장 요청이 올바르지 않습니다.", fields);
    }

    private String serializeDetail(SaveDegreeCompletionAchievementRequest request) {
        try { return objectMapper.writeValueAsString(request.achievementDetail() == null ? objectMapper.createObjectNode() : request.achievementDetail()); }
        catch (JsonProcessingException exception) { throw new IllegalArgumentException("석·박사 배출 상세 입력값을 처리할 수 없습니다."); }
    }
    private String blankToNull(String value) { return value == null || value.trim().isBlank() ? null : value.trim(); }
}
