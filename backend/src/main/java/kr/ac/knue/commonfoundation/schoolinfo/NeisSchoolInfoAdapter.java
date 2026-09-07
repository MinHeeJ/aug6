package kr.ac.knue.commonfoundation.schoolinfo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class NeisSchoolInfoAdapter implements SchoolInfoPort {
    private final ObjectMapper objectMapper;
    private final String endpointUrl;
    private final String apiKey;
    private final Duration timeout;
    private final HttpClient httpClient;

    public NeisSchoolInfoAdapter(
            ObjectMapper objectMapper,
            @Value("${neis.school-info.endpoint:https://open.neis.go.kr/hub/schoolInfo}") String endpointUrl,
            @Value("${neis.open-api-key:}") String apiKey,
            @Value("${neis.timeout:PT5S}") Duration timeout) {
        this.objectMapper = objectMapper;
        this.endpointUrl = endpointUrl;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.timeout = timeout;
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    @Override
    public SchoolInfoSearchResponse search(SchoolInfoQuery query) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpointUrl + "?" + toQueryString(query)))
                .timeout(timeout)
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ExternalIntegrationException("NEIS 학교기본정보 호출에 실패했습니다. HTTP " + response.statusCode());
            }
            return parseBody(query, response.body());
        } catch (IOException exception) {
            throw new ExternalIntegrationException("NEIS 학교기본정보 연결에 실패했습니다.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ExternalIntegrationException("NEIS 학교기본정보 호출이 중단되었습니다.", exception);
        } catch (IllegalArgumentException exception) {
            throw new ExternalIntegrationException("NEIS 학교기본정보 응답을 해석할 수 없습니다.", exception);
        }
    }

    private SchoolInfoSearchResponse parseBody(SchoolInfoQuery query, String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode topLevelResult = root.path("RESULT");
            if (isResultCode(topLevelResult, "INFO-200")) {
                return new SchoolInfoSearchResponse(query.page(), query.size(), 0, List.of());
            }
            if (!root.has("schoolInfo")) {
                throw resultException(topLevelResult, "NEIS 학교기본정보 응답에 schoolInfo가 없습니다.");
            }
            JsonNode schoolInfo = root.path("schoolInfo");
            JsonNode result = schoolInfo.path(0).path("head").path(1).path("RESULT");
            if (!isResultCode(result, "INFO-000")) {
                throw resultException(result, "NEIS 학교기본정보 조회에 실패했습니다.");
            }
            JsonNode rowsNode = schoolInfo.path(1).path("row");
            List<SchoolInfoRow> rows = new ArrayList<>();
            if (rowsNode.isArray()) {
                for (JsonNode row : rowsNode) {
                    rows.add(new SchoolInfoRow(
                            text(row, "ATPT_OFCDC_SC_NM"),
                            text(row, "SCHUL_NM"),
                            text(row, "SCHUL_KND_SC_NM"),
                            text(row, "LCTN_SC_NM"),
                            text(row, "FOND_SC_NM"),
                            text(row, "ORG_RDNMA"),
                            text(row, "ORG_TELNO")));
                }
            }
            return new SchoolInfoSearchResponse(query.page(), query.size(), rows.size(), rows);
        } catch (IOException exception) {
            throw new ExternalIntegrationException("NEIS 학교기본정보 응답을 해석할 수 없습니다.", exception);
        }
    }

    private RuntimeException resultException(JsonNode result, String fallbackMessage) {
        String code = result.path("CODE").asText("");
        String message = result.path("MESSAGE").asText(fallbackMessage);
        if (!code.isBlank()) {
            return new ExternalIntegrationException("NEIS 오류(" + code + "): " + message);
        }
        return new ExternalIntegrationException(fallbackMessage);
    }

    private boolean isResultCode(JsonNode result, String code) {
        return code.equals(result.path("CODE").asText());
    }

    private String text(JsonNode row, String field) {
        return row.path(field).asText("");
    }

    private String toQueryString(SchoolInfoQuery query) {
        List<String> params = new ArrayList<>();
        params.add(pair("Type", "json"));
        params.add(pair("pIndex", String.valueOf(query.page())));
        params.add(pair("pSize", String.valueOf(query.size())));
        if (!apiKey.isBlank()) {
            params.add(pair("KEY", apiKey));
        }
        if (!query.schoolName().isBlank()) {
            params.add(pair("SCHUL_NM", query.schoolName()));
        }
        if (!query.educationOfficeCode().isBlank()) {
            params.add(pair("ATPT_OFCDC_SC_CODE", query.educationOfficeCode()));
        }
        return String.join("&", params);
    }

    private String pair(String key, String value) {
        return URLEncoder.encode(key, StandardCharsets.UTF_8) + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
