package kr.ac.knue.commonfoundation.basic54;

public record ReportPermissionDecision(boolean allowed, String reasonCode) {
    public static ReportPermissionDecision allow() { return new ReportPermissionDecision(true, "ALLOW"); }
    public static ReportPermissionDecision deny(String reasonCode) { return new ReportPermissionDecision(false, reasonCode); }
}
