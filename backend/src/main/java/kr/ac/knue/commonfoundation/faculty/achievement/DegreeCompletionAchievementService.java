package kr.ac.knue.commonfoundation.faculty.achievement;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saves and retrieves degree-completion achievements with their student detail rows as one
 * transaction so a header can never be exposed without its degree award evidence.
 */
@Service
public class DegreeCompletionAchievementService {
    private static final Set<String> DEGREE_TYPES = Set.of("MASTER", "DOCTOR");
    private final DegreeCompletionAchievementMapper mapper;

    public DegreeCompletionAchievementService(DegreeCompletionAchievementMapper mapper) {
        this.mapper = mapper;
    }

    /** Returns a database-backed page using only predicates supplied by the caller. */
    @Transactional(readOnly = true)
    public DegreeCompletionSearchResponse list(DegreeCompletionSearchCriteria criteria) {
        DegreeCompletionSearchCriteria normalized = new DegreeCompletionSearchCriteria(
                criteria.managementNo(), criteria.teacherName(), criteria.certificationStatus(), Math.max(0, criteria.page()), criteria.safePageSize());
        return new DegreeCompletionSearchResponse(
                mapper.list(normalized), normalized.page(), normalized.pageSize(), mapper.count(normalized));
    }

    /**
     * Stores a header and all detail rows, rejecting mutation of an evaluation-confirmed record
     * before the shared detail rows are replaced and records the action in the common audit table.
     */
    @Transactional
    public DegreeCompletionAchievementRow save(
            SaveDegreeCompletionAchievementRequest request, Long userId, String employeeNo, String requestId) {
        List<ValidationError> fields = validate(request);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("석·박사 배출 실적 입력값이 올바르지 않습니다.", fields);
        }
        SaveDegreeCompletionAchievementRequest normalizedRequest = normalize(request);
        DegreeCompletionAchievementRow existing = mapper.findByManagementNo(normalizedRequest.managementNo());
        if (existing != null && "EVALUATION_CONFIRMED".equals(existing.certificationStatus())) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 데이터는 수정할 수 없습니다.");
        }
        mapper.insertAchievement(normalizedRequest, employeeNo, userId);
        DegreeCompletionAchievementRow saved = mapper.findByManagementNo(normalizedRequest.managementNo());
        mapper.deleteStudents(saved.achievementId());
        mapper.insertStudents(saved.achievementId(), normalizedRequest.students(), userId);
        mapper.insertChangeHistory(saved.managementNo(), existing == null ? "CREATE" : "UPDATE", userId, requestId);
        return mapper.findByManagementNo(saved.managementNo());
    }

    private List<ValidationError> validate(SaveDegreeCompletionAchievementRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request == null) {
            fields.add(new ValidationError("body", "석·박사 배출 실적 입력값이 필요합니다."));
            return fields;
        }
        if (blank(request.managementNo())) fields.add(new ValidationError("managementNo", "관리번호는 필수입니다."));
        if (blank(request.evaluationYear()) || !request.evaluationYear().trim().matches("\\d{4}")) fields.add(new ValidationError("evaluationYear", "평가연도는 YYYY 형식으로 입력하세요."));
        if (blank(request.managementItemCode())) fields.add(new ValidationError("managementItemCode", "관리항목은 필수입니다."));
        if (request.occurredDate() == null) fields.add(new ValidationError("occurredDate", "업적발생일은 필수입니다."));
        if (request.students() == null || request.students().isEmpty()) {
            fields.add(new ValidationError("students", "지도학생을 1명 이상 입력하세요."));
        } else {
            for (int index = 0; index < request.students().size(); index++) {
                DegreeCompletionStudentRequest student = request.students().get(index);
                String degreeType = student == null ? null : trim(student.degreeType());
                if (degreeType == null || !DEGREE_TYPES.contains(degreeType)) fields.add(new ValidationError("students[" + index + "].degreeType", "학위구분은 MASTER 또는 DOCTOR여야 합니다."));
                if (student == null || blank(student.studentName())) fields.add(new ValidationError("students[" + index + "].studentName", "학생명은 필수입니다."));
                if (student == null || blank(student.thesisTitle())) fields.add(new ValidationError("students[" + index + "].thesisTitle", "논문제목은 필수입니다."));
                if (student == null || student.degreeAwardedDate() == null) fields.add(new ValidationError("students[" + index + "].degreeAwardedDate", "학위수여일은 필수입니다."));
            }
        }
        return fields;
    }

    private SaveDegreeCompletionAchievementRequest normalize(SaveDegreeCompletionAchievementRequest request) {
        List<DegreeCompletionStudentRequest> students = request.students().stream()
                .map(student -> new DegreeCompletionStudentRequest(
                        trim(student.degreeType()),
                        student.studentName().trim(),
                        student.thesisTitle().trim(),
                        student.degreeAwardedDate()))
                .toList();
        return new SaveDegreeCompletionAchievementRequest(
                request.managementNo().trim(),
                request.evaluationYear().trim(),
                request.managementItemCode().trim(),
                request.occurredDate(),
                students);
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String trim(String value) { return value == null ? null : value.trim().toUpperCase(); }
}


