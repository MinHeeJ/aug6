package kr.ac.knue.commonfoundation.batch;

import java.util.List;
import java.util.Set;
import kr.ac.knue.commonfoundation.common.api.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BatchResultService {
    private static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(20, 50, 100);
    private final BatchResultMapper mapper;

    public BatchResultService(BatchResultMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public BatchResultSearchResponse listBatchResults(int page, int size, BatchResultSearchCriteria criteria) {
        int safePage = Math.max(page, 0);
        int safeSize = ALLOWED_PAGE_SIZES.contains(size) ? size : 20;
        BatchResultSearchCriteria normalized = new BatchResultSearchCriteria(
                blankToNull(criteria == null ? null : criteria.executionId()),
                blankToNull(criteria == null ? null : criteria.batchId()),
                blankToNull(criteria == null ? null : criteria.executionStatus()));
        List<BatchResultRow> results = mapper.listBatchResults(normalized, safeSize, safePage * safeSize);
        return new BatchResultSearchResponse(results, safePage, safeSize, mapper.countBatchResults(normalized));
    }

    @Transactional(readOnly = true)
    public BatchResultLogResponse getBatchResultLog(String executionId) {
        String normalizedExecutionId = blankToNull(executionId);
        if (normalizedExecutionId == null) {
            throw new NotFoundException("배치 실행 로그를 찾을 수 없습니다.");
        }
        BatchResultLogResponse log = mapper.findBatchResultLog(normalizedExecutionId);
        if (log == null) {
            throw new NotFoundException("배치 실행 로그를 찾을 수 없습니다.");
        }
        return log;
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isBlank() ? null : value.trim();
    }
}
