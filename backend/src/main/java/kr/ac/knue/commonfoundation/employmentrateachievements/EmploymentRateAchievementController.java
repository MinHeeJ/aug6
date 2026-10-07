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
import kr.ac.knue.commonfoundation.excel.ExcelUploadResult;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Approved eight HTTP operations, using the platform session principal and request-scoped trace. */
@RestController
@RequestMapping("/api/business/employment-rate-achievements")
public class EmploymentRateAchievementController {
    private final EmploymentRateAchievementService service;
    private final EmploymentRateExcelService excel;

    public EmploymentRateAchievementController(EmploymentRateAchievementService service, EmploymentRateExcelService excel) {
        this.service = service;
        this.excel = excel;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> listEmploymentRateAchievements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String achievementStatus,
            HttpServletRequest request) {
        CurrentUser user = user(request, "R01", "R02", "R04");
        return ApiResponse.ok(service.list(query(page, pageSize, managementItemCode, managementNo, achievementStatus), user),
                requestId(request));
    }

    @GetMapping("/{achievementId}")
    public ApiResponse<Map<String, Object>> getEmploymentRateAchievement(
            @PathVariable Long achievementId, HttpServletRequest request) {
        return ApiResponse.ok(service.get(achievementId, user(request, "R01", "R02", "R04")), requestId(request));
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> createEmploymentRateAchievement(
            @Valid @RequestBody EmploymentRateAchievementRequest body, HttpServletRequest request) {
        CurrentUser user = user(request, "R01");
        String id = requestId(request);
        return ApiResponse.ok(service.create(body, user, id), id);
    }

    @PutMapping("/{achievementId}")
    public ApiResponse<Map<String, Object>> updateEmploymentRateAchievement(
            @PathVariable Long achievementId,
            @Valid @RequestBody EmploymentRateAchievementRequest body,
            HttpServletRequest request) {
        CurrentUser user = user(request, "R01");
        String id = requestId(request);
        return ApiResponse.ok(service.update(achievementId, body, user, id), id);
    }

    /** Binary download is the exact scoped page, rendered as literal XLSX cells. */
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadEmploymentRateAchievements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String achievementStatus,
            HttpServletRequest request) {
        List<Map<String, Object>> data = service.downloadRows(
                query(page, pageSize, managementItemCode, managementNo, achievementStatus),
                user(request, "R01", "R02", "R04", "R07"));
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("관리번호", "관리항목코드", "업적발생일", "실적명", "상태"));
        for (Map<String, Object> row : data) {
            rows.add(List.of(text(row, "managementNo"), text(row, "managementItemCode"),
                    text(row, "achievementDate"), text(row, "achievementName"), text(row, "achievementStatus")));
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, EmploymentRateXlsxCodec.MIME)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("employment-rate-achievements.xlsx").build().toString())
                .header("X-Request-Id", requestId(request))
                .body(EmploymentRateXlsxCodec.write(rows));
    }

    @PostMapping(value = "/excel-uploads", consumes = "multipart/form-data")
    public ApiResponse<ExcelUploadResult> uploadEmploymentRateAchievementsExcel(
            @RequestPart("file") MultipartFile file, HttpServletRequest request) {
        CurrentUser user = user(request, "R07");
        String id = requestId(request);
        return ApiResponse.ok(excel.upload(file, user, id), id);
    }

    /** No 202 is fabricated while the generation/deletion policy is unapproved. */
    @PostMapping("/bulk-jobs")
    public ApiResponse<Void> createEmploymentRateBulkJob(
            @Valid @RequestBody EmploymentRateBulkJobRequest body, HttpServletRequest request) {
        service.bulk(body, user(request, "R07"));
        throw new IllegalStateException("미승인 일괄 정책의 실행은 허용되지 않습니다.");
    }

    @GetMapping("/bulk-jobs/{jobId}")
    public ApiResponse<Map<String, Object>> getEmploymentRateBulkJob(
            @PathVariable String jobId, HttpServletRequest request) {
        return ApiResponse.ok(service.job(jobId, user(request, "R07")), requestId(request));
    }

    private static CurrentUser user(HttpServletRequest request, String... roles) {
        CurrentUser user = request.getAttribute("currentUser") instanceof CurrentUser principal ? principal : null;
        EmploymentRateAchievementService.requireRole(user, roles);
        return user;
    }

    private static String requestId(HttpServletRequest request) {
        Object current = request.getAttribute("requestId");
        String id = current instanceof String value ? value : request.getHeader("X-Request-Id");
        if (id == null || !id.matches("[A-Za-z0-9._:-]{1,100}")) {
            id = UUID.randomUUID().toString();
        }
        request.setAttribute("requestId", id);
        return id;
    }

    private static Map<String, Object> query(int page, int pageSize, String item, String number, String status) {
        if (page < 0 || page > 1000000) {
            EmploymentRateAchievementService.invalid("page", "유효한 페이지 번호를 입력하세요.");
        }
        if (!List.of(20, 50, 100).contains(pageSize)) {
            EmploymentRateAchievementService.invalid("pageSize", "20, 50, 100건 중 선택하세요.");
        }
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("page", page);
        query.put("pageSize", pageSize);
        query.put("pageOffset", page * pageSize);
        if (item != null && !item.isBlank()) {
            query.put("managementItemCode", item.trim());
        }
        if (number != null && !number.isBlank()) {
            query.put("managementNo", number.trim());
        }
        if (status != null && !status.isBlank()) {
            query.put("achievementStatus", status.trim());
        }
        return query;
    }

    private static String text(Map<String, Object> row, String key) {
        return row.get(key) == null ? "" : row.get(key).toString();
    }
}
