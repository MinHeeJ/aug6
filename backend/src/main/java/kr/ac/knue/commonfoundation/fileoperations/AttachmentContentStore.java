package kr.ac.knue.commonfoundation.fileoperations;

public interface AttachmentContentStore {
    byte[] read(AttachmentFileInternalRow file);
}
