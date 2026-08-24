package kr.ac.knue.commonfoundation.fileoperations;

public record FilePolicySearchCriteria(
        int page,
        int size,
        String filter) {
    public int offset() {
        return page * size;
    }
}
