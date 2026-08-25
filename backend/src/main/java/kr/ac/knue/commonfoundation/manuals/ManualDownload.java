package kr.ac.knue.commonfoundation.manuals;

public record ManualDownload(Long manualId, String originalFileName, byte[] fileContent) {
}
