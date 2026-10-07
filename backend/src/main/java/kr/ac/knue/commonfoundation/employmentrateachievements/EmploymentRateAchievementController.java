package kr.ac.knue.commonfoundation.employmentrateachievements;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.RequestIds;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** All eight approved routes; no unsupported delete, status-change or commit endpoint is invented. */
@RestController
@RequestMapping("/api/business/employment-rate-achievements")
public class EmploymentRateAchievementController {
    private final EmploymentRateAchievementService service;
    private final EmploymentRateExcelService excel;

    public EmploymentRateAchievementController(
            EmploymentRateAchievementService service,
            EmploymentRateExcelService excel) {
        this.service = service;
        this.excel = excel;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestAttribute(value = "currentUser", required = false) CurrentUser user,
            HttpServletRequest request) {
        EmploymentRateAchievementService.requireRoles(user, "R01", "R02", "R04");
        String requestId = RequestIds.resolve(request);
        return ApiResponse.ok(service.list(page, pageSize, user), requestId);
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(
            @RequestBody EmploymentRateAchievementRequest input,
            @RequestAttribute(value = "currentUser", required = false) CurrentUser user,
            HttpServletRequest request) {
        EmploymentRateAchievementService.requireRoles(user, "R01");
        String requestId = RequestIds.resolve(request);
        return ApiResponse.ok(service.create(input, user, requestId), requestId);
    }

    @GetMapping("/{achievementId}")
    public ApiResponse<Map<String, Object>> detail(
            @PathVariable Long achievementId,
            @RequestAttribute(value = "currentUser", required = false) CurrentUser user,
            HttpServletRequest request) {
        EmploymentRateAchievementService.requireRoles(user, "R01", "R02", "R04");
        String requestId = RequestIds.resolve(request);
        return ApiResponse.ok(service.detail(achievementId, user), requestId);
    }

    @PutMapping("/{achievementId}")
    public ApiResponse<Map<String, Object>> update(
            @PathVariable Long achievementId,
            @RequestBody EmploymentRateAchievementRequest input,
            @RequestAttribute(value = "currentUser", required = false) CurrentUser user,
            HttpServletRequest request) {
        EmploymentRateAchievementService.requireRoles(user, "R01");
        String requestId = RequestIds.resolve(request);
        return ApiResponse.ok(service.update(achievementId, input, user, requestId), requestId);
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> download(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestAttribute(value = "currentUser", required = false) CurrentUser user,
            HttpServletRequest request) {
        EmploymentRateAchievementService.requireRoles(user, "R01", "R02", "R04", "R07");
        String requestId = RequestIds.resolve(request);
        byte[] workbook = excel.download(service.downloadRows(page, pageSize, user));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"employment-rate-achievements.xlsx\"")
                .header("X-Request-Id", requestId)
                .body(workbook);
    }

    @PostMapping(value = "/excel-uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Object> upload(
            @RequestPart("file") MultipartFile file,
            @RequestAttribute(value = "currentUser", required = false) CurrentUser user,
            HttpServletRequest request) {
        EmploymentRateAchievementService.requireRoles(user, "R07");
        String requestId = RequestIds.resolve(request);
        service.authorizeUpload(user);
        return ApiResponse.ok(excel.upload(file, user, requestId), requestId);
    }

    @PostMapping("/bulk-jobs")
    public ApiResponse<Map<String, Object>> createBulk(
            @RequestBody EmploymentRateBulkJobRequest input,
            @RequestAttribute(value = "currentUser", required = false) CurrentUser user,
            HttpServletRequest request) {
        EmploymentRateAchievementService.requireRoles(user, "R07");
        String requestId = RequestIds.resolve(request);
        return ApiResponse.ok(service.createBulk(input, user, requestId), requestId);
    }

    @GetMapping("/bulk-jobs/{jobId}")
    public ApiResponse<Map<String, Object>> job(
            @PathVariable String jobId,
            @RequestAttribute(value = "currentUser", required = false) CurrentUser user,
            HttpServletRequest request) {
        EmploymentRateAchievementService.requireRoles(user, "R07");
        String requestId = RequestIds.resolve(request);
        return ApiResponse.ok(service.job(jobId, user), requestId);
    }
}
