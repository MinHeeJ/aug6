package kr.ac.knue.commonfoundation.fileoperations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class AttachmentIntegrityClassifier {
    public static final String MISSING_BUSINESS_REF = "MISSING_BUSINESS_REF";
    public static final String MISSING_STORAGE_FILE = "MISSING_STORAGE_FILE";
    public static final String DUPLICATE_FILE = "DUPLICATE_FILE";

    public List<AttachmentIntegrityFindingDraft> classify(
            List<AttachmentFileInternalRow> metadataRows,
            List<StorageObjectSnapshot> storageObjects) {
        Set<String> storageRefs = new HashSet<>();
        for (StorageObjectSnapshot storageObject : storageObjects) {
            storageRefs.add(storageObject.storageObjectRef());
        }
        Map<String, List<AttachmentFileInternalRow>> rowsByStorageRef = new HashMap<>();
        for (AttachmentFileInternalRow metadataRow : metadataRows) {
            rowsByStorageRef.computeIfAbsent(metadataRow.storageObjectRef(), ignored -> new ArrayList<>()).add(metadataRow);
        }

        List<AttachmentIntegrityFindingDraft> findings = new ArrayList<>();
        for (String storageRef : storageRefs) {
            if (!rowsByStorageRef.containsKey(storageRef)) {
                findings.add(new AttachmentIntegrityFindingDraft(null, storageRef, MISSING_BUSINESS_REF, "저장소 파일에 연결 업무자료 메타정보가 없습니다."));
            }
        }
        for (AttachmentFileInternalRow metadataRow : metadataRows) {
            String storageRef = metadataRow.storageObjectRef();
            if (!storageRefs.contains(storageRef)) {
                findings.add(new AttachmentIntegrityFindingDraft(metadataRow.fileId(), storageRef, MISSING_STORAGE_FILE, "DB 메타정보의 실제 저장소 파일이 없습니다."));
            }
        }
        for (Map.Entry<String, List<AttachmentFileInternalRow>> entry : rowsByStorageRef.entrySet()) {
            if (entry.getValue().size() > 1) {
                for (AttachmentFileInternalRow metadataRow : entry.getValue()) {
                    findings.add(new AttachmentIntegrityFindingDraft(metadataRow.fileId(), entry.getKey(), DUPLICATE_FILE, "동일 저장소 객체를 참조하는 첨부 메타정보가 중복됩니다."));
                }
            }
        }
        return findings;
    }
}
