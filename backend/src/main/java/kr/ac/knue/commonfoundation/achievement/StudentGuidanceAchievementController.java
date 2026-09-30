package kr.ac.knue.commonfoundation.achievement;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Owns FR-027 individual student-guidance and R07-only Excel upload HTTP endpoints.
 * The Excel boundary is separate because R07 must not receive individual achievement mutation access.
 */
@RestController
public class StudentGuidanceAchievementController {
    private final StudentGuidanceAchievementService service;

    public StudentGuidanceAchievementController(StudentGuidanceAchievementService service) {
        this.service = service;
    }

    /** Saves one individual student-guidance achievement and all entered student details atomically. */
    @PostMapping("/api/business/student-guidance-achievements")
    public ApiResponse<StudentGuidanceDtos.Row> save(@Valid @RequestBody StudentGuidanceDtos.SaveRequest body,
            HttpServletRequest request) {
        return ApiResponse.ok(service.save(body, individualUser(request)));
    }

    /** Returns an individual achievement detail without exposing storage tokens or paths. */
    @GetMapping("/api/business/student-guidance-achievements/{achievementId}")
    public ApiResponse<StudentGuidanceDtos.Row> detail(@PathVariable Long achievementId, HttpServletRequest request) {
        individualUser(request);
        return ApiResponse.ok(service.find(achievementId));
    }

    /** Validates a R07 student-guidance CSV into the shared template/staging/error/history workflow. */
    @PostMapping(value = "/api/business/student-guidance-achievements/excel-uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<StudentGuidanceDtos.UploadResult> upload(@RequestPart("file") MultipartFile file, HttpServletRequest request) {
        return ApiResponse.ok(service.validateUpload(file, excelUser(request)));
    }

    /** Commits only a zero-error validation result, preserving the all-or-nothing import rule. */
    @PostMapping("/api/business/student-guidance-achievements/excel-uploads/{uploadId}/commit")
    public ApiResponse<StudentGuidanceDtos.CommitResult> commit(@PathVariable String uploadId, HttpServletRequest request) {
        return ApiResponse.ok(service.commitUpload(uploadId, excelUser(request)));
    }

    /** Returns only the calling R07 user's student-guidance upload histories. */
    @GetMapping("/api/business/student-guidance-achievements/excel-uploads/histories")
    public ApiResponse<List<StudentGuidanceDtos.UploadHistory>> histories(HttpServletRequest request) {
        return ApiResponse.ok(service.histories(excelReadUser(request)));
    }

    /** Downloads the standard, versioned STUDENT_GUIDANCE template without exposing a storage path. */
    @GetMapping("/api/business/student-guidance-achievements/excel-uploads/template")
    public ResponseEntity<byte[]> template(HttpServletRequest request) {
        return file("학생지도_일괄등록_v1.csv", service.template(excelReadUser(request)));
    }

    /** Downloads validation errors so the user can repair the source file before retrying. */
    @GetMapping("/api/business/student-guidance-achievements/excel-uploads/{uploadId}/errors/download")
    public ResponseEntity<byte[]> errorFile(@PathVariable String uploadId, HttpServletRequest request) {
        return file("학생지도_업로드오류_" + uploadId + ".csv", service.errorFile(uploadId, excelUser(request)));
    }

    private ResponseEntity<byte[]> file(String name, byte[] content) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(name, java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType("text/csv")).body(content);
    }

    private CurrentUser individualUser(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user.roles().stream().noneMatch(role -> List.of("R01", "R02", "R04").contains(role))) throw new ForbiddenException();
        return user;
    }

    private CurrentUser excelReadUser(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user.roles().stream().noneMatch(role -> List.of("R07", "R09").contains(role))) throw new ForbiddenException();
        return user;
    }

    private CurrentUser excelUser(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (!user.roles().contains("R07")) throw new ForbiddenException();
        return user;
    }

    private CurrentUser currentUser(HttpServletRequest request) {
        Object value = request.getAttribute("currentUser");
        if (!(value instanceof CurrentUser user)) throw new UnauthenticatedException();
        return user;
    }
}
