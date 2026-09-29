package kr.ac.knue.commonfoundation.studentguidance;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Saves and reads student-guidance records without bypassing the achievement confirmation lock. */
@Service
public class StudentGuidanceAchievementService {
    private static final Set<String> ROLES = Set.of("R01", "R02", "R04");
    private final StudentGuidanceAchievementMapper mapper;
    public StudentGuidanceAchievementService(StudentGuidanceAchievementMapper mapper) { this.mapper = mapper; }

    /** Returns persisted guidance rows using only allowed page sizes. */
    @Transactional(readOnly = true)
    public StudentGuidanceAchievementSearchResponse list(int page, int pageSize, CurrentUser user) {
        requireRole(user);
        int size = Set.of(20, 50, 100).contains(pageSize) ? pageSize : 20;
        int safePage = Math.max(0, page);
        List<StudentGuidanceAchievementRow> rows = mapper.list(size, safePage * size).stream()
                .map(row -> withStudents(row)).toList();
        return new StudentGuidanceAchievementSearchResponse(rows, safePage, size, mapper.count());
    }

    /** Persists the header and all guided students as one unit of work. */
    @Transactional
    public StudentGuidanceAchievementRow save(StudentGuidanceAchievementSaveRequest request, CurrentUser user) {
        requireRole(user);
        validate(request);
        StudentGuidanceAchievementRow before = request.getAchievementId() == null ? null : mapper.find(request.getAchievementId());
        if (request.getAchievementId() != null && before == null) throw new NotFoundException("학생지도 실적을 찾을 수 없습니다.");
        if (before != null && "EVALUATION_CONFIRMED".equals(before.certificationStatus())) throw new ConflictException("평가확정 실적은 수정할 수 없습니다.");
        if (request.getCertificationStatus() == null || request.getCertificationStatus().isBlank()) request.setCertificationStatus(before == null ? "DRAFTING" : before.certificationStatus());
        if (before == null) mapper.insert(request, String.valueOf(request.getOccurredDate().getYear()), user.userId()); else mapper.update(request, user.userId());
        mapper.deleteStudents(request.getAchievementId());
        for (StudentGuidanceStudentRequest student : request.getStudents()) mapper.insertStudent(request.getAchievementId(), student);
        StudentGuidanceAchievementRow saved = mapper.find(request.getAchievementId());
        if (saved == null) throw new IllegalStateException("저장한 학생지도 실적을 다시 조회할 수 없습니다.");
        return withStudents(saved);
    }

    private StudentGuidanceAchievementRow withStudents(StudentGuidanceAchievementRow row) {
        return new StudentGuidanceAchievementRow(row.achievementId(), row.managementNo(), row.teacherName(), row.managementItemCode(), row.organizationCode(), row.occurredDate(), row.guidanceStartDate(), row.guidanceEndDate(), row.studentCount(), row.achievementDetail(), row.certificationStatus(), row.attachmentRef(), row.updatedAt(), row.updatedBy(), mapper.findStudents(row.achievementId()));
    }
    private void requireRole(CurrentUser user) { if (user == null || user.roles() == null || user.roles().stream().noneMatch(ROLES::contains)) throw new ForbiddenException(); }
    private void validate(StudentGuidanceAchievementSaveRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        if (request == null) throw new BusinessValidationException("학생지도 저장 요청이 필요합니다.", errors);
        if (blank(request.getManagementItemCode())) errors.add(new ValidationError("managementItemCode", "관리항목 코드는 필수입니다."));
        if (blank(request.getOrganizationCode())) errors.add(new ValidationError("organizationCode", "소속 조직 코드는 필수입니다."));
        if (request.getOccurredDate() == null) errors.add(new ValidationError("occurredDate", "업적발생일은 필수입니다."));
        if (request.getGuidanceStartDate() == null) errors.add(new ValidationError("guidanceStartDate", "지도 시작일은 필수입니다."));
        if (request.getGuidanceEndDate() == null) errors.add(new ValidationError("guidanceEndDate", "지도 종료일은 필수입니다."));
        if (request.getGuidanceStartDate() != null && request.getGuidanceEndDate() != null && request.getGuidanceEndDate().isBefore(request.getGuidanceStartDate())) errors.add(new ValidationError("guidanceEndDate", "지도 종료일은 시작일보다 빠를 수 없습니다."));
        if (request.getStudents() == null || request.getStudents().isEmpty()) errors.add(new ValidationError("students", "지도학생을 1명 이상 입력하세요."));
        else for (int i = 0; i < request.getStudents().size(); i++) { StudentGuidanceStudentRequest student = request.getStudents().get(i); if (student == null || blank(student.getStudentNo())) errors.add(new ValidationError("students[" + i + "].studentNo", "학생번호는 필수입니다.")); if (student == null || blank(student.getStudentName())) errors.add(new ValidationError("students[" + i + "].studentName", "학생명은 필수입니다.")); }
        if (!errors.isEmpty()) throw new BusinessValidationException("학생지도 저장 요청이 올바르지 않습니다.", errors);
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }
}
