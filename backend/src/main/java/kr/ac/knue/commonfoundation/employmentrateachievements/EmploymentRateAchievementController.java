package kr.ac.knue.commonfoundation.employmentrateachievements;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** HTTP facade for individual achievements and the R07 validation/commit and retained-result workflow. */
@RestController
@RequestMapping("/api/business/employment-rate-achievements")
public class EmploymentRateAchievementController {
    private final EmploymentRateAchievementService service;
    private final EmploymentRateExcelService excel;
    private final EmploymentRateXlsxCodec codec;

    public EmploymentRateAchievementController(
            EmploymentRateAchievementService service, EmploymentRateExcelService excel, EmploymentRateXlsxCodec codec) {
        this.service = service;
        this.excel = excel;
        this.codec = codec;
    }

    @GetMapping
    public ApiResponse<?> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) String achievementStatus,
            HttpServletRequest request) {
        CurrentUser user = user(request, "R01", "R02", "R04");
        return ApiResponse.ok(service.list(filters(page, pageSize, managementNo, managementItemCode,
                teacherName, achievementStatus), user, false), trace(request));
    }

    @GetMapping("/{achievementId}")
    public ApiResponse<?> get(@PathVariable Long achievementId, HttpServletRequest request) {
        return ApiResponse.ok(service.get(achievementId, user(request, "R01", "R02", "R04")), trace(request));
    }

    @PostMapping
    public ApiResponse<?> create(@Valid @RequestBody EmploymentRateAchievementRequest body, HttpServletRequest request) {
        return ApiResponse.ok(service.save(null, body, user(request, "R01"), trace(request)), trace(request));
    }

    @PutMapping("/{achievementId}")
    public ApiResponse<?> update(
            @PathVariable Long achievementId,
            @Valid @RequestBody EmploymentRateAchievementRequest body,
            HttpServletRequest request) {
        return ApiResponse.ok(service.save(achievementId, body, user(request, "R01"), trace(request)), trace(request));
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> download(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) String achievementStatus,
            HttpServletRequest request) {
        CurrentUser user = user(request, "R01", "R02", "R04", "R07");
        Map<String, Object> result = service.list(filters(page, pageSize, managementNo, managementItemCode,
                teacherName, achievementStatus), user, true);
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("교번", "관리항목코드", "업적발생일", "실적명", "첨부참조"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> achievements = (List<Map<String, Object>>) result.get("achievements");
        for (Map<String, Object> row : achievements) {
            rows.add(List.of(text(row, "employeeNo"), text(row, "managementItemCode"),
                    text(row, "achievementDate"), text(row, "achievementName"), text(row, "attachmentRef")));
        }
        return file(codec.write(rows), "employment-rate-achievements.xlsx", request);
    }

    @PostMapping("/bulk-jobs")
    public ApiResponse<?> bulk(@Valid @RequestBody EmploymentRateBulkJobRequest body, HttpServletRequest request) {
        service.createBulk(body, user(request, "R07"));
        throw new IllegalStateException("미승인 정책입니다.");
    }

    @GetMapping("/bulk-jobs/{jobId}")
    public ApiResponse<?> job(@PathVariable String jobId, HttpServletRequest request) {
        return ApiResponse.ok(service.job(jobId, user(request, "R07")), trace(request));
    }

    @PostMapping(value = "/excel-uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<?> upload(@RequestPart MultipartFile file, HttpServletRequest request) {
        return ApiResponse.ok(excel.upload(file, user(request, "R07"), trace(request)), trace(request));
    }

    @GetMapping("/excel-uploads/template")
    public ResponseEntity<byte[]> template(HttpServletRequest request) {
        return file(excel.template(user(request, "R07")), "employment-rate-template.xlsx", request);
    }

    @PostMapping("/excel-uploads/{uploadId}/commit")
    public ApiResponse<?> commit(@PathVariable String uploadId, HttpServletRequest request) {
        return ApiResponse.ok(excel.commit(uploadId, user(request, "R07"), trace(request)), trace(request));
    }

    @GetMapping("/excel-uploads/histories")
    public ApiResponse<?> histories(HttpServletRequest request) {
        return ApiResponse.ok(excel.histories(user(request, "R07")), trace(request));
    }

    @GetMapping("/excel-uploads/{uploadId}/errors")
    public ApiResponse<?> errors(@PathVariable String uploadId, HttpServletRequest request) {
        return ApiResponse.ok(excel.errors(uploadId, user(request, "R07")), trace(request));
    }

    @GetMapping("/excel-uploads/{uploadId}/errors/download")
    public ResponseEntity<byte[]> errorsDownload(@PathVariable String uploadId, HttpServletRequest request) {
        return file(excel.errorsFile(uploadId, user(request, "R07")), "employment-rate-errors.xlsx", request);
    }

    private String text(Map<String, Object> row, String key) {
        return row.get(key) == null ? "" : row.get(key).toString();
    }

    private ResponseEntity<byte[]> file(byte[] bytes, String name, HttpServletRequest request) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                .header("X-Request-Id", trace(request))
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    private Map<String, Object> filters(
            int page, int pageSize, String managementNo, String item, String teacher, String state) {
        if (page < 0 || !List.of(20, 50, 100).contains(pageSize)) {
            EmploymentRateAchievementService.invalid("pageSize", "page는 0 이상, pageSize는 20/50/100이어야 합니다.");
        }
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("page", page);
        p.put("pageSize", pageSize);
        p.put("pageOffset", (long) page * pageSize);
        p.put("managementNo", normalize(managementNo));
        p.put("managementItemCode", normalize(item));
        p.put("teacherName", normalize(teacher));
        p.put("achievementStatus", normalize(state));
        return p;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private CurrentUser user(HttpServletRequest request, String... roles) {
        CurrentUser user = request.getAttribute("currentUser") instanceof CurrentUser current ? current : null;
        EmploymentRateAchievementService.requireRole(user, roles);
        return user;
    }

    static String trace(HttpServletRequest request) {
        if (request.getAttribute("employmentRateRequestId") instanceof String existing) {
            return existing;
        }
        String value = request.getHeader("X-Request-Id");
        if (value == null || !value.matches("[a-zA-Z0-9_.:-]{1,100}")) {
            value = UUID.randomUUID().toString();
        }
        request.setAttribute("employmentRateRequestId", value);
        return value;
    }
}
