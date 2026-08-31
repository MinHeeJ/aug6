package kr.ac.knue.commonfoundation.basic33;

import java.util.List;

public record AreaElementSystemSearchResponse(
        List<AreaElementSystemRow> areaElementSystems,
        int page,
        int size,
        long totalElements) {
}
