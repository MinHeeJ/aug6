package kr.ac.knue.commonfoundation.schoolinfo;

public record SchoolInfoQuery(String schoolName, String educationOfficeCode, int page, int size) {
    public SchoolInfoQuery {
        schoolName = normalize(schoolName);
        educationOfficeCode = normalize(educationOfficeCode);
        page = page < 1 ? 1 : page;
        size = size < 1 ? 100 : Math.min(size, 100);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
