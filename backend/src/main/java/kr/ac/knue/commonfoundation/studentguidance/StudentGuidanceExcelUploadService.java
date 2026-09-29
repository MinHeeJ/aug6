package kr.ac.knue.commonfoundation.studentguidance;

import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.excel.ExcelDownloadFile;
import kr.ac.knue.commonfoundation.excel.ExcelOperationsService;
import kr.ac.knue.commonfoundation.excel.ExcelUploadCommitResult;
import kr.ac.knue.commonfoundation.excel.ExcelUploadErrorSearchResponse;
import kr.ac.knue.commonfoundation.excel.ExcelUploadHistorySearchResponse;
import kr.ac.knue.commonfoundation.excel.ExcelUploadResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** R07-only facade that fixes the generic Excel workflow to the STUDENT_GUIDANCE template. */
@Service
public class StudentGuidanceExcelUploadService {
    private static final String BUSINESS_TYPE = "STUDENT_GUIDANCE";
    private final ExcelOperationsService excel;
    public StudentGuidanceExcelUploadService(ExcelOperationsService excel) { this.excel = excel; }

    /** Downloads the current STUDENT_GUIDANCE template through the established template storage contract. */
    @Transactional(readOnly = true)
    public ExcelDownloadFile template(CurrentUser user) {
        requireR07(user);
        var templates = excel.listUploadTemplates(0, 20, BUSINESS_TYPE, null).templates();
        if (templates.isEmpty()) throw new kr.ac.knue.commonfoundation.common.api.NotFoundException("학생지도 업로드 양식을 찾을 수 없습니다.");
        return excel.downloadUploadTemplate(templates.get(0).templateId(), user.userId());
    }
    /** Validates a file using the student-guidance template and returns normal and error rows. */
    @Transactional
    public ExcelUploadResult upload(MultipartFile file, CurrentUser user) { requireR07(user); return excel.createExcelUpload(BUSINESS_TYPE, null, file, user.userId()); }
    /** Commits only a validation result without error rows; the shared transaction protects all-or-nothing behavior. */
    @Transactional
    public ExcelUploadCommitResult commit(String uploadId, CurrentUser user) { requireR07(user); return excel.commitExcelUpload(uploadId, user.userId()); }
    /** Limits the history read model to the student-guidance business type at the facade boundary. */
    @Transactional(readOnly = true)
    public ExcelUploadHistorySearchResponse histories(int page, int size, String uploadId, String fileName, CurrentUser user) { requireR07(user); return excel.listExcelUploadHistories(page, size, uploadId, fileName); }
    @Transactional(readOnly = true)
    public ExcelUploadErrorSearchResponse errors(int page, int size, String uploadId, CurrentUser user) { requireR07(user); return excel.listExcelUploadErrors(page, size, uploadId); }
    @Transactional(readOnly = true)
    public ExcelDownloadFile errorFile(String uploadId, CurrentUser user) { requireR07(user); return excel.downloadExcelUploadErrors(uploadId, user.userId()); }
    private void requireR07(CurrentUser user) { if (user == null || user.roles() == null || !user.roles().contains("R07")) throw new ForbiddenException(); }
}
