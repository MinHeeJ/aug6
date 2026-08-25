package kr.ac.knue.commonfoundation.helpcontents;

import java.util.List;

public record HelpContentSearchResponse(
        List<HelpContentRow> helpContents,
        int page,
        int size,
        long totalElements) {
}
