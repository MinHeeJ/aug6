package kr.ac.knue.commonfoundation.fileoperations;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FileAttachmentConfirmationService {
    private final FilePolicyMapper filePolicyMapper;
    private final MalwareScanService malwareScanService;

    public FileAttachmentConfirmationService(FilePolicyMapper filePolicyMapper, MalwareScanService malwareScanService) {
        this.filePolicyMapper = filePolicyMapper;
        this.malwareScanService = malwareScanService;
    }

    @Transactional
    public void requirePolicyAndCleanScanBeforeConfirmation(AttachmentFileInternalRow file) {
        FilePolicyRow policy = filePolicyMapper.findByBusinessType(file.businessType());
        if (policy == null) {
            throw new IllegalArgumentException("파일정책을 찾을 수 없습니다.");
        }
        if ("Y".equals(policy.malwareScanEnabled()) && !"CLEAN".equals(file.malwareScanStatus())) {
            malwareScanService.scanAndRequireClean(file);
        }
    }
}
