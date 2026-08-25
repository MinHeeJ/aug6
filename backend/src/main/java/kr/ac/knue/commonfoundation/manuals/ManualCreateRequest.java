package kr.ac.knue.commonfoundation.manuals;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

public class ManualCreateRequest {
    private static final Set<String> ALLOWED_FIELDS = Set.of(
            "manualType", "version", "targetUser", "effectiveDate", "originalFileName", "fileContent", "changeReason");

    @NotBlank(message = "매뉴얼 유형을 선택하세요.")
    @Size(max = 50, message = "매뉴얼 유형은 50자 이하여야 합니다.")
    private String manualType;

    @NotBlank(message = "버전을 입력하세요.")
    @Size(max = 50, message = "버전은 50자 이하여야 합니다.")
    private String version;

    @NotBlank(message = "대상 사용자를 입력하세요.")
    @Size(max = 100, message = "대상 사용자는 100자 이하여야 합니다.")
    private String targetUser;

    @NotNull(message = "시행일을 입력하세요.")
    private LocalDate effectiveDate;

    @NotBlank(message = "원본 파일명을 입력하세요.")
    @Size(max = 255, message = "원본 파일명은 255자 이하여야 합니다.")
    private String originalFileName;

    @NotBlank(message = "매뉴얼 파일 내용을 입력하세요.")
    private String fileContent;

    @NotBlank(message = "변경 사유를 입력하세요.")
    @Size(max = 500, message = "변경 사유는 500자 이하여야 합니다.")
    private String changeReason;

    private final Set<String> unexpectedFields = new LinkedHashSet<>();

    public String getManualType() { return manualType; }
    public void setManualType(String manualType) { this.manualType = manualType; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getTargetUser() { return targetUser; }
    public void setTargetUser(String targetUser) { this.targetUser = targetUser; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }
    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
    public String getFileContent() { return fileContent; }
    public void setFileContent(String fileContent) { this.fileContent = fileContent; }
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
