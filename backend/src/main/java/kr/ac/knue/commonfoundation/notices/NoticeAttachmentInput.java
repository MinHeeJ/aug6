package kr.ac.knue.commonfoundation.notices;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class NoticeAttachmentInput {
    @NotBlank(message = "원본 파일명을 입력하세요.")
    @Size(max = 255, message = "원본 파일명은 255자 이하여야 합니다.")
    private String originalFileName;

    @Size(max = 2000000, message = "첨부파일 내용은 2MB 이하여야 합니다.")
    private String contentBase64;

    @Size(max = 1000000, message = "첨부파일 내용은 1MB 이하여야 합니다.")
    private String contentText;

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getContentBase64() {
        return contentBase64;
    }

    public void setContentBase64(String contentBase64) {
        this.contentBase64 = contentBase64;
    }

    public String getContentText() {
        return contentText;
    }

    public void setContentText(String contentText) {
        this.contentText = contentText;
    }
}
