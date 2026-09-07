package kr.ac.knue.commonfoundation.schoolinfo;

import java.util.List;

public record SchoolInfoSearchResponse(int page, int size, int displayedCount, List<SchoolInfoRow> rows) {
    public SchoolInfoSearchResponse {
        rows = rows == null ? List.of() : List.copyOf(rows);
        displayedCount = rows.size();
    }
}
