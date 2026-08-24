package kr.ac.knue.commonfoundation.fileoperations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import org.springframework.stereotype.Service;

@Service
public class AttachmentPolicyValidationService {
    private static final long BYTES_PER_MB = 1024L * 1024L;
    private final FilePolicyMapper filePolicyMapper;

    public AttachmentPolicyValidationService(FilePolicyMapper filePolicyMapper) {
        this.filePolicyMapper = filePolicyMapper;
    }

    public void validateBusinessUpload(String businessType, List<AttachmentUploadCandidate> candidates, int existingFileCount) {
        FilePolicyRow policy = filePolicyMapper.findByBusinessType(businessType);
        if (policy == null) {
            throw new BusinessValidationException("파일정책을 찾을 수 없습니다.",
                    List.of(new ValidationError("businessType", "업무구분에 대한 파일정책이 없습니다.")));
        }
        validateCandidates(policy, candidates, existingFileCount);
    }

    public void validateCandidates(FilePolicyRow policy, List<AttachmentUploadCandidate> candidates, int existingFileCount) {
        List<ValidationError> fields = new ArrayList<>();
        if (candidates == null || candidates.isEmpty()) {
            fields.add(new ValidationError("files", "첨부할 파일을 선택하세요."));
        } else {
            validateCount(policy, candidates, existingFileCount, fields);
            validateTotalSize(policy, candidates, fields);
            validateEachFile(policy, candidates, fields);
        }
        if (!fields.isEmpty()) {
            throw new BusinessValidationException("첨부파일 정책을 위반했습니다.", fields);
        }
    }

    private void validateCount(FilePolicyRow policy, List<AttachmentUploadCandidate> candidates, int existingFileCount, List<ValidationError> fields) {
        if (existingFileCount + candidates.size() > policy.maxFilesPerItem()) {
            fields.add(new ValidationError("files", "건당 첨부개수를 초과했습니다."));
        }
    }

    private void validateTotalSize(FilePolicyRow policy, List<AttachmentUploadCandidate> candidates, List<ValidationError> fields) {
        if (policy.maxTotalSizeMb() == null) {
            return;
        }
        long totalBytes = candidates.stream().mapToLong(AttachmentUploadCandidate::fileSizeBytes).sum();
        if (totalBytes > policy.maxTotalSizeMb() * BYTES_PER_MB) {
            fields.add(new ValidationError("totalSize", "전체 첨부 용량을 초과했습니다."));
        }
    }

    private void validateEachFile(FilePolicyRow policy, List<AttachmentUploadCandidate> candidates, List<ValidationError> fields) {
        Set<String> allowed = new HashSet<>(FilePolicyManagementService.extensionTokens(policy.allowedExtensions()));
        long maxFileBytes = policy.maxFileSizeMb() * BYTES_PER_MB;
        for (AttachmentUploadCandidate candidate : candidates) {
            String filename = candidate.originalFilename() == null ? "" : candidate.originalFilename();
            String extension = normalizeExtension(candidate.extension(), filename);
            if (!allowed.contains(extension)) {
                fields.add(new ValidationError("extension", filename + " 파일 형식은 허용되지 않습니다."));
            }
            if (candidate.fileSizeBytes() > maxFileBytes) {
                fields.add(new ValidationError("fileSize", filename + " 파일 용량이 단일 파일 최대용량을 초과했습니다."));
            }
            if (filename.length() > policy.maxFilenameLength()) {
                fields.add(new ValidationError("filename", filename + " 파일명 길이가 제한을 초과했습니다."));
            }
        }
    }

    private String normalizeExtension(String extension, String filename) {
        String candidate = extension;
        if (candidate == null || candidate.isBlank()) {
            int dot = filename.lastIndexOf('.');
            candidate = dot >= 0 && dot + 1 < filename.length() ? filename.substring(dot + 1) : "";
        }
        candidate = candidate.startsWith(".") ? candidate.substring(1) : candidate;
        return candidate.trim().toLowerCase(Locale.ROOT);
    }
}
