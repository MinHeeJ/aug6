package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates scoped individual student-guidance saves while preserving header, details, and audit history. */
@Service
public class StudentGuidanceAchievementService {
    private final StudentGuidanceAchievementMapper mapper;
    private final EducationAchievementAccessValidator accessValidator;

    public StudentGuidanceAchievementService(StudentGuidanceAchievementMapper mapper, EducationAchievementAccessValidator accessValidator) {
        this.mapper = mapper;
        this.accessValidator = accessValidator;
    }

    /** Lists only rows within the caller's existing education-achievement scope. */
    @Transactional(readOnly = true)
    public StudentGuidanceAchievementSearchResponse list(StudentGuidanceAchievementSearchCriteria criteria, CurrentUser actor) {
        requireWriter(actor);
        StudentGuidanceAchievementSearchCriteria source = criteria == null ? new StudentGuidanceAchievementSearchCriteria(0, 20, null, null, null, null, null, null, actor.userId(), role(actor)) : criteria;
        StudentGuidanceAchievementSearchCriteria normalized = new StudentGuidanceAchievementSearchCriteria(source.page(), source.size(), text(source.managementNo()), text(source.teacherName()), text(source.managementItemCode()), source.guidanceDateFrom(), source.guidanceDateTo(), text(source.certificationStatus()), actor.userId(), role(actor));
        return new StudentGuidanceAchievementSearchResponse(mapper.list(normalized), normalized.safePage(), normalized.safeSize(), mapper.count(normalized));
    }

    /** Saves a header and replaces its student details in one transaction after shared mutation guards pass. */
    @Transactional
    public StudentGuidanceAchievementRow save(SaveStudentGuidanceAchievementRequest request, CurrentUser actor) {
        requireWriter(actor);
        if (request.guidanceEndDate().isBefore(request.guidanceStartDate())) throw new BusinessValidationException("학생지도 기간이 올바르지 않습니다.", List.of(new ValidationError("guidanceEndDate", "종료일은 시작일 이후여야 합니다.")));
        if (request.achievementId() == null) {
            Long targetUserId = request.targetUserId() == null ? actor.userId() : request.targetUserId();
            String year = text(request.evaluationYear()) == null ? String.valueOf(request.guidanceStartDate().getYear()) : text(request.evaluationYear());
            String organization = text(request.organizationCode()) == null ? mapper.findActiveOrganizationCode(targetUserId) : text(request.organizationCode());
            accessValidator.validateMutation(actor, new EducationAchievementMutationContext(targetUserId, year, organization, request.guidanceStartDate(), LocalDateTime.now()));
            String managementNo = "SG-" + year + "-" + UUID.randomUUID();
            mapper.insertAchievement(managementNo, targetUserId, year, organization, request.managementItemCode().trim(), request.guidanceStartDate(), request.guidanceEndDate(), text(request.attachmentRef()), actor.userId(), text(request.changeReason()));
            StudentGuidanceAchievementRow saved = mapper.list(new StudentGuidanceAchievementSearchCriteria(0, 20, managementNo, null, null, null, null, null, actor.userId(), "R04")).get(0);
            insertStudents(saved.achievementId(), request.students(), actor.userId());
            mapper.insertChangeHistory(saved.achievementId(), "CREATE", null, request.managementItemCode().trim(), actor.userId(), reason(request.changeReason()));
            return mapper.findById(saved.achievementId());
        }
        StudentGuidanceAchievementRow current = mutable(request.achievementId());
        accessValidator.validateMutation(actor, new EducationAchievementMutationContext(current.teacherUserId(), current.evaluationYear(), current.organizationCode(), current.guidanceStartDate(), LocalDateTime.now()));
        mapper.updateAchievement(current.achievementId(), request.managementItemCode().trim(), request.guidanceStartDate(), request.guidanceEndDate(), text(request.attachmentRef()), actor.userId(), text(request.changeReason()));
        mapper.deleteStudents(current.achievementId());
        insertStudents(current.achievementId(), request.students(), actor.userId());
        mapper.insertChangeHistory(current.achievementId(), "UPDATE", current.managementItemCode(), request.managementItemCode().trim(), actor.userId(), reason(request.changeReason()));
        return mapper.findById(current.achievementId());
    }

    private void insertStudents(Long achievementId, List<StudentGuidanceStudentRequest> students, Long userId) {
        for (StudentGuidanceStudentRequest student : students) mapper.insertStudent(achievementId, student, userId);
    }
    private StudentGuidanceAchievementRow mutable(Long id) {
        StudentGuidanceAchievementRow row = mapper.findById(id);
        if (row == null) throw new NotFoundException("학생지도 실적을 찾을 수 없습니다.");
        if ("EVALUATION_CONFIRMED".equals(row.certificationStatus())) throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정된 실적은 변경할 수 없습니다.");
        return row;
    }
    private void requireWriter(CurrentUser actor) { if (actor == null || actor.roles().stream().noneMatch(value -> value.equals("R01") || value.equals("R02") || value.equals("R04") || value.equals("R09"))) throw new ForbiddenException(); }
    private String role(CurrentUser actor) { return actor.roles().contains("R04") || actor.roles().contains("R09") ? "R04" : actor.roles().contains("R02") ? "R02" : "R01"; }
    private String text(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String reason(String value) { return text(value) == null ? "학생지도 실적 처리" : text(value); }
}
