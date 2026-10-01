package kr.ac.knue.commonfoundation.basic81;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns guarded, atomic degree-completion header and student-detail persistence.
 * Detail replacement occurs only after finalization and data-scope guards pass.
 */
@Service
public class DegreeCompletionAchievementService {
    private final DegreeCompletionAchievementMapper mapper;
    private final EducationAchievementGuardService guardService;

    public DegreeCompletionAchievementService(
            DegreeCompletionAchievementMapper mapper,
            EducationAchievementGuardService guardService) {
        this.mapper = mapper;
        this.guardService = guardService;
    }

    /** Lists caller-scoped degree-completion achievements with their persisted student details. */
    @Transactional(readOnly = true)
    public DegreeCompletionAchievementSearchResponse list(
            DegreeCompletionAchievementSearchCriteria criteria,
            CurrentUser requester) {
        requireAuthorizedUser(requester);
        DegreeCompletionAchievementSearchCriteria safeCriteria = criteria == null
                ? new DegreeCompletionAchievementSearchCriteria(0, 20, null, null, null)
                : criteria;
        List<DegreeCompletionAchievementRow> rows = mapper.list(
                safeCriteria,
                requester.userId(),
                requester.roles()).stream().map(this::materialize).toList();
        return new DegreeCompletionAchievementSearchResponse(
                rows,
                safeCriteria.safePage(),
                safeCriteria.safePageSize(),
                mapper.count(safeCriteria, requester.userId(), requester.roles()));
    }

    /**
     * Saves the header and complete student collection in one transaction, while
     * preserving the shared lifecycle and data-change history side effects.
     */
    @Transactional
    public DegreeCompletionAchievementRow save(
            SaveDegreeCompletionAchievementRequest request,
            CurrentUser requester,
            String requestId) {
        validate(request);
        requireAuthorizedUser(requester);
        DegreeCompletionAchievementHeaderRow existing = request.achievementId() == null
                ? null
                : findExisting(request.achievementId());
        LocalDate occurredDate = request.students().stream()
                .map(DegreeCompletionStudentRequest::degreeAwardedDate)
                .min(LocalDate::compareTo)
                .orElseThrow();
        Long targetUserId = existing == null ? requester.userId() : existing.targetUserId();
        String evaluationYear = existing == null
                ? String.valueOf(Year.from(occurredDate))
                : existing.evaluationYear();
        guardService.validateMutation(
                requester,
                new EducationAchievementMutationContext(targetUserId, evaluationYear, occurredDate));
        if (existing == null) {
            return create(request, requester, requestId, occurredDate, evaluationYear);
        }
        return update(request, requester, requestId, occurredDate, existing);
    }

    private DegreeCompletionAchievementRow create(
            SaveDegreeCompletionAchievementRequest request,
            CurrentUser requester,
            String requestId,
            LocalDate occurredDate,
            String evaluationYear) {
        String managementNo = "DC-" + UUID.randomUUID();
        mapper.insertHeader(
                managementNo,
                requester.userId(),
                evaluationYear,
                request.managementItemCode().trim(),
                occurredDate,
                studentDetailSummary(request.students()),
                blankToNull(request.attachmentRef()),
                requester.userId());
        DegreeCompletionAchievementHeaderRow saved = mapper.findHeaderByManagementNo(managementNo);
        if (saved == null) {
            throw new NotFoundException("저장한 석·박사 배출 실적을 찾을 수 없습니다.");
        }
        saveStudents(saved.achievementId(), request.students(), requester.userId());
        mapper.insertStatusHistory(new EducationAchievementStatusHistory(
                "DEGREE_COMPLETION",
                saved.achievementId(),
                null,
                EducationAchievementStatus.DRAFT,
                "CREATE",
                null,
                "석·박사 배출 실적 최초 입력",
                requester.userId(),
                LocalDateTime.now()));
        mapper.insertChangeHistory(
                "degree_completion_achievements",
                String.valueOf(saved.achievementId()),
                "CREATE",
                "student_count",
                null,
                String.valueOf(request.students().size()),
                requester.userId(),
                "석·박사 배출 실적 저장",
                requestId);
        return materialize(findExisting(saved.achievementId()));
    }

    private DegreeCompletionAchievementRow update(
            SaveDegreeCompletionAchievementRequest request,
            CurrentUser requester,
            String requestId,
            LocalDate occurredDate,
            DegreeCompletionAchievementHeaderRow existing) {
        int previousStudentCount = mapper.findStudents(existing.achievementId()).size();
        mapper.updateHeader(
                existing.achievementId(),
                request.managementItemCode().trim(),
                occurredDate,
                studentDetailSummary(request.students()),
                blankToNull(request.attachmentRef()),
                requester.userId());
        mapper.deleteStudents(existing.achievementId());
        saveStudents(existing.achievementId(), request.students(), requester.userId());
        mapper.insertChangeHistory(
                "degree_completion_achievements",
                String.valueOf(existing.achievementId()),
                "UPDATE",
                "student_count",
                String.valueOf(previousStudentCount),
                String.valueOf(request.students().size()),
                requester.userId(),
                "석·박사 배출 실적 수정",
                requestId);
        return materialize(findExisting(existing.achievementId()));
    }

    private void saveStudents(
            Long achievementId,
            List<DegreeCompletionStudentRequest> students,
            Long userId) {
        for (DegreeCompletionStudentRequest student : students) {
            mapper.insertStudent(achievementId, student, userId);
        }
    }

    private DegreeCompletionAchievementHeaderRow findExisting(Long achievementId) {
        DegreeCompletionAchievementHeaderRow existing = mapper.findHeader(achievementId);
        if (existing == null) {
            throw new NotFoundException("석·박사 배출 실적을 찾을 수 없습니다.");
        }
        return existing;
    }

    private DegreeCompletionAchievementRow materialize(DegreeCompletionAchievementHeaderRow header) {
        return new DegreeCompletionAchievementRow(
                header.achievementId(),
                header.managementNo(),
                header.targetUserId(),
                header.teacherName(),
                header.evaluationYear(),
                header.managementItemCode(),
                header.occurredDate(),
                header.achievementDetail(),
                header.certificationStatus(),
                header.attachmentRef(),
                mapper.findStudents(header.achievementId()),
                header.createdAt(),
                header.updatedAt());
    }

    private void validate(SaveDegreeCompletionAchievementRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        if (request == null) {
            errors.add(new ValidationError("body", "석·박사 배출 실적 정보를 입력하세요."));
        } else {
            if (blankToNull(request.managementItemCode()) == null) {
                errors.add(new ValidationError("managementItemCode", "관리항목을 입력하세요."));
            }
            if (request.students() == null || request.students().isEmpty()) {
                errors.add(new ValidationError("students", "지도학생을 1명 이상 입력하세요."));
            } else {
                for (DegreeCompletionStudentRequest student : request.students()) {
                    if (student == null || blankToNull(student.degreeType()) == null) {
                        errors.add(new ValidationError("degreeType", "학위구분을 입력하세요."));
                    } else if (!List.of("MASTER", "DOCTORAL").contains(student.degreeType().trim())) {
                        errors.add(new ValidationError("degreeType", "학위구분은 MASTER 또는 DOCTORAL이어야 합니다."));
                    }
                    if (student == null || blankToNull(student.studentName()) == null) {
                        errors.add(new ValidationError("studentName", "학생명을 입력하세요."));
                    }
                    if (student == null || blankToNull(student.thesisTitle()) == null) {
                        errors.add(new ValidationError("thesisTitle", "논문 제목을 입력하세요."));
                    }
                    if (student == null || student.degreeAwardedDate() == null) {
                        errors.add(new ValidationError("degreeAwardedDate", "학위수여일을 입력하세요."));
                    }
                }
            }
        }
        if (!errors.isEmpty()) {
            throw new BusinessValidationException("석·박사 배출 저장 요청이 올바르지 않습니다.", errors);
        }
    }

    private void requireAuthorizedUser(CurrentUser requester) {
        if (requester == null || requester.userId() == null || requester.roles() == null
                || requester.roles().stream().noneMatch(role -> List.of("R01", "R02", "R04").contains(role))) {
            throw new ForbiddenException();
        }
    }

    private String studentDetailSummary(List<DegreeCompletionStudentRequest> students) {
        return "학생 수: " + students.size();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
