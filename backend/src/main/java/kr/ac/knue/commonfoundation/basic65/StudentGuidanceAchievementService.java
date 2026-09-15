package kr.ac.knue.commonfoundation.basic65;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.CodedConflictException;
import kr.ac.knue.commonfoundation.common.api.PayloadTooLargeException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class StudentGuidanceAchievementService {
    private static final Pattern YEAR = Pattern.compile("^[0-9]{4}$");
    private static final long MAX_UPLOAD_BYTES = 10L * 1024 * 1024;
    private final StudentGuidanceAchievementMapper mapper;
    private final ObjectMapper objectMapper;
    public StudentGuidanceAchievementService(StudentGuidanceAchievementMapper mapper, ObjectMapper objectMapper) { this.mapper = mapper; this.objectMapper = objectMapper; }

    @Transactional(readOnly = true)
    public StudentGuidanceAchievementSearchResponse list(StudentGuidanceAchievementSearchCriteria criteria, Long facultyUserId) {
        StudentGuidanceAchievementSearchCriteria safe = new StudentGuidanceAchievementSearchCriteria(criteria.safePage(), criteria.safeSize(), trim(criteria.evaluationYear()), trim(criteria.academicYear()), trim(criteria.semester()), trim(criteria.studentKeyword()), trim(criteria.guidanceType()), trim(criteria.achievementStatus()));
        return new StudentGuidanceAchievementSearchResponse(mapper.list(safe, facultyUserId), safe.safePage(), safe.safeSize(), mapper.count(safe, facultyUserId));
    }
    @Transactional
    public StudentGuidanceAchievementRow save(SaveStudentGuidanceAchievementRequest request, Long facultyUserId, String requestId) {
        validate(request);
        StudentGuidanceAchievementRow existing = request.achievementId() == null ? null : mapper.findByIdAndFaculty(request.achievementId(), facultyUserId);
        if (request.achievementId() != null && existing == null) throw new ConflictException("수정할 학생지도 실적을 찾을 수 없습니다.");
        if (existing != null && "EVALUATION_CONFIRMED".equals(existing.achievementStatus())) throw new CodedConflictException("CONFIRMED_DATA_LOCKED", "평가확정된 학생지도 실적은 수정할 수 없습니다.");
        StudentGuidanceAchievementRow duplicate = mapper.findDuplicate(facultyUserId, trim(request.studentNo()), trim(request.guidanceType()), request.guidanceDate().toString());
        if (duplicate != null && (existing == null || !duplicate.achievementId().equals(existing.achievementId()))) throw new ConflictException("동일 학생·지도유형·지도일자의 실적이 이미 존재합니다.");
        String dynamic = json(request.dynamicFields() == null ? Map.of() : request.dynamicFields());
        String attachments = json(request.attachmentRefs() == null ? List.of() : request.attachmentRefs());
        if (existing == null) { mapper.insert(request, facultyUserId, dynamic, attachments); existing = mapper.findDuplicate(facultyUserId, trim(request.studentNo()), trim(request.guidanceType()), request.guidanceDate().toString()); mapper.insertStatusHistory(existing.achievementId(), facultyUserId, trim(request.changeReason()), requestId); }
        else mapper.update(request, facultyUserId, dynamic, attachments);
        return mapper.findByIdAndFaculty(existing.achievementId(), facultyUserId);
    }
    @Transactional
    public StudentGuidanceExcelUploadResult upload(String templateId, MultipartFile file, Long facultyUserId, String requestId) {
        if (file == null || file.isEmpty()) throw new BusinessValidationException("학생지도 Excel 파일을 선택하세요.", List.of(new ValidationError("file", "파일은 필수입니다.")));
        if (file.getSize() > MAX_UPLOAD_BYTES) throw new PayloadTooLargeException("첨부파일 용량이 허용 한도를 초과했습니다.");
        if (file.getOriginalFilename() == null || !file.getOriginalFilename().toLowerCase().endsWith(".csv")) throw new BusinessValidationException("학생지도 Excel 파일은 CSV 양식으로 업로드하세요.", List.of(new ValidationError("file", "학생지도 업로드 양식(.csv)만 지원합니다.")));
        List<SaveStudentGuidanceAchievementRequest> rows = new ArrayList<>(); List<StudentGuidanceExcelRowError> errors = new ArrayList<>();
        try { String[] lines = new String(file.getBytes(), StandardCharsets.UTF_8).split("\\R");
            if (lines.length < 2 || !lines[0].contains("studentNo")) errors.add(new StudentGuidanceExcelRowError(1, "header", "INVALID_TEMPLATE", "학생지도 표준 양식을 사용하세요."));
            for (int i = 1; i < lines.length; i++) { if (lines[i].isBlank()) continue; String[] c = lines[i].split(",", -1); if (c.length < 8) { errors.add(new StudentGuidanceExcelRowError(i + 1, "row", "INVALID_ROW", "필수 열이 누락되었습니다.")); continue; }
                try { SaveStudentGuidanceAchievementRequest row = new SaveStudentGuidanceAchievementRequest(null, c[0].trim(), c[1].trim(), c[2].trim(), c[3].trim(), c[4].trim(), c[5].trim(), LocalDate.parse(c[6].trim()), c[7].trim(), null, Map.of(), List.of(), "Excel 일괄등록"); validate(row); rows.add(row); }
                catch (RuntimeException ex) { errors.add(new StudentGuidanceExcelRowError(i + 1, "row", "INVALID_VALUE", "필수값과 날짜 형식을 확인하세요.")); }
            }
        } catch (IOException ex) { throw new BusinessValidationException("학생지도 Excel 파일을 읽을 수 없습니다.", List.of(new ValidationError("file", "파일을 다시 업로드하세요."))); }
        String uploadId = "UP-" + UUID.randomUUID(); String name = file.getOriginalFilename().replace("/", "").replace("\\", "");
        if (!errors.isEmpty()) { mapper.insertUpload(uploadId, "upload-file-" + uploadId, name, facultyUserId, "REJECTED"); for (StudentGuidanceExcelRowError error : errors) mapper.insertUploadError(uploadId, error.rowNumber(), error.columnName(), error.errorCode(), error.errorReason()); mapper.upsertUploadHistory(uploadId, rows.size() + errors.size(), 0, errors.size(), 0, facultyUserId); return new StudentGuidanceExcelUploadResult(uploadId, "REJECTED", rows.size() + errors.size(), 0, errors.size(), 0, errors); }
        for (SaveStudentGuidanceAchievementRequest row : rows) save(row, facultyUserId, requestId);
        mapper.insertUpload(uploadId, "upload-file-" + uploadId, name, facultyUserId, "COMMITTED"); mapper.upsertUploadHistory(uploadId, rows.size(), rows.size(), 0, rows.size(), facultyUserId);
        return new StudentGuidanceExcelUploadResult(uploadId, "COMMITTED", rows.size(), rows.size(), 0, rows.size(), List.of());
    }
    private void validate(SaveStudentGuidanceAchievementRequest r) { List<ValidationError> e = new ArrayList<>(); if (r == null) e.add(new ValidationError("body", "요청 본문이 필요합니다.")); else { if (!YEAR.matcher(trim(r.evaluationYear())).matches()) e.add(new ValidationError("evaluationYear", "평가연도는 YYYY 형식이어야 합니다.")); if (!YEAR.matcher(trim(r.academicYear())).matches()) e.add(new ValidationError("academicYear", "학사연도는 YYYY 형식이어야 합니다.")); if (!has(r.semester())) e.add(new ValidationError("semester", "학기를 입력하세요.")); if (!has(r.studentNo())) e.add(new ValidationError("studentNo", "학번을 입력하세요.")); if (!has(r.studentName())) e.add(new ValidationError("studentName", "학생명을 입력하세요.")); if (!has(r.guidanceType())) e.add(new ValidationError("guidanceType", "지도유형을 입력하세요.")); if (r.guidanceDate() == null) e.add(new ValidationError("guidanceDate", "지도일자를 입력하세요.")); if (!has(r.guidanceContent())) e.add(new ValidationError("guidanceContent", "지도내용을 입력하세요.")); if (!has(r.changeReason())) e.add(new ValidationError("changeReason", "변경 사유를 입력하세요.")); } if (!e.isEmpty()) throw new BusinessValidationException("학생지도 실적 저장 요청이 올바르지 않습니다.", e); }
    private String json(Object value) { try { return objectMapper.writeValueAsString(value); } catch (JsonProcessingException ex) { throw new IllegalArgumentException("학생지도 저장 데이터를 처리할 수 없습니다."); } }
    private boolean has(String value) { return value != null && !value.isBlank(); } private String trim(String value) { return value == null ? "" : value.trim(); }
}
