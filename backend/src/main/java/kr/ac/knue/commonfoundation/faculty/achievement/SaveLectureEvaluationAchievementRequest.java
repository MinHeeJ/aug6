package kr.ac.knue.commonfoundation.faculty.achievement;

import java.time.LocalDate;
import java.util.Map;

/**
 * Request body defined by saveLectureEvaluationAchievement. The source contract does not
 * provide an identifier, so this phase creates a new DRAFTING achievement per save request.
 */
public class SaveLectureEvaluationAchievementRequest {
    private String managementItemCode;
    private LocalDate occurredDate;
    private Map<String, Object> achievementDetail;

    public String getManagementItemCode() {
        return managementItemCode;
    }

    public void setManagementItemCode(String managementItemCode) {
        this.managementItemCode = managementItemCode;
    }

    public LocalDate getOccurredDate() {
        return occurredDate;
    }

    public void setOccurredDate(LocalDate occurredDate) {
        this.occurredDate = occurredDate;
    }

    public Map<String, Object> getAchievementDetail() {
        return achievementDetail;
    }

    public void setAchievementDetail(Map<String, Object> achievementDetail) {
        this.achievementDetail = achievementDetail;
    }
}
