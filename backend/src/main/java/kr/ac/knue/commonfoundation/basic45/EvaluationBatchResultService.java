package kr.ac.knue.commonfoundation.basic45;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvaluationBatchResultService {
    private final EvaluationBatchResultMapper mapper;

    public EvaluationBatchResultService(EvaluationBatchResultMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public EvaluationBatchResultListResponse list(EvaluationBatchResultSearchCriteria criteria) {
        EvaluationBatchResultSearchCriteria normalized = criteria == null
                ? new EvaluationBatchResultSearchCriteria(0, 20, null, null, null)
                : criteria;
        return new EvaluationBatchResultListResponse(
                mapper.listResults(normalized),
                Math.max(normalized.page(), 0),
                normalized.safeSize(),
                mapper.countResults(normalized));
    }

    @Transactional(readOnly = true)
    public EvaluationBatchResultErrorListResponse listErrors(String batchId, int page, int size) {
        EvaluationBatchResultSearchCriteria paging = new EvaluationBatchResultSearchCriteria(page, size, null, null, null);
        String normalizedBatchId = batchId == null ? "" : batchId.trim();
        return new EvaluationBatchResultErrorListResponse(
                normalizedBatchId,
                mapper.listErrors(normalizedBatchId, paging.offset(), paging.safeSize()),
                Math.max(page, 0),
                paging.safeSize(),
                mapper.countErrors(normalizedBatchId));
    }
}
