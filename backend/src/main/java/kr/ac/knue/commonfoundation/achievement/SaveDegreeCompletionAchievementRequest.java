package kr.ac.knue.commonfoundation.achievement;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.List;

/** Input for creating a degree-completion header and its student details atomically. */
public record SaveDegreeCompletionAchievementRequest(String managementItemCode, LocalDate occurredDate,
        JsonNode achievementDetail, List<StudentInput> students) {
    /** Required student detail fields for the degree-completion record. */
    public record StudentInput(String degreeType, String studentName, String thesisTitle, LocalDate degreeAwardedDate) {
    }
}
