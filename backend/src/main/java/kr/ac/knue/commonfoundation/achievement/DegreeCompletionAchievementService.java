package kr.ac.knue.commonfoundation.achievement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates scoped, atomic degree-completion saves so a header and all graduate details are
 * persisted with the same shared mutation guards and audit history.
 */
@Service
public class DegreeCompletionAchievementService {
    private static final Set<String> DEGREE_TYPES = Set.of("MASTER", "DOCTORAL");

    private final DegreeCompletionAchievementMapper mapper;
    private final EducationAchievementAccessValidator accessValidator;

    public DegreeCompletionAchievementService(
            DegreeCompletionAchievementMapper mapper,
            EducationAchievementAccessValidator accessValidator
    ) {
        this.mapper = mapper;
        this.accessValidator = accessValidator;
    }

    /** Returns only persisted degree-completion rows within the caller's data scope. */
    @Transactional(readOnly = true)
    public DegreeCompletionAchievementSearchResponse list(
            DegreeCompletionAchievementSearchCriteria criteria,
            CurrentUser actor
    ) {
        requireWriter(actor);
        DegreeCompletionAchievementSearchCriteria normalized = normalize(criteria, actor);
        List<DegreeCompletionAchievementRow> rows = mapper.list(normalized)
                .stream()
                .map(this::withStudents)
                .toList();
        return new DegreeCompletionAchievementSearchResponse(
                rows,
                normalized.safePage(),
                normalized.safeSize(),
                mapper.count(normalized)
        );
    }

    /** Saves a header and its validated degree students as one transactional business operation. */
    @Transactional
    public DegreeCompletionAchievementRow save(
            SaveDegreeCompletionAchievementRequest request,
            CurrentUser actor
    ) {
        requireWriter(actor);
        validateStudents(request.students());
        String detail = text(request.achievementDetail());
        String reason = text(request.changeReason());
        if (request.achievementId() == null) {
            Long targetUserId = request.targetUserId() == null ? actor.userId() : request.targetUserId();
            String evaluationYear = year(request.evaluationYear(), request.occurredDate());
            String organizationCode = organization(request.organizationCode(), targetUserId);
            accessValidator.validateMutation(
                    actor,
                    new EducationAchievementMutationContext(
                            targetUserId,
                            evaluationYear,
                            organizationCode,
                            request.occurredDate(),
                            LocalDateTime.now()
                    )
            );
            String managementNo = "DC-" + evaluationYear + "-" + UUID.randomUUID();
            mapper.insertAchievement(
                    managementNo,
                    targetUserId,
                    evaluationYear,
                    organizationCode,
                    request.managementItemCode().trim(),
                    request.occurredDate(),
                    detail == null ? "{}" : detail,
                    text(request.attachmentRef()),
                    actor.userId(),
                    reason
            );
            DegreeCompletionAchievementHeaderRow saved = mapper.findByManagementNo(managementNo);
            insertStudents(saved.achievementId(), request.students(), actor.userId());
            mapper.insertChangeHistory(
                    saved.achievementId(),
                    "CREATE",
                    "achievement",
                    null,
                    detail,
                    actor.userId(),
                    reasonOrDefault(reason)
            );
            return withStudents(mapper.findById(saved.achievementId()));
        }

        DegreeCompletionAchievementHeaderRow current = mutable(request.achievementId());
        accessValidator.validateMutation(
                actor,
                new EducationAchievementMutationContext(
                        current.teacherUserId(),
                        current.evaluationYear(),
                        current.organizationCode(),
                        current.occurredDate(),
                        LocalDateTime.now()
                )
        );
        mapper.updateAchievement(
                current.achievementId(),
                request.managementItemCode().trim(),
                request.occurredDate(),
                detail == null ? "{}" : detail,
                text(request.attachmentRef()),
                actor.userId(),
                reason
        );
        mapper.deleteStudents(current.achievementId());
        insertStudents(current.achievementId(), request.students(), actor.userId());
        mapper.insertChangeHistory(
                current.achievementId(),
                "UPDATE",
                "achievement",
                current.achievementDetail(),
                detail,
                actor.userId(),
                reasonOrDefault(reason)
        );
        return withStudents(mapper.findById(current.achievementId()));
    }

    private DegreeCompletionAchievementSearchCriteria normalize(
            DegreeCompletionAchievementSearchCriteria criteria,
            CurrentUser actor
    ) {
        DegreeCompletionAchievementSearchCriteria source = criteria == null
                ? new DegreeCompletionAchievementSearchCriteria(0, 20, null, null, null, null, null)
                : criteria;
        return new DegreeCompletionAchievementSearchCriteria(
                source.page(),
                source.size(),
                text(source.managementNo()),
                text(source.teacherName()),
                text(source.certificationStatus()),
                actor.userId(),
                role(actor)
        );
    }

    private DegreeCompletionAchievementHeaderRow mutable(Long achievementId) {
        DegreeCompletionAchievementHeaderRow current = mapper.findById(achievementId);
        if (current == null) {
            throw new NotFoundException("석·박사 배출 실적을 찾을 수 없습니다.");
        }
        if ("EVALUATION_CONFIRMED".equals(current.certificationStatus())) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정된 실적은 변경할 수 없습니다.");
        }
        return current;
    }

    private void validateStudents(List<DegreeCompletionStudentRequest> students) {
        if (students == null || students.isEmpty()) {
            throw new BusinessValidationException(
                    "지도학생을 1명 이상 입력하세요.",
                    List.of(new ValidationError("students", "지도학생을 1명 이상 입력하세요."))
            );
        }
        for (DegreeCompletionStudentRequest student : students) {
            if (student == null || text(student.degreeType()) == null) {
                throw new BusinessValidationException(
                        "학위구분을 입력하세요.",
                        List.of(new ValidationError("degreeType", "학위구분을 입력하세요."))
                );
            }
            if (!DEGREE_TYPES.contains(student.degreeType().trim().toUpperCase())) {
                throw new BusinessValidationException(
                        "학위구분은 MASTER 또는 DOCTORAL이어야 합니다.",
                        List.of(new ValidationError("degreeType", "학위구분을 확인하세요."))
                );
            }
        }
    }

    private void insertStudents(
            Long achievementId,
            List<DegreeCompletionStudentRequest> students,
            Long userId
    ) {
        for (DegreeCompletionStudentRequest student : students) {
            mapper.insertStudent(
                    achievementId,
                    new DegreeCompletionStudentRequest(
                            student.degreeType().trim().toUpperCase(),
                            student.studentName().trim(),
                            text(student.thesisTitle()),
                            student.degreeAwardedDate()
                    ),
                    userId
            );
        }
    }

    private DegreeCompletionAchievementRow withStudents(DegreeCompletionAchievementHeaderRow header) {
        return new DegreeCompletionAchievementRow(
                header.achievementId(),
                header.managementNo(),
                header.teacherUserId(),
                header.teacherName(),
                header.evaluationYear(),
                header.organizationCode(),
                header.managementItemCode(),
                header.occurredDate(),
                header.achievementDetail(),
                header.certificationStatus(),
                header.attachmentPresent(),
                mapper.findStudents(header.achievementId()),
                header.createdAt(),
                header.updatedAt()
        );
    }

    private void requireWriter(CurrentUser actor) {
        if (actor == null || actor.roles() == null || actor.roles().stream().noneMatch(
                role -> role.equals("R01") || role.equals("R02") || role.equals("R04") || role.equals("R09")
        )) {
            throw new ForbiddenException();
        }
    }

    private String role(CurrentUser actor) {
        return actor.roles().contains("R04") || actor.roles().contains("R09") ? "R04" : actor.roles().contains("R02") ? "R02" : "R01";
    }

    private String year(String requested, LocalDate occurredDate) {
        String value = text(requested);
        return value == null ? String.valueOf(occurredDate.getYear()) : value;
    }

    private String organization(String requested, Long targetUserId) {
        String value = text(requested);
        return value == null ? mapper.findActiveOrganizationCode(targetUserId) : value.toUpperCase();
    }

    private String text(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String reasonOrDefault(String reason) {
        return reason == null ? "석·박사 배출 실적 처리" : reason;
    }
}
