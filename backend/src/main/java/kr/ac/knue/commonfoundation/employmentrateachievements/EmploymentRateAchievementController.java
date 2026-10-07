package kr.ac.knue.commonfoundation.employmentrateachievements;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Approved FR-032 CRUD, complete scoped export and policy-gated bulk entrypoints. */
@RestController
@RequestMapping("/api/business/employment-rate-achievements")
public class EmploymentRateAchievementController {
    private final EmploymentRateAchievementService service;

    public EmploymentRateAchievementController(EmploymentRateAchievementService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            HttpServletRequest request) {
        return ApiResponse.ok(service.list(user(request, "R01", "R02", "R04"), filters, page, pageSize), trace(request));
    }

    @GetMapping("/{achievementId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable long achievementId, HttpServletRequest request) {
        return ApiResponse.ok(service.detail(user(request, "R01", "R02", "R04"), achievementId), trace(request));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(
            @Valid @RequestBody EmploymentRateAchievementRequest body, HttpServletRequest request) {
        return ApiResponse.ok(service.save(user(request, "R01"), body, null, trace(request)), trace(request));
    }

    @PutMapping("/{achievementId}")
    public ApiResponse<Map<String, Object>> update(@PathVariable long achievementId,
            @Valid @RequestBody EmploymentRateAchievementRequest body, HttpServletRequest request) {
        return ApiResponse.ok(service.save(user(request, "R01"), body, achievementId, trace(request)), trace(request));
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> download(@RequestParam Map<String, String> filters, HttpServletRequest request) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=employment-rate-achievements.xlsx")
                .contentType(MediaType.parseMediaType(EmploymentRateWorkbookCodec.MIME))
                .body(service.download(user(request, "R01", "R02", "R04", "R07"), filters));
    }

    @PostMapping("/bulk-jobs")
    public ApiResponse<Void> createBulk(
            @RequestBody EmploymentRateAchievementService.BulkRequest body, HttpServletRequest request) {
        service.createBulk(user(request, "R07"), body);
        return ApiResponse.empty();
    }

    @GetMapping("/bulk-jobs/preview")
    public ApiResponse<Map<String, Object>> preview(
            @RequestParam Map<String, String> filters, HttpServletRequest request) {
        return ApiResponse.ok(service.preview(user(request, "R07"), filters), trace(request));
    }

    @GetMapping("/bulk-jobs/{jobId}")
    public ApiResponse<Map<String, Object>> job(@PathVariable String jobId, HttpServletRequest request) {
        return ApiResponse.ok(service.job(user(request, "R07"), jobId), trace(request));
    }

    private CurrentUser user(HttpServletRequest request, String... roles) {
        CurrentUser user = (CurrentUser) request.getAttribute("currentUser");
        EmploymentRateAchievementService.require(user, roles);
        return user;
    }

    private String trace(HttpServletRequest request) {
        if (request.getAttribute("requestId") instanceof String id) return id;
        String id = request.getHeader("X-Request-Id");
        id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id.trim();
        request.setAttribute("requestId", id);
        return id;
    }
}
