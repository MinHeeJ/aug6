package kr.ac.knue.commonfoundation.achievement;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Saves a student-guidance header and every student detail atomically after common guards run. */
@Service
public class StudentGuidanceAchievementService {
    private final StudentGuidanceAchievementMapper mapper;
    private final EducationAchievementGuard guard;
    private final ObjectMapper objectMapper;

    public StudentGuidanceAchievementService(StudentGuidanceAchievementMapper mapper, EducationAchievementGuard guard, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.guard = guard;
        this.objectMapper = objectMapper;
    }

    /** Validates and persists one FR-027 request; a rejected detail prevents any header mutation. */
    @Transactional
    public StudentGuidanceAchievementMapper.StudentGuidanceRow save(StudentGuidanceSaveRequest request, CurrentUser user) {
        validate(request);
        Long teacherUserId = user.userId();
        String organizationCode = mapper.findOrganizationCodeForUser(teacherUserId);
        String evaluationYear = String.valueOf(request.getGuidanceStartDate().getYear());
        StudentGuidanceAchievementMapper.StudentGuidanceRow before = request.getAchievementId() == null ? null : mapper.findById(request.getAchievementId());
        if (request.getAchievementId() != null && before == null) throw new NotFoundException("수정할 학생지도 실적을 찾을 수 없습니다.");
        if (before != null) {
            teacherUserId = before.teacherUserId();
            organizationCode = before.organizationCode();
            evaluationYear = before.evaluationYear();
        }
        guard.validateMutation(user, new EducationAchievementCommandContext(teacherUserId, evaluationYear, organizationCode, request.getGuidanceStartDate(), before == null ? "DRAFT" : before.certificationStatus()));
        if (before == null) mapper.insert(request, evaluationYear, teacherUserId, organizationCode, request.getStudents().size(), user.userId());
        else {
            mapper.update(request, request.getStudents().size(), user.userId());
            mapper.deleteStudents(request.getAchievementId());
        }
        String details = serialize(request.getStudents());
        for (Object student : request.getStudents()) mapper.insertStudent(request.getAchievementId(), serialize(student), user.userId());
        mapper.insertChangeHistory(request.getAchievementId(), before == null ? "CREATE" : "UPDATE", details, user.userId(), before == null ? "학생지도 등록" : "학생지도 수정");
        StudentGuidanceAchievementMapper.StudentGuidanceRow saved = mapper.findById(request.getAchievementId());
        if (saved == null) throw new NotFoundException("저장한 학생지도 실적을 재조회하지 못했습니다.");
        return saved;
    }

    private void validate(StudentGuidanceSaveRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request == null || blank(request.getManagementItemCode())) fields.add(new ValidationError("managementItemCode", "관리항목을 선택하세요."));
        if (request == null || request.getGuidanceStartDate() == null) fields.add(new ValidationError("guidanceStartDate", "지도 시작일을 입력하세요."));
        if (request == null || request.getGuidanceEndDate() == null) fields.add(new ValidationError("guidanceEndDate", "지도 종료일을 입력하세요."));
        if (request != null && request.getGuidanceStartDate() != null && request.getGuidanceEndDate() != null && request.getGuidanceEndDate().isBefore(request.getGuidanceStartDate())) fields.add(new ValidationError("guidanceEndDate", "지도 종료일은 시작일보다 빠를 수 없습니다."));
        if (request == null || request.getStudents() == null || request.getStudents().isEmpty()) fields.add(new ValidationError("students", "지도학생을 1명 이상 입력하세요."));
        if (!fields.isEmpty()) throw new BusinessValidationException("학생지도 저장 요청이 올바르지 않습니다.", fields);
    }
    private String serialize(Object value) { try { return objectMapper.writeValueAsString(value); } catch (Exception exception) { throw new BusinessValidationException("학생 상세 입력값 형식이 올바르지 않습니다.", List.of(new ValidationError("students", "학생 상세 입력값을 확인하세요."))); } }
    private boolean blank(String value) { return value == null || value.isBlank(); }
}
