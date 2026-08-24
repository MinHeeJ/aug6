package kr.ac.knue.commonfoundation.fileoperations;

import java.util.List;

public interface AttachmentStorageInventory {
    List<StorageObjectSnapshot> listStorageObjects(List<AttachmentFileInternalRow> metadataRows);
}
