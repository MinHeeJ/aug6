package kr.ac.knue.commonfoundation.basic65;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record TeachingEvaluationAchievementRow(
        Long achievementId,
        Long facultyUserId,
        String evaluationYear,
        String academicYear,
        String semester,
        String courseCode,
        String courseName,
        BigDecimal evaluationScore,
        String evaluationStatus,
        Long managementItemSettingId,
        String dynamicFieldsJson,
        String attachmentRefsJson,
        LocalDateTime createdAt,
        Long createdBy,
        LocalDateTime updatedAt,
        Long updatedBy) {
    private static final Pattern JSON_STRING_FIELD = Pattern.compile("\\\"((?:\\\\.|[^\\\"])*)\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"");
    private static final Pattern JSON_STRING_VALUE = Pattern.compile("\\\"((?:\\\\.|[^\\\"])*)\\\"");

    @JsonProperty("dynamicFields")
    public Map<String, String> dynamicFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        if (dynamicFieldsJson == null || dynamicFieldsJson.isBlank()) {
            return fields;
        }
        Matcher matcher = JSON_STRING_FIELD.matcher(dynamicFieldsJson);
        while (matcher.find()) {
            fields.put(unescape(matcher.group(1)), unescape(matcher.group(2)));
        }
        return fields;
    }

    @JsonProperty("attachmentRefs")
    public List<String> attachmentRefs() {
        if (attachmentRefsJson == null || attachmentRefsJson.isBlank()) {
            return List.of();
        }
        return JSON_STRING_VALUE.matcher(attachmentRefsJson).results()
                .map(result -> unescape(result.group(1)))
                .toList();
    }

    private static String unescape(String value) {
        return value.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
