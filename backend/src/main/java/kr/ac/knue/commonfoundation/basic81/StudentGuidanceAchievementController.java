package kr.ac.knue.commonfoundation.basic81;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import kr.ac.knue.commonfoundation.excel.ExcelDownloadFile;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Exposes the student-guidance individual API and the separately authorized R07 Excel wizard API. */
@RestController
public class StudentGuidanceAchievementController {
    private final StudentGuidanceAchievementService service;

    public StudentGuidanceAchievementController(StudentGuidanceAchievementService service) {
        this.service = service;
    }

    /** Lists student guidance records for R01/R02/R04 through service-side data scoping. */
    @GetMapping("/api/business/student-guidance-achievements")
    public ApiResponse<StudentGuidanceAchievementSearchResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String managementNo,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) String managementItemCode,
            @RequestParam(required = false) String guidanceDateFrom,
            @RequestParam(required = false) String guidanceDateTo,
            @RequestParam(required = false) String certificationStatus,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            HttpServletRequest request) {
        validatePageSize(pageSize);
        CurrentUser user = requireAchievementUser(request);
        return ApiResponse.ok(service.list(new StudentGuidanceAchievementSearchCriteria(page, pageSize, managementNo,
                teacherName, managementItemCode, guidanceDateFrom, guidanceDateTo, certificationStatus), user), traceId(requestId));
    }

    /** Persists a student-guidance header and its complete student detail collection atomically. */
    @PostMapping("/api/business/student-guidance-achievements")
    public ApiResponse<StudentGuidanceAchievementRow> save(@Valid @RequestBody SaveStudentGuidanceAchievementRequest body,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest request) {
        CurrentUser user = requireAchievementUser(request);
        String traceId = traceId(requestId);
        return ApiResponse.ok(service.save(body, user, traceId), traceId);
    }

    /** Downloads the current R07 student-guidance template through the same role boundary as upload. */
    @GetMapping("/api/business/student-guidance-achievements/excel-uploads/template")
    public ResponseEntity<byte[]> downloadTemplate(HttpServletRequest request) {
        ExcelDownloadFile template = service.downloadExcelTemplate(requireExcelUser(request));
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(template.originalFileName(), StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType(template.contentType()))
                .body(template.content());
    }

    /** Validates a current STUDENT_GUIDANCE CSV and records validation/history rows for R07 only. */
    @PostMapping(value = "/api/business/student-guidance-achievements/excel-uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<StudentGuidanceExcelUploadResult> upload(@RequestPart("file") MultipartFile file,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest request) {
        CurrentUser user = requireExcelUser(request);
        return ApiResponse.ok(service.validateExcelUpload(file, user), traceId(requestId));
    }

    /** Performs the confirmed all-or-nothing commit of a previously validated R07 upload. */
    @PostMapping("/api/business/student-guidance-achievements/excel-uploads/{uploadId}/commit")
    public ApiResponse<StudentGuidanceExcelCommitResult> commit(@PathVariable String uploadId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId, HttpServletRequest request) {
        CurrentUser user = requireExcelUser(request);
        String traceId = traceId(requestId);
        return ApiResponse.ok(service.commitExcelUpload(uploadId, user, traceId), traceId);
    }

    /** Returns R07-visible upload history without exposing generic R09 administration records. */
    @GetMapping("/api/business/student-guidance-achievements/excel-uploads/histories")
    public ApiResponse<List<StudentGuidanceExcelHistoryRow>> histories(HttpServletRequest request) {
        return ApiResponse.ok(service.listExcelHistories(requireExcelUser(request)));
    }

    /** Returns validation errors for the selected R07 upload. */
    @GetMapping("/api/business/student-guidance-achievements/excel-uploads/{uploadId}/errors")
    public ApiResponse<List<StudentGuidanceExcelErrorRow>> errors(@PathVariable String uploadId, HttpServletRequest request) {
        return ApiResponse.ok(service.listExcelErrors(uploadId, requireExcelUser(request)));
    }

    /** Downloads the persisted validation error view for a selected R07 upload. */
    @GetMapping("/api/business/student-guidance-achievements/excel-uploads/{uploadId}/errors/download")
    public ResponseEntity<byte[]> downloadErrors(@PathVariable String uploadId, HttpServletRequest request) {
        List<StudentGuidanceExcelErrorRow> errors = service.listExcelErrors(uploadId, requireExcelUser(request));
        StringBuilder csv = new StringBuilder("rowNumber,columnName,inputValue,errorCode,errorReason,correctionGuide\n");
        for (StudentGuidanceExcelErrorRow error : errors) {
            csv.append(error.rowNumber()).append(',').append(error.columnName()).append(',').append(error.inputValue())
                    .append(',').append(error.errorCode()).append(',').append(error.errorReason()).append(',')
                    .append(error.correctionGuide()).append('\n');
        }
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("학생지도_업로드오류_" + uploadId + ".csv", StandardCharsets.UTF_8).build().toString())
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private CurrentUser requireAchievementUser(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user.roles() == null || user.roles().stream().noneMatch(role -> List.of("R01", "R02", "R04").contains(role))) throw new ForbiddenException();
        return user;
    }
    private CurrentUser requireExcelUser(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user.roles() == null || !user.roles().contains("R07")) throw new ForbiddenException();
        return user;
    }
    private CurrentUser currentUser(HttpServletRequest request) {
        Object current = request.getAttribute("currentUser");
        if (!(current instanceof CurrentUser user)) throw new UnauthenticatedException();
        return user;
    }
    private void validatePageSize(int pageSize) {
        if (pageSize != 20 && pageSize != 50 && pageSize != 100) throw new BusinessValidationException("목록 표시 건수가 올바르지 않습니다.", List.of(new ValidationError("pageSize", "20, 50, 100건 중 하나를 선택하세요.")));
    }
    private String traceId(String requestId) { return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId.trim(); }
}
