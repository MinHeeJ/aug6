package kr.ac.knue.commonfoundation.degreecompletions;

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

/**
 * Persists degree-completion headers and student details through the education-achievement lifecycle.
 * Header/detail replacement and history evidence share one transaction so partial student rows cannot persist.
 */
@Service
public class DegreeCompletionAchievementService {
    private static final Set<String> ALLOWED_ROLES = Set.of("R01", "R02", "R04");
    private static final Set<Integer> PAGE_SIZES = Set.of(20, 50, 100);
    private final DegreeCompletionAchievementMapper mapper;

    public DegreeCompletionAchievementService(DegreeCompletionAchievementMapper mapper) { this.mapper = mapper; }

    /** Returns data-backed rows while delegating optional predicate construction to the MyBatis mapper. */
    @Transactional(readOnly = true)
    public DegreeCompletionAchievementSearchResponse list(DegreeCompletionAchievementSearchCriteria criteria, int page,
            int pageSize, CurrentUser user) {
        requireRole(user);
        int safePage = Math.max(0, page);
        int safeSize = PAGE_SIZES.contains(pageSize) ? pageSize : 20;
        DegreeCompletionAchievementSearchCriteria normalized = new DegreeCompletionAchievementSearchCriteria(
                text(criteria == null ? null : criteria.managementNo()), text(criteria == null ? null : criteria.teacherName()),
                text(criteria == null ? null : criteria.certificationStatus()));
        List<DegreeCompletionAchievementRow> rows = mapper.list(normalized, safeSize, safePage * safeSize).stream()
                .map(this::withStudents).toList();
        return new DegreeCompletionAchievementSearchResponse(rows, safePage, safeSize, mapper.count(normalized));
    }

    /** Saves the header and complete student sub-table, rejecting any mutation of an evaluation-confirmed row. */
    @Transactional
    public DegreeCompletionAchievementRow save(DegreeCompletionAchievementSaveRequest request, CurrentUser user) {
        requireRole(user);
        validate(request);
        DegreeCompletionAchievementRow before = request.getAchievementId() == null ? null : mapper.find(request.getAchievementId());
        if (request.getAchievementId() != null && before == null) throw new NotFoundException("석·박사 배출 실적을 찾을 수 없습니다.");
        if (before != null && "EVALUATION_CONFIRMED".equals(before.certificationStatus())) {
            throw new ConflictException("평가확정 실적은 수정하거나 삭제할 수 없습니다.");
        }
        if (text(request.getCertificationStatus()) == null) {
            request.setCertificationStatus(before == null ? "DRAFTING" : before.certificationStatus());
        }
        if (before == null) mapper.insert(request, String.valueOf(request.getOccurredDate().getYear()), user.userId());
        else mapper.update(request, user.userId());
        mapper.deleteStudents(request.getAchievementId());
        for (DegreeCompletionStudentRequest student : request.getStudents()) {
            mapper.insertStudent(request.getAchievementId(), student);
        }
        DegreeCompletionAchievementRow saved = mapper.find(request.getAchievementId());
        if (saved == null) throw new IllegalStateException("저장한 석·박사 배출 실적을 다시 조회할 수 없습니다.");
        mapper.insertChangeHistory(saved.achievementId(), before == null ? "CREATE" : "UPDATE",
                before == null ? null : before.achievementDetail(), saved.achievementDetail(), user.userId(),
                text(request.getChangeReason()) == null ? "석·박사 배출 실적 저장" : text(request.getChangeReason()));
        return withStudents(saved);
    }

    private DegreeCompletionAchievementRow withStudents(DegreeCompletionAchievementRow row) {
        return new DegreeCompletionAchievementRow(row.achievementId(), row.managementNo(), row.teacherName(),
                row.managementItemCode(), row.organizationCode(), row.occurredDate(), row.achievementDetail(),
                row.certificationStatus(), row.attachmentRef(), row.updatedAt(), row.updatedBy(),
                mapper.findStudents(row.achievementId()));
    }

    /** Enforces the operation's x-roles contract before records are read or mutated. */
    private void requireRole(CurrentUser user) {
        if (user == null || user.roles() == null || user.roles().stream().noneMatch(ALLOWED_ROLES::contains)) {
            throw new ForbiddenException();
        }
    }

    private void validate(DegreeCompletionAchievementSaveRequest request) {
        if (request == null) throw new BusinessValidationException("석·박사 배출 저장 요청이 필요합니다.", List.of());
        List<ValidationError> errors = new ArrayList<>();
        if (text(request.getManagementItemCode()) == null) errors.add(new ValidationError("managementItemCode", "관리항목 코드는 필수입니다."));
        if (text(request.getOrganizationCode()) == null) errors.add(new ValidationError("organizationCode", "소속 조직 코드는 필수입니다."));
        if (request.getOccurredDate() == null) errors.add(new ValidationError("occurredDate", "업적발생일은 필수입니다."));
        if (request.getStudents() == null || request.getStudents().isEmpty()) {
            errors.add(new ValidationError("students", "지도학생을 1명 이상 입력하세요."));
        } else {
            for (int index = 0; index < request.getStudents().size(); index++) validateStudent(request.getStudents().get(index), index, errors);
        }
        if (!errors.isEmpty()) throw new BusinessValidationException("석·박사 배출 저장 요청이 올바르지 않습니다.", errors);
    }

    private void validateStudent(DegreeCompletionStudentRequest student, int index, List<ValidationError> errors) {
        if (student == null || text(student.getDegreeType()) == null) errors.add(new ValidationError("degreeType", "학위구분은 필수입니다."));
        else if (!Set.of("MASTER", "DOCTORAL").contains(student.getDegreeType())) errors.add(new ValidationError("degreeType", "학위구분은 MASTER 또는 DOCTORAL이어야 합니다."));
        if (student == null || text(student.getStudentName()) == null) errors.add(new ValidationError("students[" + index + "].studentName", "학생명은 필수입니다."));
        if (student == null || text(student.getThesisTitle()) == null) errors.add(new ValidationError("students[" + index + "].thesisTitle", "논문 제목은 필수입니다."));
        if (student == null || student.getDegreeAwardedDate() == null) errors.add(new ValidationError("students[" + index + "].degreeAwardedDate", "학위 수여일은 필수입니다."));
    }

    private String text(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
