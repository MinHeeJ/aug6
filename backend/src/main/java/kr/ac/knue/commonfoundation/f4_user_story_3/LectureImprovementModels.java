package kr.ac.knue.commonfoundation.f4_user_story_3;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Contract records for the BASIC-83 lecture-improvement HTTP and persistence boundary. */
public final class LectureImprovementModels {
    private LectureImprovementModels() {
    }

    public record Request(
            @NotBlank(message = "관리항목을 입력하세요.") String managementItemCode,
            @NotNull(message = "업적발생일을 입력하세요.") LocalDate achievementDate,
            @NotBlank(message = "강의개선 내용을 입력하세요.") String achievementContent,
            @NotNull(message = "학년도를 입력하세요.") @Min(value = 2000, message = "학년도는 2000 이상이어야 합니다.") Integer academicYear,
            @NotNull(message = "학기를 입력하세요.") Integer semester,
            List<String> attachmentIds) {
    }

    public record Row(
            Long achievementId,
            String managementNo,
            Long targetUserId,
            String managementItemCode,
            LocalDate achievementDate,
            String achievementContent,
            Integer academicYear,
            Integer semester,
            String certificationStatus,
            String attachmentIds,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    public record SearchResponse(List<Row> achievements, int page, int pageSize, long totalElements) {
    }

    public record SaveResponse(Row achievement, boolean occurredDateWarning, String warningMessage) {
    }
}
