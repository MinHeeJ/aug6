package kr.ac.knue.commonfoundation.basic36;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface KorusFacultySyncMapper {
    Boolean requestIdExists(@Param("requestId") String requestId);
    void insertRun(@Param("requestId") String requestId, @Param("runType") String runType,
                   @Param("targetStartDate") LocalDate targetStartDate, @Param("targetEndDate") LocalDate targetEndDate,
                   @Param("createdBy") Long createdBy);
    KorusFacultySyncRunRow findRunByRequestId(@Param("requestId") String requestId);
    List<KorusFacultySourceRow> listFacultySources(@Param("targetStartDate") LocalDate targetStartDate,
                                                   @Param("targetEndDate") LocalDate targetEndDate);
    Boolean organizationExists(@Param("organizationCode") String organizationCode);
    void upsertResult(@Param("runId") Long runId, @Param("requestId") String requestId,
                      @Param("source") KorusFacultySourceRow source, @Param("syncStatus") String syncStatus,
                      @Param("errorMessage") String errorMessage, @Param("retryOfResultId") Long retryOfResultId);
    void updateRunCounts(@Param("runId") Long runId, @Param("runStatus") String runStatus,
                         @Param("totalCount") int totalCount, @Param("successCount") int successCount,
                         @Param("failureCount") int failureCount, @Param("failureReason") String failureReason);
    KorusFacultySyncResultRow findResult(@Param("resultId") Long resultId);
    List<KorusFacultySyncResultRow> listResults(@Param("criteria") KorusFacultySyncSearchCriteria criteria);
    long countResults(@Param("criteria") KorusFacultySyncSearchCriteria criteria);
}
