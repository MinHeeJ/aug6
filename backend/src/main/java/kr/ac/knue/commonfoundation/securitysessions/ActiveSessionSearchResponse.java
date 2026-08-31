package kr.ac.knue.commonfoundation.securitysessions;

import java.util.List;

public record ActiveSessionSearchResponse(List<ActiveSessionRow> sessions, int page, int size, long totalElements) {
}
