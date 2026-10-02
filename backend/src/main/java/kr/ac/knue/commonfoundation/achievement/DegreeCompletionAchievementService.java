package kr.ac.knue.commonfoundation.achievement;

import com.fasterxml.jackson.core.JsonProcessingException;
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

/**
 * Coordinates the FR-028 degree-completion header and 지도학생 rows so audit history and details
 * are committed atomically after the shared authorization, period, scope, and lock checks.
 */
@Service
public class DegreeCompletionAchievementService {
    private final DegreeCompletionAchievementMapper mapper;
    private final EducationAchievementGuard guard;
    private final ObjectMapper objectMapper;

    public DegreeCompletionAchievementService(
            DegreeCompletionAchievementMapper mapper,
            EducationAchievementGuard guard,
            ObjectMapper objectMapper
    ) {
        this.mapper = mapper;
        this.guard = guard;
        this.objectMapper = objectMapper;
    }

    /** Returns the current principal's scoped degree-completion rows and their recipient details. */
    @Transactional(readOnly = true)
    public DegreeCompletionAchievementModels.SearchResponse list(
            DegreeCompletionAchievementModels.SearchCriteria criteria,
            CurrentUser user
    ) {
        DegreeCompletionAchievementModels.SearchCriteria requested = criteria == null
                ? new DegreeCompletionAchievementModels.SearchCriteria(0, 20, null, null, null)
                : criteria;
        DegreeCompletionAchievementModels.SearchCriteria normalized =
                new DegreeCompletionAchievementModels.SearchCriteria(
                        Math.max(requested.page(), 0),
                        requested.safeSize(),
                        requested.managementNo(),
                        requested.teacherName(),
                        requested.certificationStatus()
                );
        boolean managerScope = user.roles().contains("R02") || user.roles().contains("R04");
        List<DegreeCompletionAchievementModels.Row> rows = mapper.list(
                normalized,
                user.userId(),
                managerScope
        ).stream().map(header -> row(header, false)).toList();
        return new DegreeCompletionAchievementModels.SearchResponse(
                rows,
                normalized.page(),
                normalized.safeSize(),
                mapper.count(normalized, user.userId(), managerScope)
        );
    }

    /** Saves one header and replaces its detail rows only after all business guards have passed. */
    @Transactional
    public DegreeCompletionAchievementModels.Row save(DegreeCompletionSaveRequest request, CurrentUser user) {
        validate(request);
        DegreeCompletionAchievementModels.Header before = request.getAchievementId() == null
                ? null
                : mapper.findById(request.getAchievementId());
        if (request.getAchievementId() != null && before == null) {
            throw new NotFoundException("수정할 석·박사 배출 실적을 찾을 수 없습니다.");
        }
        normalizeOwnership(request, user, before);
        LocalDate occurredDate = earliestAwardDate(request.getStudents());
        EducationAchievementGuardResult guardResult = guard.validateMutation(
                user,
                new EducationAchievementCommandContext(
                        request.getTeacherUserId(),
                        request.getEvaluationYear(),
                        request.getOrganizationCode(),
                        occurredDate,
                        before == null ? "DRAFT" : before.certificationStatus()
                )
        );
        EducationAchievementTransition transition = resolveTransition(before, request);
        String nextStatus = transition == null
                ? (before == null ? "DRAFT" : before.certificationStatus())
                : transition.nextStatus();
        String achievementDetail = serializeAchievementDetail(request);
        String details = serialize(request.getStudents());
        String beforeDetails = before == null ? null : serialize(mapper.findStudents(before.achievementId()));
        if (before == null) {
            mapper.insert(request, occurredDate, achievementDetail, nextStatus, user.userId());
        } else {
            mapper.update(request, occurredDate, achievementDetail, nextStatus, user.userId());
            mapper.deleteStudents(request.getAchievementId());
        }
        for (DegreeCompletionSaveRequest.StudentRequest student : request.getStudents()) {
            mapper.insertStudent(request.getAchievementId(), student, user.userId());
        }
        if (transition != null || before == null) {
            mapper.insertStatusHistory(
                    request.getAchievementId(),
                    before == null ? null : before.certificationStatus(),
                    nextStatus,
                    transition == null ? "CREATE" : transition.actionType(),
                    transition == null ? null : transition.reasonCode(),
                    transition == null ? null : transition.opinion(),
                    user.userId()
            );
        }
        mapper.insertChangeHistory(
                request.getAchievementId(),
                before == null ? "CREATE" : "UPDATE",
                beforeDetails,
                details,
                user.userId(),
                before == null ? "석·박사 배출 실적 등록" : "석·박사 배출 실적 수정"
        );
        DegreeCompletionAchievementModels.Header saved = mapper.findById(request.getAchievementId());
        if (saved == null) {
            throw new NotFoundException("저장한 석·박사 배출 실적을 재조회하지 못했습니다.");
        }
        return row(saved, guardResult.occurredDateOutOfRangeWarning());
    }

    private DegreeCompletionAchievementModels.Row row(
            DegreeCompletionAchievementModels.Header header,
            boolean warning
    ) {
        return DegreeCompletionAchievementModels.Row.from(
                header,
                mapper.findStudents(header.achievementId()),
                warning
        );
    }

    private void normalizeOwnership(
            DegreeCompletionSaveRequest request,
            CurrentUser user,
            DegreeCompletionAchievementModels.Header before
    ) {
        if (before != null) {
            request.setTeacherUserId(before.teacherUserId());
            request.setOrganizationCode(before.organizationCode());
            request.setEvaluationYear(before.evaluationYear());
            return;
        }
        if (request.getTeacherUserId() == null) {
            request.setTeacherUserId(user.userId());
        }
        if (blank(request.getOrganizationCode())) {
            request.setOrganizationCode(mapper.findOrganizationCodeForUser(request.getTeacherUserId()));
        }
        if (blank(request.getEvaluationYear())) {
            request.setEvaluationYear(String.valueOf(earliestAwardDate(request.getStudents()).getYear()));
        }
    }

    private EducationAchievementTransition resolveTransition(
            DegreeCompletionAchievementModels.Header before,
            DegreeCompletionSaveRequest request
    ) {
        if (blank(request.getActionType())) {
            return null;
        }
        return guard.validateTransition(
                before == null ? "DRAFT" : before.certificationStatus(),
                request.getActionType(),
                request.getReasonCode(),
                request.getOpinion()
        );
    }

    private void validate(DegreeCompletionSaveRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request == null || blank(request.getManagementItemCode())) {
            fields.add(new ValidationError("managementItemCode", "관리항목을 선택하세요."));
        }
        if (request == null || request.getStudents() == null || request.getStudents().isEmpty()) {
            fields.add(new ValidationError("students", "지도학생을 1명 이상 입력하세요."));
        } else {
            for (DegreeCompletionSaveRequest.StudentRequest student : request.getStudents()) {
                if (student == null || blank(student.getDegreeType())) {
                    fields.add(new ValidationError("degreeType", "학위구분을 입력하세요."));
                } else if (!"MASTER".equals(student.getDegreeType())
                        && !"DOCTOR".equals(student.getDegreeType())) {
                    fields.add(new ValidationError("degreeType", "학위구분은 MASTER 또는 DOCTOR여야 합니다."));
                }
                if (student == null || blank(student.getStudentName())) {
                    fields.add(new ValidationError("studentName", "학생명을 입력하세요."));
                }
                if (student == null || blank(student.getThesisTitle())) {
                    fields.add(new ValidationError("thesisTitle", "논문 제목을 입력하세요."));
                }
                if (student == null || student.getDegreeAwardedDate() == null) {
                    fields.add(new ValidationError("degreeAwardedDate", "학위수여일을 입력하세요."));
                }
            }
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("석·박사 배출 실적 저장 요청이 올바르지 않습니다.", fields);
        }
    }

    private LocalDate earliestAwardDate(List<DegreeCompletionSaveRequest.StudentRequest> students) {
        return students.stream()
                .map(DegreeCompletionSaveRequest.StudentRequest::getDegreeAwardedDate)
                .min(LocalDate::compareTo)
                .orElseThrow();
    }

    private String serializeAchievementDetail(DegreeCompletionSaveRequest request) {
        try {
            return request.getAchievementDetail() == null
                    ? "{}"
                    : objectMapper.writeValueAsString(request.getAchievementDetail());
        } catch (JsonProcessingException exception) {
            throw new BusinessValidationException(
                    "석·박사 배출 상세 입력값을 처리할 수 없습니다.",
                    List.of(new ValidationError("achievementDetail", "상세 입력값 형식이 올바르지 않습니다."))
            );
        }
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessValidationException(
                    "석·박사 배출 상세를 처리할 수 없습니다.",
                    List.of(new ValidationError("students", "지도학생 상세 형식이 올바르지 않습니다."))
            );
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
