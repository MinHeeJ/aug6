package kr.ac.knue.commonfoundation.employmentrateachievements;

/** Opaque-token file boundary for retained Excel originals and validation diagnostics. */
public interface EmploymentRateFileStoragePort {
    void put(String token, byte[] bytes);
    byte[] read(String token);
    void delete(String token);
}
