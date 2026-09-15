package kr.ac.knue.commonfoundation.basic65;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
public class TeachingAchievementService {
    private static final Pattern YEAR = Pattern.compile("^[0-9]{4}$");
    private static final int MAX_ATTACHMENTS = 10;
    private final TeachingAchievementMapper mapper;
    private final ObjectMapper objectMapper;

    public TeachingAchievementService(TeachingAchievementMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public TeachingAchievementSearchResponse list(TeachingAchievementSearchCriteria criteria, Long facultyUserId) {
        TeachingAchievementSearchCriteria normalized = new TeachingAchievementSearchCriteria(criteria.safePage(), criteria.safeSize(),
                trim(criteria.evaluationYear()), trim(criteria.academicYear()), trim(criteria.semester()), trim(criteria.courseKeyword()), trim(criteria.achievementStatus()));
        return new TeachingAchievementSearchResponse(mapper.list(normalized, facultyUserId), normalized.safePage(), normalized.safeSize(), mapper.count(normalized, facultyUserId));
    }

    @Transactional
    public TeachingAchievementRow save(SaveTeachingAchievementRequest request, Long facultyUserId, String requestId) {
        List<ValidationError> errors = validate(request);
        if (!errors.isEmpty()) throw new BusinessValidationException("강의실적 저장 요청이 올바르지 않습니다.", errors);
        TeachingAchievementRow existing = request.achievementId() == null ? null : mapper.findByIdAndFaculty(request.achievementId(), facultyUserId);
        if (request.achievementId() != null && existing == null) throw new ConflictException("수정할 강의실적을 찾을 수 없습니다.");
        if (existing != null && "EVALUATION_CONFIRMED".equals(existing.achievementStatus())) throw new CodedConflictException("CONFIRMED_DATA_LOCKED", "평가확정된 강의실적은 수정할 수 없습니다.");
        TeachingAchievementRow duplicate = mapper.findByUniqueKey(facultyUserId, trim(request.academicYear()), trim(request.semester()), trim(request.courseCode()));
        if (duplicate != null && (existing == null || !duplicate.achievementId().equals(existing.achievementId()))) throw new ConflictException("동일 강좌의 강의실적이 이미 존재합니다.");
        String dynamicFields = json(request.dynamicFields() == null ? Map.of() : new LinkedHashMap<>(request.dynamicFields()));
        String attachmentRefs = json(request.attachmentRefs() == null ? List.of() : request.attachmentRefs().stream().map(String::trim).toList());
        if (existing == null) {
            mapper.insert(request, facultyUserId, dynamicFields, attachmentRefs);
            existing = mapper.findByUniqueKey(facultyUserId, trim(request.academicYear()), trim(request.semester()), trim(request.courseCode()));
            mapper.insertStatusHistory(existing.achievementId(), facultyUserId, trim(request.changeReason()), requestId);
        } else {
            mapper.update(request, facultyUserId, dynamicFields, attachmentRefs);
        }
        return mapper.findByIdAndFaculty(existing.achievementId(), facultyUserId);
    }

    private List<ValidationError> validate(SaveTeachingAchievementRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        if (!YEAR.matcher(trim(request.evaluationYear())).matches()) errors.add(new ValidationError("evaluationYear", "평가연도는 YYYY 형식이어야 합니다."));
        if (!YEAR.matcher(trim(request.academicYear())).matches()) errors.add(new ValidationError("academicYear", "학사연도는 YYYY 형식이어야 합니다."));
        if (!hasText(request.semester())) errors.add(new ValidationError("semester", "학기를 입력하세요."));
        if (!hasText(request.courseCode())) errors.add(new ValidationError("courseCode", "강좌코드를 입력하세요."));
        if (!hasText(request.courseName())) errors.add(new ValidationError("courseName", "강좌명을 입력하세요."));
        if (!hasText(request.courseType())) errors.add(new ValidationError("courseType", "강좌유형을 입력하세요."));
        if (request.creditHours() == null || request.creditHours().compareTo(BigDecimal.ZERO) <= 0) errors.add(new ValidationError("creditHours", "학점시수는 0보다 커야 합니다."));
        if (!hasText(request.changeReason())) errors.add(new ValidationError("changeReason", "변경 사유를 입력하세요."));
        if (request.attachmentRefs() != null && (request.attachmentRefs().size() > MAX_ATTACHMENTS || request.attachmentRefs().stream().anyMatch(value -> !hasText(value)))) errors.add(new ValidationError("attachmentRefs", "첨부파일은 유효한 참조값 최대 10개까지 등록할 수 있습니다."));
        if (request.dynamicFields() != null && request.dynamicFields().entrySet().stream().anyMatch(entry -> !hasText(entry.getKey()) || entry.getValue() == null)) errors.add(new ValidationError("dynamicFields", "동적 입력 항목이 올바르지 않습니다."));
        return errors;
    }

    private String json(Object value) { try { return objectMapper.writeValueAsString(value); } catch (JsonProcessingException exception) { throw new IllegalArgumentException("강의실적 저장 데이터를 처리할 수 없습니다."); } }
    private boolean hasText(String value) { return value != null && !value.isBlank(); }
    private String trim(String value) { return value == null ? "" : value.trim(); }
}
