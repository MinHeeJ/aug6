package kr.ac.knue.commonfoundation.basic65;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeachingEvaluationAchievementService {
    private static final Pattern YEAR = Pattern.compile("^[0-9]{4}$");
    private static final int MAX_ATTACHMENTS = 10;
    private final TeachingEvaluationAchievementMapper mapper;
    private final ObjectMapper objectMapper;

    public TeachingEvaluationAchievementService(TeachingEvaluationAchievementMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public TeachingEvaluationAchievementSearchResponse list(TeachingEvaluationAchievementSearchCriteria criteria, Long facultyUserId) {
        TeachingEvaluationAchievementSearchCriteria normalized = new TeachingEvaluationAchievementSearchCriteria(
                criteria.safePage(), criteria.safeSize(), trim(criteria.evaluationYear()), trim(criteria.academicYear()),
                trim(criteria.semester()), trim(criteria.courseKeyword()), trim(criteria.evaluationStatus()));
        return new TeachingEvaluationAchievementSearchResponse(
                mapper.list(normalized, facultyUserId), normalized.safePage(), normalized.safeSize(), mapper.count(normalized, facultyUserId));
    }

    @Transactional
    public TeachingEvaluationAchievementRow save(SaveTeachingEvaluationAchievementRequest request, Long facultyUserId, String requestId) {
        List<ValidationError> errors = validate(request);
        if (!errors.isEmpty()) {
            throw new BusinessValidationException("강의평가 실적 저장 요청이 올바르지 않습니다.", errors);
        }
        TeachingEvaluationAchievementRow existing = mapper.findByUniqueKey(
                facultyUserId, request.academicYear().trim(), request.semester().trim(), request.courseCode().trim());
        if (existing != null) {
            throw new ConflictException("동일 강좌의 강의평가 실적이 이미 존재합니다.");
        }
        String dynamicFieldsJson = json(request.dynamicFields() == null ? Map.of() : new LinkedHashMap<>(request.dynamicFields()));
        String attachmentRefsJson = json(request.attachmentRefs() == null ? List.of() : request.attachmentRefs().stream().map(String::trim).toList());
        mapper.insert(request, facultyUserId, dynamicFieldsJson, attachmentRefsJson);
        TeachingEvaluationAchievementRow saved = mapper.findByUniqueKey(
                facultyUserId, request.academicYear().trim(), request.semester().trim(), request.courseCode().trim());
        mapper.insertStatusHistory(saved.achievementId(), facultyUserId, request.changeReason().trim(), requestId);
        return mapper.findById(saved.achievementId());
    }

    private List<ValidationError> validate(SaveTeachingEvaluationAchievementRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        if (!YEAR.matcher(trim(request.evaluationYear())).matches()) errors.add(new ValidationError("evaluationYear", "평가연도는 YYYY 형식이어야 합니다."));
        if (!YEAR.matcher(trim(request.academicYear())).matches()) errors.add(new ValidationError("academicYear", "학사연도는 YYYY 형식이어야 합니다."));
        if (!hasText(request.semester())) errors.add(new ValidationError("semester", "학기를 입력하세요."));
        if (!hasText(request.courseCode())) errors.add(new ValidationError("courseCode", "강좌코드를 입력하세요."));
        if (!hasText(request.courseName())) errors.add(new ValidationError("courseName", "강좌명을 입력하세요."));
        if (request.evaluationScore() == null || request.evaluationScore().signum() < 0 || request.evaluationScore().compareTo(java.math.BigDecimal.valueOf(100)) > 0) errors.add(new ValidationError("evaluationScore", "강의평가 점수는 0에서 100 사이여야 합니다."));
        if (!hasText(request.changeReason())) errors.add(new ValidationError("changeReason", "변경 사유를 입력하세요."));
        if (request.attachmentRefs() != null && request.attachmentRefs().size() > MAX_ATTACHMENTS) errors.add(new ValidationError("attachmentRefs", "첨부파일은 최대 10개까지 등록할 수 있습니다."));
        if (request.attachmentRefs() != null && request.attachmentRefs().stream().anyMatch(value -> !hasText(value))) errors.add(new ValidationError("attachmentRefs", "첨부 참조값이 올바르지 않습니다."));
        if (request.dynamicFields() != null && request.dynamicFields().entrySet().stream().anyMatch(entry -> !hasText(entry.getKey()) || entry.getValue() == null)) errors.add(new ValidationError("dynamicFields", "동적 입력 항목이 올바르지 않습니다."));
        return errors;
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("강의평가 저장 데이터를 처리할 수 없습니다.");
        }
    }

    private boolean hasText(String value) { return value != null && !value.isBlank(); }
    private String trim(String value) { return value == null ? "" : value.trim(); }
}
