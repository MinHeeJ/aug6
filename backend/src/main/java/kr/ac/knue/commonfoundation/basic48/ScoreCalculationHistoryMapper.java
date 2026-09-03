package kr.ac.knue.commonfoundation.basic48;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ScoreCalculationHistoryMapper {
    List<ScoreCalculationHistoryRow> listScoreCalculationHistories(@Param("criteria") ScoreCalculationHistorySearchCriteria criteria);

    long countScoreCalculationHistories(@Param("criteria") ScoreCalculationHistorySearchCriteria criteria);

    ScoreCalculationHistoryDetail findScoreCalculationHistoryDetail(@Param("calcHistId") String calcHistId,
                                                                    @Param("dataScope") ScoreCalculationHistoryDataScope dataScope,
                                                                    @Param("organizationCode") String organizationCode,
                                                                    @Param("selfUserId") Long selfUserId);

    long countScoreCalculationHistoriesForReadonlyGuard();

    long countEvaluationMaterialsForReadonlyGuard();

    void insertReadAudit(@Param("targetKey") String targetKey,
                         @Param("afterStateJson") String afterStateJson,
                         @Param("actorUserId") Long actorUserId,
                         @Param("requestId") String requestId);
}
