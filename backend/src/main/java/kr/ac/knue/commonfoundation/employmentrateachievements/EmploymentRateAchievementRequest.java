package kr.ac.knue.commonfoundation.employmentrateachievements;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/** Binds the approved individual employment-rate achievement create and update payload. */
public class EmploymentRateAchievementRequest {
    @NotBlank(message = "관리항목을 입력하세요.")
    private String managementItemCode;

    @NotNull(message = "업적발생일을 입력하세요.")
    private LocalDate achievementDate;

    private String achievementName;
    private List<String> attachmentIds;

    public String getManagementItemCode() {
        return managementItemCode;
    }

    public void setManagementItemCode(String managementItemCode) {
        this.managementItemCode = managementItemCode;
    }

    public LocalDate getAchievementDate() {
        return achievementDate;
    }

    public void setAchievementDate(LocalDate achievementDate) {
        this.achievementDate = achievementDate;
    }

    public String getAchievementName() {
        return achievementName;
    }

    public void setAchievementName(String achievementName) {
        this.achievementName = achievementName;
    }

    public List<String> getAttachmentIds() {
        return attachmentIds;
    }

    public void setAttachmentIds(List<String> attachmentIds) {
        this.attachmentIds = attachmentIds;
    }
}
