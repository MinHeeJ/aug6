package kr.ac.knue.commonfoundation.basic71;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Applies ownership, role, lifecycle, persistence, and audit invariants for education achievements. */
@Service
public class EducationAchievementService {
    private final EducationAchievementMapper mapper;
    private final ObjectMapper objectMapper;

    public EducationAchievementService(EducationAchievementMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    /** Lists only records the current role can inspect; faculty members are restricted to their own records. */
    public EducationAchievementSearchResponse list(EducationAchievementSearchCriteria criteria, CurrentUser user) {
        Long viewer = user.roles().contains("R01") ? user.userId() : null;
        List<EducationAchievementRow> rows = mapper.list(criteria, viewer).stream().map(this::hydrate).toList();
        return new EducationAchievementSearchResponse(rows, criteria.safePage(), criteria.safeSize(), mapper.count(criteria, viewer));
    }

    /** Creates a draft achievement and its dependent records atomically, then writes the CREATE audit event. */
    @Transactional
    public EducationAchievementRow create(SaveEducationAchievementRequest request, Long teacherUserId, String requestId) {
        String type = request.achievementType().trim().toUpperCase();
        if (!validType(type)) throw new IllegalArgumentException("지원하지 않는 교육영역 실적유형입니다.");
        String details = toJson(request.details());
        EducationAchievementRow saved = mapper.insert(request, String.valueOf(Year.now().getValue()), details, teacherUserId);
        List<String> attachments = request.attachmentFileTokens() == null ? List.of() : request.attachmentFileTokens();
        for (int index = 0; index < attachments.size(); index++) mapper.insertAttachment(saved.achievementId(), attachments.get(index), index + 1);
        List<GraduateDegreeCompletionStudentRequest> students = request.degreeCompletionStudents() == null ? List.of() : request.degreeCompletionStudents();
        for (int index = 0; index < students.size(); index++) mapper.insertDegreeStudent(saved.achievementId(), students.get(index), index + 1);
        mapper.insertChangeHistory(saved.achievementId(), "CREATE", null, "DRAFT", teacherUserId, requestId);
        return hydrate(saved);
    }

    /** Retrieves a single record after enforcing faculty ownership and reviewer visibility. */
    public EducationAchievementRow get(Long achievementId, CurrentUser user) {
        EducationAchievementRow row = required(achievementId);
        assertVisible(row, user);
        return hydrate(row);
    }

    /** Performs only the approved lifecycle transitions and records an immutable status/audit trail. */
    @Transactional
    public EducationAchievementRow transition(Long achievementId, EducationAchievementTransitionRequest request, CurrentUser user, String requestId) {
        EducationAchievementRow current = required(achievementId);
        String action = request.actionType().trim().toUpperCase();
        String next = nextStatus(current.certificationStatus(), action, user);
        if (mapper.updateStatus(achievementId, current.certificationStatus(), next, user.userId()) != 1) {
            throw new ConflictException("INVALID_STATE_TRANSITION", "다른 사용자가 실적 상태를 변경했습니다.");
        }
        mapper.insertStatusHistory(achievementId, current.certificationStatus(), next, action, user.userId(), request.reasonCode(), request.opinion(), requestId);
        mapper.insertChangeHistory(achievementId, "STATUS_TRANSITION", current.certificationStatus(), next, user.userId(), requestId);
        return hydrate(required(achievementId));
    }

    /** Validates uploaded student-guidance rows before any source achievement is created. */
    @Transactional
    public StudentGuidanceUploadResult validateStudentGuidanceUpload(MultipartFile file, Long uploaderUserId, String requestId) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("업로드 파일을 선택하세요.");
        if (file.getSize() > 10 * 1024 * 1024) throw new IllegalArgumentException("파일 크기는 10MB 이하여야 합니다.");
        String uploadId = UUID.randomUUID().toString();
        mapper.insertExcelUploadFile(uploadId, requestId, file.getOriginalFilename() == null ? "학생지도-업로드" : file.getOriginalFilename(), uploaderUserId);
        List<StudentGuidanceUploadError> errors = new ArrayList<>();
        int total = 0;
        try {
            String[] lines = new String(file.getBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n").split("\n");
            if (lines.length < 2 || !lines[0].contains("managementItemCode")) throw new IllegalArgumentException("학생지도 현행 양식의 managementItemCode 헤더가 필요합니다.");
            for (int index = 1; index < lines.length; index++) {
                if (lines[index].isBlank()) continue;
                total++;
                String[] columns = lines[index].split(",", -1);
                StudentGuidanceUploadError error = columns.length == 0 || columns[0].isBlank()
                        ? new StudentGuidanceUploadError(index + 1, "managementItemCode", "관리항목코드는 필수입니다.") : null;
                if (error == null && mapper.educationAchievementExists(uploaderUserId, columns[0].trim(), java.time.LocalDate.now()) > 0) {
                    error = new StudentGuidanceUploadError(index + 1, "managementItemCode", "중복 데이터는 반영할 수 없습니다.");
                }
                mapper.insertStudentGuidanceStaging(uploadId, index + 1, lines[index], error == null ? "VALID" : "ERROR");
                if (error != null) { mapper.insertStudentGuidanceError(uploadId, error); errors.add(error); }
            }
        } catch (IOException exception) { throw new IllegalArgumentException("업로드 파일을 읽을 수 없습니다."); }
        // Staging rows are persisted; this response must not create source achievements.
        return new StudentGuidanceUploadResult(uploadId, total, total - errors.size(), errors.size(), errors);
    }

    private EducationAchievementRow hydrate(EducationAchievementRow row) { return row.withChildren(mapper.listAttachmentFileTokens(row.achievementId()), mapper.listDegreeStudents(row.achievementId())); }
    private EducationAchievementRow required(Long achievementId) { EducationAchievementRow row = mapper.find(achievementId); if (row == null || row.deleted()) throw new NotFoundException("교육영역 실적을 찾을 수 없습니다."); return row; }
    private void assertVisible(EducationAchievementRow row, CurrentUser user) { if (user.roles().contains("R01") && !row.teacherUserId().equals(user.userId())) throw new ForbiddenException(); }
    private String firstRole(CurrentUser user) { return user.roles().isEmpty() ? "" : user.roles().get(0); }
    private boolean validType(String value) { return List.of("LECTURE_EVALUATION", "LECTURE_PERFORMANCE", "STUDENT_GUIDANCE", "GRADUATE_DEGREE_COMPLETION").contains(value); }
    private String toJson(Object value) { try { return value == null ? "{}" : objectMapper.writeValueAsString(value); } catch (JsonProcessingException exception) { throw new IllegalArgumentException("실적 상세정보를 처리할 수 없습니다."); } }
    private String nextStatus(String current, String action, CurrentUser user) {
        if ("DRAFT".equals(current) && "SUBMIT".equals(action) && user.roles().contains("R01")) return "SUBMITTED";
        if ("SUBMITTED".equals(current) && "DEPARTMENT_CONFIRM".equals(action) && user.roles().contains("R02")) return "DEPARTMENT_CONFIRMED";
        if ("SUBMITTED".equals(current) && "DEPARTMENT_REJECT".equals(action) && user.roles().contains("R02")) return "DEPARTMENT_REJECTED";
        if ("DEPARTMENT_REJECTED".equals(current) && "SUBMIT".equals(action) && user.roles().contains("R01")) return "SUBMITTED";
        if ("DEPARTMENT_CONFIRMED".equals(current) && "CERTIFY".equals(action) && user.roles().contains("R04")) return "CERTIFIED";
        if ("DEPARTMENT_CONFIRMED".equals(current) && "CERTIFICATION_REJECT".equals(action) && user.roles().contains("R04")) return "CERTIFICATION_REJECTED";
        if ("CERTIFICATION_REJECTED".equals(current) && "SUBMIT".equals(action) && user.roles().contains("R01")) return "SUBMITTED";
        throw new ConflictException("INVALID_STATE_TRANSITION", "허용되지 않은 상태 전이입니다.");
    }
}
