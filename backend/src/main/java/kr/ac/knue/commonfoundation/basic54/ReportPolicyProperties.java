package kr.ac.knue.commonfoundation.basic54;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "report.policy")
public class ReportPolicyProperties {
    private int resultFileRetentionDays = 0;
    private PermissionMergeStrategy permissionMergeStrategy = PermissionMergeStrategy.ANY_ALLOW;
    private UnauthorizedBulkTargetPolicy unauthorizedBulkTargetPolicy = UnauthorizedBulkTargetPolicy.REJECT_JOB;
    private boolean recordFailedPrintHistory = true;

    public static ReportPolicyProperties defaults() {
        return new ReportPolicyProperties();
    }

    public int resultFileRetentionDays() {
        return resultFileRetentionDays;
    }

    public void setResultFileRetentionDays(int resultFileRetentionDays) {
        this.resultFileRetentionDays = resultFileRetentionDays;
    }

    public PermissionMergeStrategy permissionMergeStrategy() {
        return permissionMergeStrategy;
    }

    public void setPermissionMergeStrategy(PermissionMergeStrategy permissionMergeStrategy) {
        this.permissionMergeStrategy = permissionMergeStrategy;
    }

    public UnauthorizedBulkTargetPolicy unauthorizedBulkTargetPolicy() {
        return unauthorizedBulkTargetPolicy;
    }

    public void setUnauthorizedBulkTargetPolicy(UnauthorizedBulkTargetPolicy unauthorizedBulkTargetPolicy) {
        this.unauthorizedBulkTargetPolicy = unauthorizedBulkTargetPolicy;
    }

    public boolean recordFailedPrintHistory() {
        return recordFailedPrintHistory;
    }

    public void setRecordFailedPrintHistory(boolean recordFailedPrintHistory) {
        this.recordFailedPrintHistory = recordFailedPrintHistory;
    }

    public enum PermissionMergeStrategy {
        PRIORITY_FIRST_MATCH,
        ANY_ALLOW
    }

    public enum UnauthorizedBulkTargetPolicy {
        REJECT_JOB,
        EXCLUDE_AND_RECORD
    }
}
