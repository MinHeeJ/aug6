package kr.ac.knue.commonfoundation.basic36;

import java.util.List;

public record FullTimeFacultyStatusSearchResponse(List<FullTimeFacultyStatusRow> statuses, int page, int pageSize,
                                                  long totalElements, int baseYear) {
}
