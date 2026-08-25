package kr.ac.knue.commonfoundation.privacy;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PrivacyAccessLogMapper {
    List<PrivacyAccessLogRow> searchPrivacyAccessLogs(@Param("criteria") PrivacyAccessLogSearchCriteria criteria);

    long countPrivacyAccessLogs(@Param("criteria") PrivacyAccessLogSearchCriteria criteria);

    PrivacyAccessLogRow findPrivacyAccessLog(@Param("historyId") Long historyId);

    long countPrivacyAccessLogsByHistoryId(@Param("historyId") Long historyId);

    void insertPrivacyAccessLog(@Param("processType") String processType,
                                @Param("actorUserId") Long actorUserId,
                                @Param("targetRef") String targetRef,
                                @Param("processPurpose") String processPurpose,
                                @Param("requestIp") String requestIp,
                                @Param("processResult") String processResult);

    PrivacyAccessLogRow findLatestForActorTargetPurpose(@Param("actorUserId") Long actorUserId,
                                                        @Param("targetRef") String targetRef,
                                                        @Param("processPurpose") String processPurpose);
}
