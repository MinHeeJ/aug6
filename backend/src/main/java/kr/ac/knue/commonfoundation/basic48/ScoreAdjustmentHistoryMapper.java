package kr.ac.knue.commonfoundation.basic48;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ScoreAdjustmentHistoryMapper {
    List<ScoreAdjustmentHistoryRow> listScoreAdjustmentHistories(@Param("criteria") ScoreAdjustmentHistorySearchCriteria criteria);

    long countScoreAdjustmentHistories(@Param("criteria") ScoreAdjustmentHistorySearchCriteria criteria);

    ScoreAdjustmentHistoryDetail findScoreAdjustmentHistoryDetail(@Param("adjustmentHistId") String adjustmentHistId,
                                                                  @Param("dataScope") ScoreAdjustmentHistoryDataScope dataScope,
                                                                  @Param("organizationCode") String organizationCode);

    long countScoreAdjustmentHistoriesForReadonlyGuard();

    long countScoreCalculationHistoriesForReadonlyGuard();

    void insertReadAudit(@Param("targetKey") String targetKey,
                         @Param("afterStateJson") String afterStateJson,
                         @Param("actorUserId") Long actorUserId,
                         @Param("requestId") String requestId);
}
