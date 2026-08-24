package kr.ac.knue.commonfoundation.fileoperations;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class FilePolicySaveRequest {
    @NotBlank(message = "업무구분을 입력하세요.")
    private String businessType;

    @NotBlank(message = "허용 확장자를 입력하세요.")
    private String allowedExtensions;

    @NotNull(message = "단일 파일 최대용량을 입력하세요.")
    @Min(value = 1, message = "단일 파일 최대용량은 1MB 이상이어야 합니다.")
    private Integer maxFileSizeMb;

    @NotNull(message = "건당 첨부개수를 입력하세요.")
    @Min(value = 1, message = "건당 첨부개수는 1개 이상이어야 합니다.")
    private Integer maxFilesPerItem;

    @Min(value = 1, message = "전체용량은 1MB 이상이어야 합니다.")
    private Integer maxTotalSizeMb;

    @NotNull(message = "파일명 길이를 입력하세요.")
    @Min(value = 1, message = "파일명 길이는 1자 이상이어야 합니다.")
    private Integer maxFilenameLength;

    @NotNull(message = "악성파일 검사 적용여부를 선택하세요.")
    private Boolean malwareScanEnabled;

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getAllowedExtensions() {
        return allowedExtensions;
    }

    public void setAllowedExtensions(String allowedExtensions) {
        this.allowedExtensions = allowedExtensions;
    }

    public Integer getMaxFileSizeMb() {
        return maxFileSizeMb;
    }

    public void setMaxFileSizeMb(Integer maxFileSizeMb) {
        this.maxFileSizeMb = maxFileSizeMb;
    }

    public Integer getMaxFilesPerItem() {
        return maxFilesPerItem;
    }

    public void setMaxFilesPerItem(Integer maxFilesPerItem) {
        this.maxFilesPerItem = maxFilesPerItem;
    }

    public Integer getMaxTotalSizeMb() {
        return maxTotalSizeMb;
    }

    public void setMaxTotalSizeMb(Integer maxTotalSizeMb) {
        this.maxTotalSizeMb = maxTotalSizeMb;
    }

    public Integer getMaxFilenameLength() {
        return maxFilenameLength;
    }

    public void setMaxFilenameLength(Integer maxFilenameLength) {
        this.maxFilenameLength = maxFilenameLength;
    }

    public Boolean getMalwareScanEnabled() {
        return malwareScanEnabled;
    }

    public void setMalwareScanEnabled(Boolean malwareScanEnabled) {
        this.malwareScanEnabled = malwareScanEnabled;
    }
}
