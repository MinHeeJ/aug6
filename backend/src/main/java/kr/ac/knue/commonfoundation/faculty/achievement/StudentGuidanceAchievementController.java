package kr.ac.knue.commonfoundation.faculty.achievement;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Provides the student-guidance save and list HTTP contract, including the repeated student detail
 * projection required by the education-achievement screen.
 */
@RestController
public class StudentGuidanceAchievementController {
    private final List<StudentGuidanceAchievementResponse> savedAchievements = new ArrayList<>();

    /**
     * Saves a draft student-guidance achievement after checking the individual-entry roles and
     * mandatory period and student fields. The request id remains visible in the standard envelope.
     */
    @PostMapping("/api/business/student-guidance-achievements")
    public synchronized ApiResponse<StudentGuidanceAchievementResponse> saveStudentGuidanceAchievement(
            @RequestBody StudentGuidanceAchievementRequest request,
            HttpServletRequest servletRequest,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        requireIndividualAchievementUser(servletRequest);
        validate(request);
        StudentGuidanceAchievementResponse saved = new StudentGuidanceAchievementResponse(
                request.managementNo().trim(),
                request.managementItemCode().trim(),
                request.occurredDate(),
                request.guidanceStartDate(),
                request.guidanceEndDate(),
                request.studentCount(),
                List.copyOf(request.students()),
                "DRAFT");
        savedAchievements.removeIf(item -> item.managementNo().equals(saved.managementNo()));
        savedAchievements.add(saved);
        return ApiResponse.ok(saved, requestId);
    }

    /**
     * Lists the matching student-guidance projection with the requested pagination metadata.
     */
    @GetMapping("/api/business/student-guidance-achievements")
    public synchronized ApiResponse<StudentGuidanceAchievementListResponse> listStudentGuidanceAchievements(
            HttpServletRequest servletRequest,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @RequestParam(value = "managementNo", required = false) String managementNo,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        requireIndividualAchievementUser(servletRequest);
        List<StudentGuidanceAchievementResponse> items = savedAchievements.stream()
                .filter(item -> managementNo == null || managementNo.isBlank() || item.managementNo().equals(managementNo.trim()))
                .toList();
        return ApiResponse.ok(new StudentGuidanceAchievementListResponse(items, Math.max(page, 0), normalizePageSize(pageSize), items.size()), requestId);
    }

    private void validate(StudentGuidanceAchievementRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request == null) {
            fields.add(new ValidationError("body", "학생지도 실적 입력값이 필요합니다."));
        } else {
            if (blank(request.managementNo())) fields.add(new ValidationError("managementNo", "관리번호는 필수입니다."));
            if (blank(request.managementItemCode())) fields.add(new ValidationError("managementItemCode", "관리항목은 필수입니다."));
            if (request.occurredDate() == null) fields.add(new ValidationError("occurredDate", "업적발생일은 필수입니다."));
            if (request.guidanceStartDate() == null) fields.add(new ValidationError("guidanceStartDate", "지도 시작일은 필수입니다."));
            if (request.guidanceEndDate() == null) fields.add(new ValidationError("guidanceEndDate", "지도 종료일은 필수입니다."));
            if (request.guidanceStartDate() != null && request.guidanceEndDate() != null
                    && request.guidanceEndDate().isBefore(request.guidanceStartDate())) {
                fields.add(new ValidationError("guidanceEndDate", "지도 종료일은 시작일보다 빠를 수 없습니다."));
            }
            if (request.studentCount() == null || request.studentCount() < 1) fields.add(new ValidationError("studentCount", "학생수는 1명 이상이어야 합니다."));
            if (request.students() == null || request.students().isEmpty()) fields.add(new ValidationError("students", "지도학생을 1명 이상 입력하세요."));
            else {
                if (request.studentCount() != null && request.studentCount() != request.students().size()) {
                    fields.add(new ValidationError("studentCount", "학생수는 지도학생 상세 건수와 일치해야 합니다."));
                }
                for (int index = 0; index < request.students().size(); index++) {
                    StudentGuidanceStudent student = request.students().get(index);
                    if (student == null || blank(student.studentNo())) fields.add(new ValidationError("students[" + index + "].studentNo", "학번은 필수입니다."));
                    if (student == null || blank(student.studentName())) fields.add(new ValidationError("students[" + index + "].studentName", "학생명은 필수입니다."));
                }
            }
        }
        if (!fields.isEmpty()) throw new BusinessValidationException("학생지도 실적 입력값이 올바르지 않습니다.", fields);
    }

    private void requireIndividualAchievementUser(HttpServletRequest servletRequest) {
        Object user = servletRequest.getAttribute("currentUser");
        if (!(user instanceof CurrentUser currentUser)) throw new UnauthenticatedException();
        if (currentUser.roles().stream().noneMatch(role -> List.of("R01", "R02", "R04").contains(role))) throw new ForbiddenException();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private int normalizePageSize(int pageSize) {
        return List.of(20, 50, 100).contains(pageSize) ? pageSize : 20;
    }

    /** Request payload for the student-guidance header and its student detail rows. */
    public record StudentGuidanceAchievementRequest(
            String managementNo,
            String managementItemCode,
            LocalDate occurredDate,
            LocalDate guidanceStartDate,
            LocalDate guidanceEndDate,
            Integer studentCount,
            List<StudentGuidanceStudent> students) {
    }

    /** Student detail visible on the saved achievement and list row. */
    public record StudentGuidanceStudent(String studentNo, String studentName) {
    }

    /** Saved student-guidance header plus student detail projection. */
    public record StudentGuidanceAchievementResponse(
            String managementNo,
            String managementItemCode,
            LocalDate occurredDate,
            LocalDate guidanceStartDate,
            LocalDate guidanceEndDate,
            Integer studentCount,
            List<StudentGuidanceStudent> students,
            String certificationStatus) {
    }

    /** Pagination envelope for student-guidance search results. */
    public record StudentGuidanceAchievementListResponse(
            List<StudentGuidanceAchievementResponse> items,
            int page,
            int pageSize,
            long totalElements) {
    }
}
