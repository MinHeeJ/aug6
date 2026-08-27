package kr.ac.knue.commonfoundation.securitysessions;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ActiveSessionMapper {
    List<ActiveSessionRow> listActiveSessions(@Param("criteria") ActiveSessionSearchCriteria criteria,
            @Param("limit") int limit, @Param("offset") int offset);
    long countActiveSessions(@Param("criteria") ActiveSessionSearchCriteria criteria);
    ActiveSessionRow findSessionForUpdate(@Param("sessionId") String sessionId);
    void markTerminated(@Param("sessionId") String sessionId, @Param("reason") String reason,
            @Param("operatorUserId") Long operatorUserId, @Param("requestId") String requestId);
    void insertTerminationHistory(@Param("sessionId") String sessionId, @Param("reason") String reason);
    void insertSessionTerminateAudit(@Param("sessionId") String sessionId, @Param("operatorUserId") Long operatorUserId,
            @Param("reason") String reason, @Param("requestId") String requestId);
    List<SessionTerminationHistoryRow> listSessionTerminationHistories(
            @Param("criteria") SessionTerminationHistorySearchCriteria criteria,
            @Param("limit") int limit, @Param("offset") int offset);
    long countSessionTerminationHistories(@Param("criteria") SessionTerminationHistorySearchCriteria criteria);
}
