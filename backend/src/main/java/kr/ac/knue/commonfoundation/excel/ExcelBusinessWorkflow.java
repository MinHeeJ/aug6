package kr.ac.knue.commonfoundation.excel;

import kr.ac.knue.commonfoundation.auth.CurrentUser;
import org.springframework.web.multipart.MultipartFile;

/** Business-owned staging/materialization port; shared HTTP code never depends on feature classes. */
public interface ExcelBusinessWorkflow {
    String businessType();
    boolean ownsTemplate(String templateId);
    boolean ownsUpload(String uploadId);
    ExcelTemplateSearchResponse templates(int page, int size, String effectiveDate, CurrentUser user);
    ExcelDownloadFile template(String templateId, CurrentUser user);
    ExcelUploadResult upload(MultipartFile file, CurrentUser user, String requestId);
    ExcelUploadCommitResult commit(String uploadId, CurrentUser user, String requestId);
    ExcelUploadErrorSearchResponse errors(int page, int size, String uploadId, CurrentUser user);
    ExcelDownloadFile errorFile(String uploadId, CurrentUser user);
    ExcelUploadHistorySearchResponse histories(int page, int size, String uploadId,
            String originalFileName, CurrentUser user);
}
