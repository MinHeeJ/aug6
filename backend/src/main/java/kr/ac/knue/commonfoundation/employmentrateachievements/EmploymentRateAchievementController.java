package kr.ac.knue.commonfoundation.employmentrateachievements;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.*;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** Employment-rate transport boundary, with distinct individual and operator role admission. */
@RestController
@RequestMapping("/api/business/employment-rate-achievements")
public class EmploymentRateAchievementController {
    private final EmploymentRateAchievementService service;

    public EmploymentRateAchievementController(EmploymentRateAchievementService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(@RequestParam Map<String, String> filters, HttpServletRequest request) {
        return ApiResponse.ok(service.list(new HashMap<>(filters), user(request, "R01", "R02", "R04")), trace(request));
    }

    @GetMapping("/{achievementId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long achievementId, HttpServletRequest request) {
        return ApiResponse.ok(service.detail(achievementId, user(request, "R01", "R02", "R04")), trace(request));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(
            @Valid @RequestBody EmploymentRateAchievementRequest body, HttpServletRequest request) {
        return ApiResponse.ok(service.save(null, body, user(request, "R01"), trace(request)), trace(request));
    }

    @PutMapping("/{achievementId}")
    public ApiResponse<Map<String, Object>> update(
            @PathVariable Long achievementId,
            @Valid @RequestBody EmploymentRateAchievementRequest body,
            HttpServletRequest request) {
        return ApiResponse.ok(service.save(achievementId, body, user(request, "R01"), trace(request)), trace(request));
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> download(@RequestParam Map<String, String> filters, HttpServletRequest request) {
        return workbook(service.download(new HashMap<>(filters), user(request, "R01", "R02", "R04", "R07")),
                "취업률_실적.xlsx");
    }

    @PostMapping("/bulk-jobs")
    public ApiResponse<Void> bulk(@Valid @RequestBody EmploymentRateBulkJobRequest body, HttpServletRequest request) {
        service.bulk(body, user(request, "R07"));
        return ApiResponse.empty();
    }

    @GetMapping("/bulk-jobs/{jobId}")
    public ApiResponse<Map<String, Object>> job(@PathVariable String jobId, HttpServletRequest request) {
        return ApiResponse.ok(service.job(jobId, user(request, "R07")), trace(request));
    }

    @PostMapping(value = "/excel-uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> upload(@RequestPart("file") MultipartFile file, HttpServletRequest request) {
        return ApiResponse.ok(service.upload(file, user(request, "R07"), trace(request)), trace(request));
    }

    @GetMapping("/excel-uploads/template")
    public ResponseEntity<byte[]> template(HttpServletRequest request) {
        return workbook(service.template(user(request, "R07")), "취업률_템플릿.xlsx");
    }

    @PostMapping("/excel-uploads/{uploadId}/commit")
    public ApiResponse<Map<String, Object>> commit(@PathVariable String uploadId, HttpServletRequest request) {
        return ApiResponse.ok(service.commit(uploadId, user(request, "R07"), trace(request)), trace(request));
    }

    @GetMapping("/excel-uploads/histories")
    public ApiResponse<List<Map<String, Object>>> histories(HttpServletRequest request) {
        return ApiResponse.ok(service.histories(user(request, "R07")), trace(request));
    }

    @GetMapping("/excel-uploads/{uploadId}/errors")
    public ApiResponse<List<Map<String, Object>>> errors(@PathVariable String uploadId, HttpServletRequest request) {
        return ApiResponse.ok(service.errors(uploadId, user(request, "R07")), trace(request));
    }

    @GetMapping("/excel-uploads/{uploadId}/errors/download")
    public ResponseEntity<byte[]> errorsDownload(@PathVariable String uploadId, HttpServletRequest request) {
        return workbook(service.errorDownload(uploadId, user(request, "R07")), "취업률_오류결과.xlsx");
    }

    private CurrentUser user(HttpServletRequest request, String... roles) {
        Object principal = request.getAttribute("currentUser");
        if (!(principal instanceof CurrentUser user)) {
            throw new UnauthenticatedException();
        }
        EmploymentRateAchievementService.role(user, roles);
        return user;
    }

    static String trace(HttpServletRequest request) {
        Object existing = request.getAttribute("employmentRateRequestId");
        if (existing != null) {
            return existing.toString();
        }
        String value = request.getHeader("X-Request-Id");
        if (value == null || value.isBlank()) {
            value = UUID.randomUUID().toString();
        }
        request.setAttribute("employmentRateRequestId", value);
        return value;
    }

    private ResponseEntity<byte[]> workbook(byte[] bytes, String name) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(EmploymentRateWorkbook.CONTENT_TYPE))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(name, StandardCharsets.UTF_8).build().toString())
                .body(bytes);
    }
}
