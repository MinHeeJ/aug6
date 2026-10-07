package kr.ac.knue.commonfoundation.employmentrateachievements;

/** Durable opaque, owner-bound storage; callers compensate only references they created. */
public interface FileStoragePort {
    String save(long ownerUserId, String purpose, byte[] content);
    byte[] read(long ownerUserId, String reference);
    boolean exists(long ownerUserId, String reference);
    void delete(long ownerUserId, String reference);
}
