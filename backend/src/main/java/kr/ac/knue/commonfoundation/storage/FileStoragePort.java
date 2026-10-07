package kr.ac.knue.commonfoundation.storage;

/** Opaque byte storage; paths and client filenames never cross the storage boundary. */
public interface FileStoragePort {
    String save(byte[] bytes);
    byte[] read(String token);
    boolean exists(String token);
    long size(String token);
    void delete(String token);
}
