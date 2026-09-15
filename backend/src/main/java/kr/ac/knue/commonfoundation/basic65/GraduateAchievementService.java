package kr.ac.knue.commonfoundation.basic65;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.CodedConflictException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GraduateAchievementService {
    private static final Pattern YEAR = Pattern.compile("^[0-9]{4}$");
    private static final int MAX_ATTACHMENTS = 10;
    private final GraduateAchievementMapper mapper;
    private final ObjectMapper objectMapper;

    public GraduateAchievementService(GraduateAchievementMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public GraduateAchievementSearchResponse list(GraduateAchievementSearchCriteria criteria, Long facultyUserId) {
        GraduateAchievementSearchCriteria safe = new GraduateAchievementSearchCriteria(criteria.safePage(), criteria.safeSize(),
                trim(criteria.evaluationYear()), trim(criteria.academicYear()), trim(criteria.semester()), trim(criteria.studentKeyword()),
                trim(criteria.degreeType()), trim(criteria.achievementStatus()));
        return new GraduateAchievementSearchResponse(mapper.list(safe, facultyUserId), safe.safePage(), safe.safeSize(), mapper.count(safe, facultyUserId));
    }

    @Transactional
    public GraduateAchievementRow save(SaveGraduateAchievementRequest request, Long facultyUserId, String requestId) {
        List<ValidationError> errors = validate(request);
        if (!errors.isEmpty()) throw new BusinessValidationException("석·박사 배출 실적 저장 요청이 올바르지 않습니다.", errors);
        GraduateAchievementRow existing = request.achievementId() == null ? null : mapper.findByIdAndFaculty(request.achievementId(), facultyUserId);
        if (request.achievementId() != null && existing == null) throw new ConflictException("수정할 석·박사 배출 실적을 찾을 수 없습니다.");
        if (existing != null && "EVALUATION_CONFIRMED".equals(existing.achievementStatus())) throw new CodedConflictException("CONFIRMED_DATA_LOCKED", "평가확정된 석·박사 배출 실적은 수정할 수 없습니다.");
        GraduateAchievementRow duplicate = mapper.findDuplicate(facultyUserId, trim(request.studentNo()), trim(request.degreeType()), request.awardDate().toString());
        if (duplicate != null && (existing == null || !duplicate.achievementId().equals(existing.achievementId()))) throw new ConflictException("동일 학생·학위구분·수여일의 석·박사 배출 실적이 이미 존재합니다.");
        String dynamicFields = json(request.dynamicFields() == null ? Map.of() : request.dynamicFields());
        String attachmentRefs = json(request.attachmentRefs() == null ? List.of() : request.attachmentRefs());
        String before = existing == null ? "{}" : json(existing);
        if (existing == null) {
            mapper.insert(request, facultyUserId, dynamicFields, attachmentRefs);
            existing = mapper.findDuplicate(facultyUserId, trim(request.studentNo()), trim(request.degreeType()), request.awardDate().toString());
            mapper.insertStatusHistory(existing.achievementId(), facultyUserId, trim(request.changeReason()), requestId);
        } else {
            mapper.update(request, facultyUserId, dynamicFields, attachmentRefs);
        }
        GraduateAchievementRow saved = mapper.findByIdAndFaculty(existing.achievementId(), facultyUserId);
        mapper.insertChangeHistory(saved.achievementId(), existing.achievementId().equals(saved.achievementId()) && request.achievementId() == null ? "CREATE" : "UPDATE", before, json(saved), facultyUserId, trim(request.changeReason()), requestId);
        return saved;
    }

    private List<ValidationError> validate(SaveGraduateAchievementRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        if (request == null) { errors.add(new ValidationError("body", "요청 본문이 필요합니다.")); return errors; }
        if (!YEAR.matcher(trim(request.evaluationYear())).matches()) errors.add(new ValidationError("evaluationYear", "평가연도는 YYYY 형식이어야 합니다."));
        if (!YEAR.matcher(trim(request.academicYear())).matches()) errors.add(new ValidationError("academicYear", "학사연도는 YYYY 형식이어야 합니다."));
        if (!has(request.semester())) errors.add(new ValidationError("semester", "학기를 입력하세요."));
        if (!has(request.studentNo())) errors.add(new ValidationError("studentNo", "학번을 입력하세요."));
        if (!has(request.studentName())) errors.add(new ValidationError("studentName", "학생명을 입력하세요."));
        if (!"MASTER".equals(trim(request.degreeType())) && !"DOCTOR".equals(trim(request.degreeType()))) errors.add(new ValidationError("degreeType", "학위구분은 MASTER 또는 DOCTOR여야 합니다."));
        if (!has(request.thesisTitle())) errors.add(new ValidationError("thesisTitle", "논문제목을 입력하세요."));
        if (request.awardDate() == null) errors.add(new ValidationError("awardDate", "수여일을 입력하세요."));
        if (!has(request.changeReason())) errors.add(new ValidationError("changeReason", "변경 사유를 입력하세요."));
        if (request.attachmentRefs() != null && request.attachmentRefs().size() > MAX_ATTACHMENTS) errors.add(new ValidationError("attachmentRefs", "첨부파일은 최대 10개까지 등록할 수 있습니다."));
        return errors;
    }

    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalArgumentException("석·박사 배출 저장 데이터를 처리할 수 없습니다."); }
    }
    private boolean has(String value) { return value != null && !value.isBlank(); }
    private String trim(String value) { return value == null ? "" : value.trim(); }
}
