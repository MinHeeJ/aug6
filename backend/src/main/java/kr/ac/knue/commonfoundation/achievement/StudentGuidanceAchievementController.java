package kr.ac.knue.commonfoundation.achievement;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ApiResponse;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.UnauthenticatedException;
import kr.ac.knue.commonfoundation.excel.ExcelOperationsService;
import kr.ac.knue.commonfoundation.excel.ExcelUploadCommitResult;
import kr.ac.knue.commonfoundation.excel.ExcelUploadHistorySearchResponse;
import kr.ac.knue.commonfoundation.excel.ExcelUploadResult;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Exposes the role-separated FR-027 individual-save and R07 Excel verification/commit operations. */
@RestController
public class StudentGuidanceAchievementController {
    private final StudentGuidanceAchievementService service;
    private final ExcelOperationsService excelService;

    public StudentGuidanceAchievementController(StudentGuidanceAchievementService service, ExcelOperationsService excelService) {
        this.service = service;
        this.excelService = excelService;
    }

    /** Saves an individual student-guidance achievement for R01/R02/R04. */
    @PostMapping("/api/business/student-guidance-achievements")
    public ApiResponse<StudentGuidanceAchievementMapper.StudentGuidanceRow> save(@RequestBody StudentGuidanceSaveRequest request, HttpServletRequest servletRequest) {
        CurrentUser user = currentUser(servletRequest);
        if (user.roles().stream().noneMatch(role -> List.of("R01", "R02", "R04").contains(role))) throw new ForbiddenException();
        return ApiResponse.ok(service.save(request, user));
    }

    /** Validates an R07 student-guidance file through the shared Excel lifecycle using a fixed business type. */
    @PostMapping(value = "/api/business/student-guidance-achievements/excel-uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ExcelUploadResult> upload(@RequestPart("file") MultipartFile file, HttpServletRequest servletRequest) {
        CurrentUser user = requireR07(servletRequest);
        return ApiResponse.ok(excelService.createExcelUpload("STUDENT_GUIDANCE", null, file, user.userId()));
    }

    /** Returns R07's student-guidance upload history without granting generic R09 Excel access. */
    @GetMapping("/api/business/student-guidance-achievements/excel-uploads/histories")
    public ApiResponse<ExcelUploadHistorySearchResponse> histories(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "20") int size,
            HttpServletRequest servletRequest
    ) {
        requireR07(servletRequest);
        return ApiResponse.ok(excelService.listExcelUploadHistories(page, size, null, null));
    }

    /** Commits a previously validated, error-free R07 student-guidance upload. */
    @PostMapping("/api/business/student-guidance-achievements/excel-uploads/{uploadId}/commit")
    public ApiResponse<ExcelUploadCommitResult> commit(@org.springframework.web.bind.annotation.PathVariable String uploadId, HttpServletRequest servletRequest) {
        CurrentUser user = requireR07(servletRequest);
        return ApiResponse.ok(excelService.commitExcelUpload(uploadId, user.userId()));
    }

    private CurrentUser requireR07(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (!user.roles().contains("R07")) throw new ForbiddenException();
        return user;
    }
    private CurrentUser currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (!(user instanceof CurrentUser currentUser)) throw new UnauthenticatedException();
        return currentUser;
    }
}
