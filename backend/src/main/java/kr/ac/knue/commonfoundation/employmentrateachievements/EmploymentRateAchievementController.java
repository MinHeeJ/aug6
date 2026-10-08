package kr.ac.knue.commonfoundation.employmentrateachievements;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.*;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** HTTP contract for individual, XLSX staging and policy-blocked bulk employment-rate workflows. */
@RestController
@RequestMapping("/api/business/employment-rate-achievements")
public class EmploymentRateAchievementController {
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private final EmploymentRateAchievementService service;
    private final EmploymentRateExcelService excel;

    public EmploymentRateAchievementController(EmploymentRateAchievementService service,
            EmploymentRateExcelService excel) {
        this.service = service;
        this.excel = excel;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String evaluationYear,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) String achievementStatus, HttpServletRequest request) {
        return ApiResponse.ok(service.list(page, pageSize, evaluationYear, managementItemCode, achievementStatus,
                user(request, EmploymentRateAchievementService.READ), false), trace(request));
    }

    @GetMapping("/{achievementId}")
    public ApiResponse<Map<String, Object>> get(@PathVariable Long achievementId, HttpServletRequest request) {
        return ApiResponse.ok(service.get(achievementId, user(request, EmploymentRateAchievementService.READ)),
                trace(request));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@Valid @RequestBody EmploymentRateAchievementRequest body,
            HttpServletRequest request) {
        String trace = trace(request);
        return ApiResponse.ok(service.save(null, body, user(request, Set.of("R01")), trace), trace);
    }

    @PutMapping("/{achievementId}")
    public ApiResponse<Map<String, Object>> update(@PathVariable Long achievementId,
            @Valid @RequestBody EmploymentRateAchievementRequest body, HttpServletRequest request) {
        String trace = trace(request);
        return ApiResponse.ok(service.save(achievementId, body, user(request, Set.of("R01")), trace), trace);
    }

    @GetMapping("/download")
    @SuppressWarnings("unchecked")
    public ResponseEntity<byte[]> download(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String evaluationYear,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) String achievementStatus, HttpServletRequest request) {
        Map<String, Object> found = service.list(page, pageSize, evaluationYear, managementItemCode,
                achievementStatus, user(request, EmploymentRateAchievementService.DOWNLOAD), true);
        List<String> headers = List.of("achievementId", "teacherUserId", "evaluationYear", "managementItemCode",
                "achievementDate", "achievementName", "achievementDetail", "achievementStatus", "attachmentRef");
        List<List<String>> rows = new ArrayList<>();
        rows.add(headers);
        for (Map<String, Object> row : (List<Map<String, Object>>) found.get("achievements")) {
            rows.add(headers.stream().map(key -> Objects.toString(row.get(key), "")).toList());
        }
        return binary(EmploymentRateWorkbook.write(rows), "employment-rate.xlsx");
    }

    @PostMapping("/bulk-jobs")
    public ApiResponse<Object> bulk(@Valid @RequestBody EmploymentRateBulkJobRequest body,
            HttpServletRequest request) {
        service.bulk(body, user(request, Set.of("R07")));
        throw new ConflictException("BULK_POLICY_NOT_APPROVED");
    }

    @GetMapping("/bulk-jobs/{jobId}")
    public ApiResponse<Map<String, Object>> job(@PathVariable String jobId, HttpServletRequest request) {
        return ApiResponse.ok(service.job(jobId, user(request, Set.of("R07"))), trace(request));
    }

    @PostMapping("/bulk-jobs/preview")
    public ApiResponse<Map<String, Object>> preview(@Valid @RequestBody EmploymentRateBulkJobRequest body,
            HttpServletRequest request) {
        return ApiResponse.ok(service.preview(body, user(request, Set.of("R07"))), trace(request));
    }

    @PostMapping(value = "/excel-uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> upload(@RequestPart("file") MultipartFile file,
            HttpServletRequest request) {
        String trace = trace(request);
        Map<String, Object> result = excel.upload(file, user(request, Set.of("R07")), trace);
        ApiResponse<Map<String, Object>> response = ApiResponse.ok(result, trace);
        if ("REJECTED".equals(result.get("validationStatus"))) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, result,
                    ApiError.of("VALIDATION_ERROR", "오류 행이 있어 전체 반영하지 않았습니다."), response.meta()));
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/excel-uploads/template")
    public ResponseEntity<byte[]> template(HttpServletRequest request) {
        return binary(excel.template(user(request, Set.of("R07"))), "employment-rate-template.xlsx");
    }

    @PostMapping("/excel-uploads/{uploadId}/commit")
    public ApiResponse<Map<String, Object>> commit(@PathVariable String uploadId, HttpServletRequest request) {
        String trace = trace(request);
        return ApiResponse.ok(excel.commit(uploadId, user(request, Set.of("R07")), trace), trace);
    }

    @GetMapping("/excel-uploads/histories")
    public ApiResponse<List<Map<String, Object>>> histories(HttpServletRequest request) {
        return ApiResponse.ok(excel.histories(user(request, Set.of("R07"))), trace(request));
    }

    @GetMapping("/excel-uploads/{uploadId}/errors")
    public ApiResponse<List<Map<String, Object>>> errors(@PathVariable String uploadId, HttpServletRequest request) {
        return ApiResponse.ok(excel.errors(uploadId, user(request, Set.of("R07"))), trace(request));
    }

    @GetMapping("/excel-uploads/{uploadId}/errors/download")
    public ResponseEntity<byte[]> errorFile(@PathVariable String uploadId, HttpServletRequest request) {
        return binary(excel.downloadErrors(uploadId, user(request, Set.of("R07"))), "employment-rate-errors.xlsx");
    }

    private ResponseEntity<byte[]> binary(byte[] data, String name) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(XLSX))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(name).build().toString())
                .body(data);
    }

    private CurrentUser user(HttpServletRequest request, Set<String> roles) {
        if (!(request.getAttribute("currentUser") instanceof CurrentUser user)) throw new UnauthenticatedException();
        if (user.roles() == null || (!user.roles().contains("R09")
                && user.roles().stream().noneMatch(roles::contains))) throw new ForbiddenException();
        return user;
    }

    private String trace(HttpServletRequest request) {
        String id = request.getHeader("X-Request-Id");
        return id == null || id.isBlank() || id.length() > 100 ? UUID.randomUUID().toString() : id.trim();
    }
}
