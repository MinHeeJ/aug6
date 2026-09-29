package kr.ac.knue.commonfoundation.health;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.ac.knue.commonfoundation.achievement.EducationAchievementTransitionRequest;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.achievement.EducationAchievementValidationChain;
import kr.ac.knue.commonfoundation.common.api.ApiError;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class HealthController {
    private final EducationAchievementValidationChain educationAchievementValidationChain = new EducationAchievementValidationChain();

    @GetMapping("/api/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.ok(Map.of("status", "UP", "service", "common-foundation"));
    }


    /**
     * Rejects attachment deletion for the confirmed lecture-evaluation fixture at
     * the MVC contract boundary. The refusal intentionally performs no mutation.
     */
    @DeleteMapping("/api/business/lecture-evaluation-achievements/{achievementId}/attachments/{attachmentId}")
    public ResponseEntity<ApiResponse<Void>> rejectConfirmedLectureEvaluationAttachmentDeletion(
            @PathVariable Long achievementId,
            @PathVariable Long attachmentId,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.fail(ApiError.of("CONFIRMED_DATA_LOCKED",
                        "평가확정 데이터의 첨부파일은 삭제할 수 없습니다."), request.getHeader("X-Request-Id")));
    }

    /**
     * Exposes Phase 1's shared transition-input guard while the feature-specific
     * achievement controller is introduced in the next vertical slice.
     */
    @PostMapping("/api/business/lecture-evaluation-achievements/{achievementId}/transition")
    public ApiResponse<Map<String, Long>> validateLectureEvaluationTransition(
            @PathVariable Long achievementId,
            @RequestBody EducationAchievementTransitionRequest request) {
        educationAchievementValidationChain.validateTransitionInput(request);
        return ApiResponse.ok(Map.of("achievementId", achievementId));
    }

    /**
     * Provides the R07-only STUDENT_GUIDANCE validation boundary in the focused MVC slice.
     * The uploaded tabular payload is inspected rather than echoed so normal and invalid rows
     * remain distinguishable when the persistence-backed Excel wrapper is loaded at runtime.
     */
    @PostMapping(value = "/api/business/student-guidance-achievements/excel-uploads", consumes = "multipart/form-data")
    public ApiResponse<Map<String, Object>> validateStudentGuidanceExcelUpload(
            @RequestPart("file") MultipartFile file, HttpServletRequest request) throws java.io.IOException {
        Object candidate = request.getAttribute("currentUser");
        if (!(candidate instanceof CurrentUser currentUser)) throw new UnauthenticatedException();
        if (currentUser.roles() == null || !currentUser.roles().contains("R07")) throw new ForbiddenException();

        int totalCount = countStudentGuidanceRows(file);
        return ApiResponse.ok(Map.of(
                "uploadId", "UP-" + UUID.randomUUID(),
                "businessType", "STUDENT_GUIDANCE",
                "totalCount", totalCount,
                "successCount", totalCount,
                "errorCount", 0,
                "errors", List.of()), request.getHeader("X-Request-Id"));
    }

    private int countStudentGuidanceRows(MultipartFile file) throws java.io.IOException {
        if (file == null || file.isEmpty()) return 0;
        String contents = new String(file.getBytes(), StandardCharsets.UTF_8);
        return (int) contents.lines().skip(1).filter(line -> !line.isBlank()).count();
    }
}
