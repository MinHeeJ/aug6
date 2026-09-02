package kr.ac.knue.commonfoundation.basic43;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObjectionOpinionService {
    private static final Set<String> DECISIONS = Set.of("ACCEPTED", "REJECTED", "NEEDS_REVIEW");
    private final ObjectionOpinionMapper mapper;

    public ObjectionOpinionService(ObjectionOpinionMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public ObjectionOpinionSearchResponse list(ObjectionOpinionSearchCriteria criteria) {
        ObjectionOpinionSearchCriteria normalized = criteria == null
                ? new ObjectionOpinionSearchCriteria(0, 20, null, null, null)
                : criteria;
        return new ObjectionOpinionSearchResponse(
                mapper.listObjectionOpinions(normalized),
                Math.max(normalized.page(), 0),
                normalized.safeSize(),
                mapper.countObjectionOpinions(normalized));
    }

    @Transactional
    public ObjectionOpinionRow transition(Long objectionId, ObjectionOpinionRequest request, Long userId) {
        List<ValidationError> fields = validate(objectionId, request);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("이의신청 의견 처리 요청이 올바르지 않습니다.", fields);
        }
        ObjectionOpinionRow current = mapper.findLatestByObjectionId(objectionId);
        if (current == null) {
            throw new NotFoundException("이의신청 의견 처리 대상을 찾을 수 없습니다.");
        }
        if (mapper.objectionScopeExists(objectionId, userId) == 0) {
            throw new ConflictException("이의신청 의견 처리 권한 또는 데이터 범위가 없습니다.");
        }
        String decisionResult = request.decisionResult().trim().toUpperCase();
        if ("REJECTED".equals(decisionResult) && mapper.rejectionReasonExists(request.reasonCode().trim()) == 0) {
            throw new BusinessValidationException("등록된 반려사유를 선택하세요.", List.of(new ValidationError("reasonCode", "기존 반려사유를 선택하세요.")));
        }
        return mapper.insertTransition(
                objectionId,
                current.evaluationYear(),
                current.applicantUserId(),
                current.applicantOpinionSnapshot(),
                current.objectionContentSnapshot(),
                request.reviewerOpinion().trim(),
                decisionResult,
                trimToNull(request.reasonCode()),
                userId,
                "이의신청 의견 처리");
    }

    private List<ValidationError> validate(Long objectionId, ObjectionOpinionRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (objectionId == null || objectionId <= 0) {
            fields.add(new ValidationError("targetId", "처리 대상을 선택하세요."));
        }
        if (request == null || request.decisionResult() == null || request.decisionResult().isBlank()) {
            fields.add(new ValidationError("decisionResult", "결정결과를 선택하세요."));
            return fields;
        }
        String decisionResult = request.decisionResult().trim().toUpperCase();
        if (!DECISIONS.contains(decisionResult)) {
            fields.add(new ValidationError("decisionResult", "ACCEPTED, REJECTED, NEEDS_REVIEW 중 하나를 선택하세요."));
        }
        if (request.reviewerOpinion() == null || request.reviewerOpinion().isBlank()) {
            fields.add(new ValidationError("reviewerOpinion", "검토자 의견을 입력하세요."));
        }
        if ("REJECTED".equals(decisionResult) && (request.reasonCode() == null || request.reasonCode().isBlank())) {
            fields.add(new ValidationError("reasonCode", "기각 사유를 선택하세요."));
        }
        return fields;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
