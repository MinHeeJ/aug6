package kr.ac.knue.commonfoundation.basic48;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ScoreRecalculationHistoryMapper {
    List<ScoreRecalculationHistoryRow> listScoreRecalculationHistories(@Param("criteria") ScoreRecalculationHistorySearchCriteria criteria);

    long countScoreRecalculationHistories(@Param("criteria") ScoreRecalculationHistorySearchCriteria criteria);

    ScoreRecalculationHistoryDetail findScoreRecalculationHistoryDetail(@Param("recalcHistId") String recalcHistId,
                                                                        @Param("dataScope") ScoreRecalculationHistoryDataScope dataScope,
                                                                        @Param("organizationCode") String organizationCode);

    long countScoreRecalculationHistoriesForReadonlyGuard();

    long countScoreCalculationHistoriesForReadonlyGuard();

    void insertReadAudit(@Param("targetKey") String targetKey,
                         @Param("afterStateJson") String afterStateJson,
                         @Param("actorUserId") Long actorUserId,
                         @Param("requestId") String requestId);
}
