package kr.ac.knue.commonfoundation.fileoperations;

import com.fasterxml.jackson.annotation.JsonAlias;

public class AttachmentDeleteRequest {
    @JsonAlias("delete_reason")
    private String deleteReason;

    public String getDeleteReason() {
        return deleteReason;
    }

    public void setDeleteReason(String deleteReason) {
        this.deleteReason = deleteReason;
    }
}
