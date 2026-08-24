package kr.ac.knue.commonfoundation.fileoperations;

import java.time.LocalDateTime;

public class AttachmentIntegrityCheckRow {
    private Long checkId;
    private String status;
    private Long startedBy;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    public AttachmentIntegrityCheckRow() {
    }

    public AttachmentIntegrityCheckRow(Long checkId, String status, Long startedBy, LocalDateTime startedAt, LocalDateTime completedAt) {
        this.checkId = checkId;
        this.status = status;
        this.startedBy = startedBy;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public Long checkId() {
        return checkId;
    }

    public Long getCheckId() {
        return checkId;
    }

    public void setCheckId(Long checkId) {
        this.checkId = checkId;
    }

    public String status() {
        return status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long startedBy() {
        return startedBy;
    }

    public Long getStartedBy() {
        return startedBy;
    }

    public void setStartedBy(Long startedBy) {
        this.startedBy = startedBy;
    }

    public LocalDateTime startedAt() {
        return startedAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime completedAt() {
        return completedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
