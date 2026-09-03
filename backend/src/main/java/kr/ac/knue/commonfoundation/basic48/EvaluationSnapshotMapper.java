package kr.ac.knue.commonfoundation.basic48;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EvaluationSnapshotMapper {
    List<EvaluationSnapshotRow> listEvaluationSnapshots(@Param("criteria") EvaluationSnapshotSearchCriteria criteria);

    long countEvaluationSnapshots(@Param("criteria") EvaluationSnapshotSearchCriteria criteria);

    EvaluationSnapshotDetail findEvaluationSnapshotDetail(@Param("snapshotId") String snapshotId,
                                                          @Param("dataScope") EvaluationSnapshotDataScope dataScope,
                                                          @Param("organizationCode") String organizationCode);

    long countEvaluationMaterialsForReadonlyGuard();

    long countEvaluationRuleSetsForReadonlyGuard();

    void insertReadAudit(@Param("targetKey") String targetKey,
                         @Param("afterStateJson") String afterStateJson,
                         @Param("actorUserId") Long actorUserId,
                         @Param("requestId") String requestId);
}
