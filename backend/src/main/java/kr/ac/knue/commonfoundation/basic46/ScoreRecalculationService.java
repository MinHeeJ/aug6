package kr.ac.knue.commonfoundation.basic46;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ConflictException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScoreRecalculationService {
    private final ScoreRecalculationMapper mapper;

    public ScoreRecalculationService(ScoreRecalculationMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public ScoreRecalculationSearchResponse list(ScoreRecalculationSearchCriteria criteria) {
        ScoreRecalculationSearchCriteria normalized = criteria == null
                ? new ScoreRecalculationSearchCriteria(0, 20, null, null, null)
                : criteria;
        return new ScoreRecalculationSearchResponse(
                mapper.listScoreRecalculations(normalized),
                Math.max(normalized.page(), 0),
                normalized.safeSize(),
                mapper.countScoreRecalculations(normalized));
    }

    @Transactional
    public ScoreRecalculationResult create(ScoreRecalculationRequest request, Long requestedBy) {
        List<ValidationError> fields = validate(request);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("점수 재계산 요청이 올바르지 않습니다.", fields);
        }
        Long formulaVersionId = parseFormulaVersionId(request.formulaVersionId().trim(), fields);
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("점수 재계산 요청이 올바르지 않습니다.", fields);
        }
        ScoreFormulaSnapshot formula = mapper.findFormulaSnapshot(formulaVersionId, request.evaluationYear().trim());
        if (formula == null) {
            throw new ConflictException("선택한 평가연도에 사용할 수 있는 산식버전을 찾을 수 없습니다.");
        }

        ScoreRecalculationRequest normalizedRequest = new ScoreRecalculationRequest(
                request.evaluationYear().trim(), request.areaCode().trim(), request.targetUserId(),
                request.formulaVersionId().trim(), request.selectionReason().trim());
        String requestId = "REQ-B46-RECALC-" + UUID.randomUUID();
        String batchJobId = "B46-RECALC-" + UUID.randomUUID();
        List<ScoreRecalculationCandidate> candidates = mapper.listRecalculationCandidates(normalizedRequest);
        int plannedSuccess = 0;
        int plannedExcluded = 0;
        for (ScoreRecalculationCandidate candidate : candidates) {
            if (candidate.canRecalculate()) {
                plannedSuccess++;
            } else {
                plannedExcluded++;
            }
        }
        mapper.insertRecalculationBatchJob(batchJobId, normalizedRequest, formulaVersionId, candidates.size(),
                plannedSuccess, 0, plannedExcluded, requestedBy, requestId);

        int success = 0;
        int failure = 0;
        int excluded = 0;
        for (ScoreRecalculationCandidate candidate : candidates) {
            String targetRef = "EVALUATION-MATERIAL-" + candidate.evaluationMaterialId();
            if (!candidate.canRecalculate()) {
                excluded++;
                mapper.insertBatchJobItem(batchJobId, targetRef, "EXCLUDED", null, null,
                        candidate.excludedReason(), requestId, requestedBy);
                continue;
            }
            try {
                BigDecimal previousScore = nullToZero(candidate.currentScore());
                BigDecimal recalculatedScore = calculate(candidate, formula);
                int inserted = mapper.insertScoreGeneration(candidate, formulaVersionId, previousScore, recalculatedScore,
                        normalizedRequest.selectionReason(), batchJobId, requestId, requestedBy);
                if (inserted == 1) {
                    mapper.updateEvaluationMaterialScore(candidate.evaluationMaterialId(), recalculatedScore, batchJobId,
                            candidate.nextGenerationNo(), requestId, requestedBy);
                    success++;
                    mapper.insertBatchJobItem(batchJobId, targetRef, "SUCCESS", null, null, null, requestId, requestedBy);
                } else {
                    excluded++;
                    mapper.insertBatchJobItem(batchJobId, targetRef, "EXCLUDED", null, null,
                            "이미 같은 계산 세대가 존재하여 제외되었습니다.", requestId, requestedBy);
                }
            } catch (RuntimeException exception) {
                failure++;
                mapper.insertBatchJobItem(batchJobId, targetRef, "FAILURE", "B46_SCORE_RECALCULATION_ERROR",
                        "점수 재계산 중 오류가 발생했습니다.", null, requestId, requestedBy);
            }
        }
        mapper.updateBatchJobCounts(batchJobId, success, failure, excluded, requestedBy);
        return new ScoreRecalculationResult(batchJobId, normalizedRequest.evaluationYear(), normalizedRequest.areaCode(),
                normalizedRequest.targetUserId(), normalizedRequest.formulaVersionId(), candidates.size(), success, failure,
                excluded, requestId);
    }

    private List<ValidationError> validate(ScoreRecalculationRequest request) {
        List<ValidationError> fields = new ArrayList<>();
        if (request == null) {
            fields.add(new ValidationError("evaluationYear", "평가연도를 입력하세요."));
            fields.add(new ValidationError("areaCode", "평가영역을 입력하세요."));
            fields.add(new ValidationError("formulaVersionId", "산식버전을 입력하세요."));
            fields.add(new ValidationError("selectionReason", "선택 사유를 입력하세요."));
            return fields;
        }
        if (request.evaluationYear() == null || request.evaluationYear().isBlank()) {
            fields.add(new ValidationError("evaluationYear", "평가연도를 입력하세요."));
        } else if (!request.evaluationYear().trim().matches("^[0-9]{4}$")) {
            fields.add(new ValidationError("evaluationYear", "평가연도는 4자리 연도여야 합니다."));
        }
        if (request.areaCode() == null || request.areaCode().isBlank()) {
            fields.add(new ValidationError("areaCode", "평가영역을 입력하세요."));
        }
        if (request.formulaVersionId() == null || request.formulaVersionId().isBlank()) {
            fields.add(new ValidationError("formulaVersionId", "산식버전을 입력하세요."));
        }
        if (request.selectionReason() == null || request.selectionReason().isBlank()) {
            fields.add(new ValidationError("selectionReason", "선택 사유를 입력하세요."));
        }
        return fields;
    }

    private Long parseFormulaVersionId(String raw, List<ValidationError> fields) {
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException exception) {
            fields.add(new ValidationError("formulaVersionId", "산식버전은 숫자 식별자여야 합니다."));
            return null;
        }
    }

    private BigDecimal calculate(ScoreRecalculationCandidate candidate, ScoreFormulaSnapshot formula) {
        BigDecimal score = nullToZero(candidate.baseScore());
        if ("DISTRIBUTION_RATE".equals(formula.calculationType()) && candidate.distributionRate() != null) {
            score = score.multiply(candidate.distributionRate());
        }
        if (formula.lowerBoundScore() != null && score.compareTo(formula.lowerBoundScore()) < 0) {
            score = formula.lowerBoundScore();
        }
        if (("CAP".equals(formula.calculationType()) || formula.upperBoundScore() != null)
                && formula.upperBoundScore() != null
                && score.compareTo(formula.upperBoundScore()) > 0) {
            score = formula.upperBoundScore();
        }
        return score.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
