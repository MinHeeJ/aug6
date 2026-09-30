package kr.ac.knue.commonfoundation.achievement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates an authorized degree-completion header and all guided-student details in one transaction. */
@Service
public class DegreeCompletionAchievementService {
    private static final Set<String> DEGREE_TYPES = Set.of("MASTER", "DOCTOR");
    private final DegreeCompletionAchievementMapper mapper;
    private final ObjectMapper objectMapper;

    public DegreeCompletionAchievementService(DegreeCompletionAchievementMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    /** Returns only rows visible under the persisted faculty-achievement data scope. */
    @Transactional(readOnly = true)
    public DegreeCompletionAchievementResponse.Search list(DegreeCompletionAchievementSearchCriteria criteria,
            CurrentUser user) {
        requireReadUser(user);
        List<DegreeCompletionAchievementResponse.Row> rows = mapper.findAll(criteria, user.userId()).stream()
                .map(this::response).toList();
        return new DegreeCompletionAchievementResponse.Search(rows, criteria.page(), criteria.pageSize(),
                mapper.countAll(criteria, user.userId()));
    }

    /**
     * Saves the header, replaces its detail set, records an allowed status transition, and writes field audit evidence.
     * A confirmed header is rejected before any child or audit mutation to preserve final-evaluation immutability.
     */
    @Transactional
    public DegreeCompletionAchievementResponse.Row save(DegreeCompletionAchievementRequest request, CurrentUser user,
            String requestId) {
        requireFaculty(user);
        validateStudents(request.students());
        DegreeCompletionAchievementData current = request.achievementId() == null ? null
                : mapper.findById(request.achievementId());
        if (current != null && current.getCertificationStatus() == EducationAchievementStatus.EVALUATION_CONFIRMED) {
            throw new ConflictException("CONFIRMED_DATA_LOCKED: 평가확정 데이터는 변경할 수 없습니다.");
        }

        LocalDate occurredDate = request.occurredDate() == null ? earliestAwardedDate(request.students())
                : request.occurredDate();
        String evaluationYear = current == null ? String.valueOf(occurredDate.getYear()) : current.getEvaluationYear();
        String organizationCode = current == null ? organizationFor(user.userId()) : current.getOrganizationCode();
        Long ownerUserId = current == null ? user.userId() : current.getTeacherUserId();
        EducationAchievementValidationResult validation = new EducationAchievementValidationService(mapper)
                .validateMutation(new EducationAchievementValidationContext(user, ownerUserId, evaluationYear,
                        organizationCode, null, occurredDate));

        DegreeCompletionAchievementData row = current == null ? new DegreeCompletionAchievementData() : current;
        String before = current == null ? null : snapshot(current);
        if (current == null) {
            row.setManagementNo("DC-" + UUID.randomUUID());
            row.setEvaluationYear(evaluationYear);
            row.setOrganizationCode(organizationCode);
            row.setTeacherUserId(user.userId());
            row.setCertificationStatus(EducationAchievementStatus.DRAFTING);
        }
        row.setManagementItemCode(request.managementItemCode().trim());
        row.setOccurredDate(occurredDate);
        row.setAchievementDetail(json(request.achievementDetail()));
        row.setAttachmentCount(request.attachmentCount() == null ? current == null ? 0 : current.getAttachmentCount()
                : request.attachmentCount());
        row.setOccurrenceDateWarning(validation.occurrenceDateWarning());

        if (current == null) mapper.insert(row); else mapper.update(row);
        if (request.nextStatus() != null && request.nextStatus() != row.getCertificationStatus()) {
            new EducationAchievementStatusTransitionService(mapper, java.time.Clock.systemUTC()).transition(
                    "DEGREE_COMPLETION", row.getAchievementId(), row.getCertificationStatus(), request.nextStatus(),
                    request.transitionReason(), user.userId(), requestId);
            row.setCertificationStatus(request.nextStatus());
            mapper.update(row);
        }
        mapper.deleteStudents(row.getAchievementId());
        for (DegreeCompletionStudentRequest student : request.students()) {
            mapper.insertStudent(row.getAchievementId(), student, user.userId());
        }
        mapper.insertChangeHistory(row.getAchievementId(), current == null ? "CREATE" : "UPDATE", before,
                snapshot(row), user.userId(), normalizeReason(request.changeReason()), requestId);
        return response(mapper.findById(row.getAchievementId()));
    }

    private void validateStudents(List<DegreeCompletionStudentRequest> students) {
        if (students == null || students.isEmpty()) {
            throw validation("students", "지도학생을 한 명 이상 입력하세요.");
        }
        for (DegreeCompletionStudentRequest student : students) {
            if (student == null || student.degreeType() == null || student.degreeType().isBlank()) {
                throw validation("degreeType", "학위구분을 입력하세요.");
            }
            if (!DEGREE_TYPES.contains(student.degreeType().trim())) {
                throw validation("degreeType", "학위구분은 MASTER 또는 DOCTOR여야 합니다.");
            }
            if (student.studentName() == null || student.studentName().isBlank()) throw validation("studentName", "학생명을 입력하세요.");
            if (student.thesisTitle() == null || student.thesisTitle().isBlank()) throw validation("thesisTitle", "논문 제목을 입력하세요.");
            if (student.degreeAwardedDate() == null) throw validation("degreeAwardedDate", "학위수여일을 입력하세요.");
        }
    }

    private LocalDate earliestAwardedDate(List<DegreeCompletionStudentRequest> students) {
        return students.stream().map(DegreeCompletionStudentRequest::degreeAwardedDate).min(LocalDate::compareTo)
                .orElseThrow(() -> validation("degreeAwardedDate", "학위수여일을 입력하세요."));
    }

    private String organizationFor(Long userId) {
        String organizationCode = mapper.findOrganizationCodeForUser(userId);
        if (organizationCode == null || organizationCode.isBlank()) {
            throw validation("organizationCode", "평가조직 매핑을 먼저 설정하세요.");
        }
        return organizationCode;
    }

    private void requireFaculty(CurrentUser user) {
        if (user == null || user.roles() == null || user.roles().stream()
                .noneMatch(role -> Set.of("R01", "R02", "R04").contains(role))) throw new ForbiddenException();
    }

    /** System administrators may inspect records but cannot use the faculty mutation workflow. */
    private void requireReadUser(CurrentUser user) {
        if (user == null || user.roles() == null || user.roles().stream()
                .noneMatch(role -> Set.of("R01", "R02", "R04", "R09").contains(role))) throw new ForbiddenException();
    }

    private String json(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node == null ? objectMapper.createObjectNode() : node);
        } catch (Exception exception) {
            throw validation("achievementDetail", "상세값 형식이 올바르지 않습니다.");
        }
    }

    private String snapshot(DegreeCompletionAchievementData row) {
        try {
            return objectMapper.writeValueAsString(java.util.Map.of("managementItemCode", row.getManagementItemCode(),
                    "occurredDate", row.getOccurredDate().toString(), "achievementDetail", row.getAchievementDetail()));
        } catch (Exception exception) {
            throw new IllegalStateException("변경 이력 값을 생성할 수 없습니다.", exception);
        }
    }

    private String normalizeReason(String value) {
        return value == null || value.isBlank() ? "석·박사 배출 실적 저장" : value.trim();
    }

    private BusinessValidationException validation(String field, String message) {
        return new BusinessValidationException("석·박사 배출 입력값이 올바르지 않습니다.",
                List.of(new ValidationError(field, message)));
    }

    private DegreeCompletionAchievementResponse.Row response(DegreeCompletionAchievementData row) {
        try {
            return new DegreeCompletionAchievementResponse.Row(row.getAchievementId(), row.getManagementNo(),
                    row.getEvaluationYear(), row.getOrganizationCode(), row.getTeacherUserId(), row.getTeacherName(),
                    row.getManagementItemCode(), row.getOccurredDate(), row.getCertificationStatus(),
                    objectMapper.readTree(row.getAchievementDetail()), row.getAttachmentCount(),
                    row.isOccurrenceDateWarning(), row.getStudents() == null ? java.util.Collections.emptyList() : row.getStudents());
        } catch (Exception exception) {
            throw new IllegalStateException("저장된 석·박사 배출 상세값을 읽을 수 없습니다.", exception);
        }
    }
}
