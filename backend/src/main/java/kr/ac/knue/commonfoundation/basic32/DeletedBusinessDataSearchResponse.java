package kr.ac.knue.commonfoundation.basic32;

import java.util.List;

public record DeletedBusinessDataSearchResponse(
        List<DeletedBusinessDataRow> deletedData,
        int page,
        int size,
        long totalElements) {
}
