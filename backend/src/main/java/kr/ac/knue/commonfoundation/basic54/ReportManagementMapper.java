package kr.ac.knue.commonfoundation.basic54;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReportManagementMapper {
    List<ReportRow> listReports(@Param("criteria") ReportSearchCriteria criteria);
    long countReports(@Param("criteria") ReportSearchCriteria criteria);
    ReportRow findReportById(@Param("reportId") String reportId, @Param("includeInactive") boolean includeInactive);
    ReportRow insertReport(@Param("request") ReportSaveRequest request, @Param("userId") Long userId);
    ReportRow updateReport(@Param("request") ReportSaveRequest request, @Param("userId") Long userId);

    List<ReportFormVersionRow> listReportFormVersions(@Param("criteria") ReportOperationCriteria criteria);
    long countReportFormVersions(@Param("criteria") ReportOperationCriteria criteria);
    ReportFormVersionRow findReportFormVersionById(@Param("formVersionId") Long formVersionId);
    ReportFormVersionRow findApplicableFormVersion(@Param("reportId") String reportId, @Param("baseDate") LocalDate baseDate);
    ReportFormVersionRow insertReportFormVersion(@Param("request") ReportFormVersionSaveRequest request, @Param("userId") Long userId);
    ReportFormVersionRow updateReportFormVersion(@Param("request") ReportFormVersionSaveRequest request, @Param("userId") Long userId);

    List<ReportPermissionRow> listReportPermissions(@Param("criteria") ReportOperationCriteria criteria);
    long countReportPermissions(@Param("criteria") ReportOperationCriteria criteria);
    ReportPermissionRow findReportPermissionById(@Param("permissionId") Long permissionId);
    List<ReportPermissionRow> findActivePermissionsForReport(@Param("reportId") String reportId, @Param("roleCodes") List<String> roleCodes, @Param("userId") Long userId);
    ReportPermissionRow insertReportPermission(@Param("request") ReportPermissionSaveRequest request, @Param("userId") Long userId);
    ReportPermissionRow updateReportPermission(@Param("request") ReportPermissionSaveRequest request, @Param("userId") Long userId);

    List<ReportPrintHistoryRow> listReportPrintHistories(@Param("criteria") ReportOperationCriteria criteria);
    long countReportPrintHistories(@Param("criteria") ReportOperationCriteria criteria);
    void insertReportPrintHistory(@Param("reportId") String reportId, @Param("requesterId") Long requesterId, @Param("targetSummary") String targetSummary, @Param("outputFormat") String outputFormat, @Param("outputCount") int outputCount, @Param("resultCode") String resultCode, @Param("fileRef") String fileRef, @Param("requestId") String requestId);

    List<BulkReportJobRow> listBulkReportJobs(@Param("criteria") ReportOperationCriteria criteria);
    long countBulkReportJobs(@Param("criteria") ReportOperationCriteria criteria);
    BulkReportJobRow findBulkReportJobById(@Param("jobId") Long jobId);
    int countRunningBulkReportJobs(@Param("reportId") String reportId, @Param("targetHash") String targetHash);
    BulkReportJobRow insertBulkReportJob(@Param("request") BulkReportJobCreateRequest request, @Param("userId") Long userId, @Param("requestId") String requestId, @Param("totalCount") int totalCount);
    void insertBulkReportJobTarget(@Param("jobId") Long jobId, @Param("targetPersonId") Long targetPersonId);
    BulkReportJobRow completeBulkReportJob(@Param("jobId") Long jobId, @Param("resultFileRef") String resultFileRef);
    List<BulkReportJobTargetRow> listBulkReportTargets(@Param("criteria") ReportOperationCriteria criteria);
    long countBulkReportTargets(@Param("criteria") ReportOperationCriteria criteria);
    List<BulkReportJobTargetRow> listBulkReportJobFailures(@Param("jobId") Long jobId);

    void insertChangeHistory(@Param("targetBusiness") String targetBusiness, @Param("targetKey") String targetKey, @Param("changeType") String changeType, @Param("fieldName") String fieldName, @Param("beforeValue") String beforeValue, @Param("afterValue") String afterValue, @Param("changedBy") Long changedBy, @Param("changeReason") String changeReason, @Param("requestId") String requestId);
}
