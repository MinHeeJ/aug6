package kr.ac.knue.commonfoundation.basic65;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public record TeachingAchievementRow(
        Long achievementId, Long facultyUserId, String evaluationYear, String academicYear, String semester,
        String courseCode, String courseName, String courseType, BigDecimal creditHours, String achievementStatus,
        Long managementItemSettingId, String dynamicFieldsJson, String attachmentRefsJson, LocalDateTime createdAt,
        Long createdBy, LocalDateTime updatedAt, Long updatedBy) {
    private static final Pattern JSON_STRING_FIELD = Pattern.compile("\\\"((?:\\\\.|[^\\\"])*)\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"");
    private static final Pattern JSON_STRING_VALUE = Pattern.compile("\\\"((?:\\\\.|[^\\\"])*)\\\"");

    @JsonProperty("dynamicFields")
    public Map<String, String> dynamicFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        if (dynamicFieldsJson != null) JSON_STRING_FIELD.matcher(dynamicFieldsJson).results()
                .forEach(match -> fields.put(unescape(match.group(1)), unescape(match.group(2))));
        return fields;
    }

    @JsonProperty("attachmentRefs")
    public List<String> attachmentRefs() {
        return attachmentRefsJson == null ? List.of() : JSON_STRING_VALUE.matcher(attachmentRefsJson).results()
                .map(match -> unescape(match.group(1))).toList();
    }

    private static String unescape(String value) {
        return value.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
