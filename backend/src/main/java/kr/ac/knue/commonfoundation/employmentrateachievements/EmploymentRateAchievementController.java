package kr.ac.knue.commonfoundation.employmentrateachievements;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Approved FR-032 HTTP entrypoints; service authorization also protects non-HTTP callers. */
@RestController
@RequestMapping("/api/business/employment-rate-achievements")
public class EmploymentRateAchievementController {
    private final EmploymentRateAchievementService service;

    public EmploymentRateAchievementController(EmploymentRateAchievementService service) {
        this.service = service;
    }

    /** Lists the session principal's rows with safe dynamic filters and bounded pagination. */
    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam Map<String, String> parameters, HttpServletRequest request) {
        return ApiResponse.ok(service.list(query(parameters), user(request)), trace(request));
    }

    @GetMapping("/{achievementId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long achievementId, HttpServletRequest request) {
        return ApiResponse.ok(service.detail(achievementId, user(request)), trace(request));
    }

    /** Collection POST never updates an existing identity. */
    @PostMapping
    public ApiResponse<Map<String, Object>> create(
            @Valid @RequestBody EmploymentRateAchievementRequest body, HttpServletRequest request) {
        String trace = trace(request);
        return ApiResponse.ok(service.create(body, user(request), trace), trace);
    }

    /** Path identity is the sole update target; ownership and state are not request fields. */
    @PutMapping("/{achievementId}")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long achievementId,
            @Valid @RequestBody EmploymentRateAchievementRequest body, HttpServletRequest request) {
        String trace = trace(request);
        return ApiResponse.ok(service.update(achievementId, body, user(request), trace), trace);
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> download(@RequestParam Map<String, String> parameters, HttpServletRequest request) {
        var file = service.download(query(parameters), user(request));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.originalFileName(), StandardCharsets.UTF_8).build().toString())
                .header("X-Request-Id", trace(request))
                .contentType(MediaType.parseMediaType(file.contentType())).body(file.content());
    }

    /**
     * Reserves 202 for actual policy-approved acceptance; the current service rejects pending
     * policy with 409 before creating any job, so this annotation does not enable execution.
     */
    @PostMapping("/bulk-jobs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<Map<String, Object>> bulk(
            @Valid @RequestBody EmploymentRateBulkJobRequest body, HttpServletRequest request) {
        return ApiResponse.ok(service.createBulk(body, user(request)), trace(request));
    }

    @PostMapping("/bulk-jobs/preview")
    public ApiResponse<Map<String, Object>> preview(
            @Valid @RequestBody EmploymentRateBulkJobRequest body, HttpServletRequest request) {
        return ApiResponse.ok(service.preview(body, user(request)), trace(request));
    }

    @GetMapping("/bulk-jobs/{jobId}")
    public ApiResponse<Map<String, Object>> job(@PathVariable String jobId, HttpServletRequest request) {
        return ApiResponse.ok(service.job(jobId, user(request)), trace(request));
    }

    private Map<String, Object> query(Map<String, String> parameters) {
        int page = integer(parameters.getOrDefault("page", "0"), "page");
        int size = integer(parameters.getOrDefault("pageSize", "20"), "pageSize");
        if (page < 0 || !List.of(20, 50, 100).contains(size)) {
            throw invalid(page < 0 ? "page" : "pageSize", "페이지는 0 이상, 표시 건수는 20/50/100입니다.");
        }
        Map<String, Object> query = new HashMap<>();
        query.put("page", page);
        query.put("pageSize", size);
        query.put("pageOffset", (long) page * size);
        for (String key : List.of("managementNo", "teacherName", "managementItemCode", "certificationStatus")) {
            String value = parameters.get(key);
            if (value != null && !value.isBlank()) query.put(key, value.trim());
        }
        return query;
    }

    private int integer(String value, String field) {
        try { return Integer.parseInt(value); }
        catch (NumberFormatException exception) { throw invalid(field, "정수를 입력하세요."); }
    }

    private BusinessValidationException invalid(String field, String message) {
        return new BusinessValidationException("조회조건을 확인하세요.", List.of(new ValidationError(field, message)));
    }

    private CurrentUser user(HttpServletRequest request) {
        if (request.getAttribute("currentUser") instanceof CurrentUser user) return user;
        throw new UnauthenticatedException();
    }

    private String trace(HttpServletRequest request) {
        String id = request.getHeader("X-Request-Id");
        return id == null || id.isBlank() ? UUID.randomUUID().toString() : id.trim();
    }
}
