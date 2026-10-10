package kr.ac.knue.commonfoundation.excel;

/** Stores retained Excel evidence under opaque tokens, never client-controlled paths. */
public interface FileStoragePort {
    String save(byte[] content);
    byte[] read(String token);
    void delete(String token);
}
