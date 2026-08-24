package kr.ac.knue.commonfoundation.fileoperations;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import kr.ac.knue.commonfoundation.auth.CurrentUser;
import kr.ac.knue.commonfoundation.common.api.BusinessValidationException;
import kr.ac.knue.commonfoundation.common.api.ForbiddenException;
import kr.ac.knue.commonfoundation.common.api.ValidationError;
import kr.ac.knue.commonfoundation.common.pagination.CommonPaginationFixture;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttachmentIntegrityService {
    private static final List<String> ALLOWED_ANOMALY_TYPES = List.of(
            AttachmentIntegrityClassifier.MISSING_BUSINESS_REF,
            AttachmentIntegrityClassifier.MISSING_STORAGE_FILE,
            AttachmentIntegrityClassifier.DUPLICATE_FILE);

    private final AttachmentFileMapper fileMapper;
    private final AttachmentIntegrityMapper integrityMapper;
    private final AttachmentStorageInventory storageInventory;
    private final AttachmentIntegrityClassifier classifier;

    public AttachmentIntegrityService(
            AttachmentFileMapper fileMapper,
            AttachmentIntegrityMapper integrityMapper,
            AttachmentStorageInventory storageInventory,
            AttachmentIntegrityClassifier classifier) {
        this.fileMapper = fileMapper;
        this.integrityMapper = integrityMapper;
        this.storageInventory = storageInventory;
        this.classifier = classifier;
    }

    @Transactional
    public AttachmentIntegrityCheckResponse createCheck(CurrentUser currentUser) {
        requireR09(currentUser, "파일 정합성 점검 권한이 없습니다.");
        LocalDateTime startedAt = LocalDateTime.now();
        AttachmentIntegrityCheckRow check = new AttachmentIntegrityCheckRow(null, "RUNNING", currentUser.userId(), startedAt, null);
        integrityMapper.insertCheck(check);
        try {
            List<AttachmentFileInternalRow> metadataRows = fileMapper.listActiveInternalFiles();
            List<StorageObjectSnapshot> storageObjects = storageInventory.listStorageObjects(metadataRows);
            List<AttachmentIntegrityFindingDraft> findings = classifier.classify(metadataRows, storageObjects);
            for (AttachmentIntegrityFindingDraft finding : findings) {
                integrityMapper.insertFinding(check.checkId(), finding);
            }
            integrityMapper.completeCheck(check.checkId(), "COMPLETED");
            return new AttachmentIntegrityCheckResponse(
                    check.checkId(),
                    "COMPLETED",
                    currentUser.userId(),
                    startedAt.toString(),
                    LocalDateTime.now().toString(),
                    findings.size(),
                    findings.stream()
                            .map(AttachmentIntegrityFindingDraft::anomalyType)
                            .collect(java.util.stream.Collectors.collectingAndThen(
                                    java.util.stream.Collectors.toCollection(LinkedHashSet::new),
                                    java.util.ArrayList::new)));
        } catch (RuntimeException exception) {
            if (check.checkId() != null) {
                integrityMapper.completeCheck(check.checkId(), "FAILED");
            }
            if (exception instanceof BusinessValidationException || exception instanceof ForbiddenException) {
                throw exception;
            }
            throw new BusinessValidationException("저장소 정합성 점검 중 오류가 발생했습니다.",
                    List.of(new ValidationError("storage", "저장소 adapter 상태를 확인하세요.")));
        }
    }

    @Transactional(readOnly = true)
    public AttachmentIntegrityResultSearchResponse listResults(
            Long checkId,
            String anomalyType,
            int page,
            int size,
            CurrentUser currentUser) {
        requireR09(currentUser, "파일 정합성 결과 조회 권한이 없습니다.");
        validateAnomalyType(anomalyType);
        int safePage = Math.max(page, 0);
        int safeSize = CommonPaginationFixture.requireSupportedSize(size == 0 ? CommonPaginationFixture.DEFAULT_SIZE : size);
        int offset = safePage * safeSize;
        List<AttachmentIntegrityFindingRow> rows = integrityMapper.listFindings(checkId, anomalyType, safeSize, offset);
        long total = integrityMapper.countFindings(checkId, anomalyType);
        return new AttachmentIntegrityResultSearchResponse(rows, safePage, safeSize, total);
    }

    @Transactional(readOnly = true)
    public AttachmentIntegrityExcelDownload downloadExcel(Long checkId, String anomalyType, CurrentUser currentUser) {
        requireR09(currentUser, "파일 정합성 엑셀 다운로드 권한이 없습니다.");
        validateAnomalyType(anomalyType);
        List<AttachmentIntegrityFindingRow> rows = integrityMapper.listFindingsForExcel(checkId, anomalyType);
        StringBuilder csv = new StringBuilder("점검ID,결과ID,파일ID,저장소객체,이상유형,점검결과,생성일시\n");
        rows.stream()
                .sorted(Comparator.comparing(AttachmentIntegrityFindingRow::findingId))
                .forEach(row -> csv.append(csv(row.checkId()))
                        .append(',').append(csv(row.findingId()))
                        .append(',').append(csv(row.fileId()))
                        .append(',').append(csv(row.storageObjectRef()))
                        .append(',').append(csv(row.anomalyType()))
                        .append(',').append(csv(row.resultMessage()))
                        .append(',').append(csv(row.createdAt()))
                        .append('\n'));
        byte[] content = ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);
        return new AttachmentIntegrityExcelDownload(
                "attachment-integrity-results.csv",
                "text/csv; charset=UTF-8",
                content);
    }

    private void requireR09(CurrentUser currentUser, String message) {
        if (currentUser == null) {
            throw new ForbiddenException();
        }
        if (currentUser.roles().contains("R09")) {
            return;
        }
        throw new ForbiddenException();
    }

    private void validateAnomalyType(String anomalyType) {
        if (anomalyType == null || anomalyType.isBlank()) {
            return;
        }
        if (!ALLOWED_ANOMALY_TYPES.contains(anomalyType)) {
            throw new BusinessValidationException("지원하지 않는 이상유형입니다.",
                    List.of(new ValidationError("anomalyType", "연결자료 없음, 실제파일 없음, 중복파일 중 하나를 선택하세요.")));
        }
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
