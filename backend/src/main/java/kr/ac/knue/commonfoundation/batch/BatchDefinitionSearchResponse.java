package kr.ac.knue.commonfoundation.batch;

import java.util.List;

public record BatchDefinitionSearchResponse(List<BatchDefinitionRow> definitions, int page, int size, long totalElements) {
}
