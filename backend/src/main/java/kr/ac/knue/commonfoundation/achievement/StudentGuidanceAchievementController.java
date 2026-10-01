package kr.ac.knue.commonfoundation.achievement;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.excel.ExcelDownloadFile;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** HTTP boundary for role-scoped student-guidance entry and the R07 Excel validation/commit workflow. */
@RestController
public class StudentGuidanceAchievementController {
    private final StudentGuidanceAchievementService service;
    private final StudentGuidanceExcelService excelService;
    public StudentGuidanceAchievementController(StudentGuidanceAchievementService service, StudentGuidanceExcelService excelService) { this.service = service; this.excelService = excelService; }
    /** Lists individual guidance achievements for R01/R02/R04 callers. */
    @GetMapping("/api/business/student-guidance-achievements")
    public ApiResponse<StudentGuidanceAchievementSearchResponse> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String managementNo, @RequestParam(required = false) String teacherName, @RequestParam(required = false) String managementItemCode, @RequestParam(required = false) LocalDate guidanceDateFrom, @RequestParam(required = false) LocalDate guidanceDateTo, @RequestParam(required = false) String certificationStatus, HttpServletRequest request) { return ApiResponse.ok(service.list(new StudentGuidanceAchievementSearchCriteria(page, size, managementNo, teacherName, managementItemCode, guidanceDateFrom, guidanceDateTo, certificationStatus, null, null), writer(request))); }
    /** Saves individual guidance headers and students after server-side validation. */
    @PostMapping("/api/business/student-guidance-achievements")
    public ApiResponse<StudentGuidanceAchievementRow> save(@Valid @RequestBody SaveStudentGuidanceAchievementRequest body, HttpServletRequest request) { return ApiResponse.ok(service.save(body, writer(request))); }
    /** Downloads the current R07 student-guidance template without exposing storage metadata. */
    @GetMapping("/api/business/student-guidance-achievements/excel-template")
    public ResponseEntity<byte[]> template(HttpServletRequest request) {
        r07(request);
        byte[] content = "교번,관리항목코드,지도시작일,지도종료일,학생명\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("학생지도_업로드양식.csv", java.nio.charset.StandardCharsets.UTF_8).build().toString()).contentType(MediaType.parseMediaType("text/csv")).body(content);
    }
    /** Validates an R07 upload using the fixed STUDENT_GUIDANCE template wrapper. */
    @PostMapping(value = "/api/business/student-guidance-achievements/excel-uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<StudentGuidanceExcelUploadResult> upload(@RequestPart("file") MultipartFile file, HttpServletRequest request) { return ApiResponse.ok(excelService.upload(file, r07(request))); }
    /** Commits a previously validated upload atomically; errors yield 409 before any business row is written. */
    @PostMapping("/api/business/student-guidance-achievements/excel-uploads/{uploadId}/commit")
    public ApiResponse<StudentGuidanceExcelCommitResult> commit(@PathVariable String uploadId, HttpServletRequest request) { return ApiResponse.ok(excelService.commit(uploadId, r07(request))); }
    /** Produces a user-safe CSV containing validation row numbers and correction guidance. */
    @GetMapping("/api/business/student-guidance-achievements/excel-uploads/{uploadId}/errors/download")
    public ResponseEntity<byte[]> downloadErrors(@PathVariable String uploadId, HttpServletRequest request) { ExcelDownloadFile file = excelService.downloadErrors(uploadId, r07(request)); return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(file.originalFileName(), java.nio.charset.StandardCharsets.UTF_8).build().toString()).contentType(MediaType.parseMediaType(file.contentType())).body(file.content()); }
    /** Lists only the authenticated R07 operator's retained student-guidance upload history. */
    @GetMapping("/api/business/student-guidance-achievements/excel-upload-histories")
    public ApiResponse<java.util.List<kr.ac.knue.commonfoundation.excel.ExcelUploadHistoryRow>> histories(HttpServletRequest request) { return ApiResponse.ok(excelService.histories(historyReader(request))); }
    private CurrentUser writer(HttpServletRequest request) { CurrentUser actor = current(request); if (actor.roles().stream().noneMatch(role -> role.equals("R01") || role.equals("R02") || role.equals("R04") || role.equals("R09"))) throw new ForbiddenException(); return actor; }
    private CurrentUser r07(HttpServletRequest request) { CurrentUser actor = current(request); if (!actor.roles().contains("R07") && !actor.roles().contains("R09")) throw new ForbiddenException(); return actor; }
    private CurrentUser historyReader(HttpServletRequest request) { CurrentUser actor = current(request); if (actor.roles().stream().noneMatch(role -> role.equals("R01") || role.equals("R02") || role.equals("R04") || role.equals("R07") || role.equals("R09"))) throw new ForbiddenException(); return actor; }
    private CurrentUser current(HttpServletRequest request) { Object value = request.getAttribute("currentUser"); if (!(value instanceof CurrentUser actor)) throw new UnauthenticatedException(); return actor; }
}
