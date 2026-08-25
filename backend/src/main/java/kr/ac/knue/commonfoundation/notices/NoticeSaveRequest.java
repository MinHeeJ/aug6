package kr.ac.knue.commonfoundation.notices;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class NoticeSaveRequest {
    private static final Set<String> ALLOWED_FIELDS = Set.of(
            "title", "content", "publishStartDate", "publishEndDate", "importantYn", "targets", "attachments", "changeReason");

    @NotBlank(message = "제목을 입력하세요.")
    @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
    private String title;

    @NotBlank(message = "공지 내용을 입력하세요.")
    private String content;

    @NotNull(message = "게시 시작일을 입력하세요.")
    private LocalDate publishStartDate;

    @NotNull(message = "게시 종료일을 입력하세요.")
    private LocalDate publishEndDate;

    @NotBlank(message = "중요 여부를 선택하세요.")
    @Pattern(regexp = "Y|N", message = "중요 여부는 Y 또는 N이어야 합니다.")
    private String importantYn;

    @Valid
    private List<NoticeTargetInput> targets = new ArrayList<>();

    @Valid
    private List<NoticeAttachmentInput> attachments = new ArrayList<>();

    @NotBlank(message = "변경 사유를 입력하세요.")
    @Size(max = 500, message = "변경 사유는 500자 이하여야 합니다.")
    private String changeReason;

    private final Set<String> unexpectedFields = new LinkedHashSet<>();

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDate getPublishStartDate() { return publishStartDate; }
    public void setPublishStartDate(LocalDate publishStartDate) { this.publishStartDate = publishStartDate; }
    public LocalDate getPublishEndDate() { return publishEndDate; }
    public void setPublishEndDate(LocalDate publishEndDate) { this.publishEndDate = publishEndDate; }
    public String getImportantYn() { return importantYn; }
    public void setImportantYn(String importantYn) { this.importantYn = importantYn; }
    public List<NoticeTargetInput> getTargets() { return targets; }
    public void setTargets(List<NoticeTargetInput> targets) { this.targets = targets == null ? new ArrayList<>() : targets; }
    public List<NoticeAttachmentInput> getAttachments() { return attachments; }
    public void setAttachments(List<NoticeAttachmentInput> attachments) { this.attachments = attachments == null ? new ArrayList<>() : attachments; }
    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }
    public Set<String> getUnexpectedFields() { return unexpectedFields; }

    @JsonAnySetter
    public void captureUnexpectedField(String field, Object ignored) {
        if (!ALLOWED_FIELDS.contains(field)) {
            unexpectedFields.add(field);
        }
    }
}
