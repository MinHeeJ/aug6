package kr.ac.knue.commonfoundation.excel;

/** Opaque, owner-bound references; callers never receive filesystem paths. */
public interface FileStoragePort {
    /** Stores new bytes; implementations compensate new files after transaction rollback. */
    String save(byte[] content, long ownerUserId);
    /** Checks an opaque reference against its owner without exposing a path. */
    boolean isOwned(String reference, long ownerUserId);
    /** Reads bytes only after verifying reference ownership. */
    byte[] readOwned(String reference, long ownerUserId);
}
