package kr.ac.knue.commonfoundation.basic46;

import java.util.List;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BatchProcessingResultService {
    private final BatchProcessingResultMapper mapper;

    public BatchProcessingResultService(BatchProcessingResultMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public BatchProcessingResultSearchResponse list(BatchProcessingResultSearchCriteria criteria) {
        BatchProcessingResultSearchCriteria normalized = criteria == null
                ? new BatchProcessingResultSearchCriteria(0, 20, null, null, null)
                : criteria;
        return new BatchProcessingResultSearchResponse(
                mapper.listBatchProcessingResults(normalized),
                Math.max(normalized.page(), 0),
                normalized.safeSize(),
                mapper.countBatchProcessingResults(normalized));
    }

    @Transactional(readOnly = true)
    public List<BatchProcessingResultErrorRow> listErrors(String batchId) {
        if (batchId == null || batchId.isBlank()) {
            throw new BusinessValidationException("배치ID를 입력하세요.",
                    List.of(new ValidationError("batchId", "배치ID를 입력하세요.")));
        }
        return mapper.listBatchProcessingResultErrors(batchId.trim());
    }

    public void createOrRerun(Object ignored) {
        throw new UnsupportedOperationException("처리 결과 조회 화면은 일괄작업 실행 또는 재실행을 제공하지 않습니다.");
    }
}
