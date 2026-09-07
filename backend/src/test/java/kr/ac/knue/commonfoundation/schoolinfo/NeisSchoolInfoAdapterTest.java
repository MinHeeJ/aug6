package kr.ac.knue.commonfoundation.schoolinfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class NeisSchoolInfoAdapterTest {
    @Test
    void adapterEncodesKoreanSchoolNameAndParsesInfo000RowsWithoutKey() throws Exception {
        AtomicReference<String> rawQuery = new AtomicReference<>();
        try (StubNeisServer server = StubNeisServer.start(rawQuery, """
                {"schoolInfo":[{"head":[{"list_total_count":200},{"RESULT":{"CODE":"INFO-000","MESSAGE":"정상 처리되었습니다."}}]},{"row":[{"ATPT_OFCDC_SC_NM":"서울특별시교육청","SCHUL_NM":"가락고등학교","SCHUL_KND_SC_NM":"고등학교","LCTN_SC_NM":"서울","FOND_SC_NM":"공립","ORG_RDNMA":"서울 송파구 송이로 42","ORG_TELNO":"02-0000-0000"}]}]}
                """)) {
            NeisSchoolInfoAdapter adapter = new NeisSchoolInfoAdapter(new ObjectMapper(), server.url(), "", Duration.ofSeconds(2));

            SchoolInfoSearchResponse response = adapter.search(new SchoolInfoQuery("가락", "B10", 1, 100));

            assertThat(rawQuery.get()).contains("Type=json", "pIndex=1", "pSize=100", "ATPT_OFCDC_SC_CODE=B10");
            assertThat(rawQuery.get()).contains("SCHUL_NM=%EA%B0%80%EB%9D%BD");
            assertThat(rawQuery.get()).doesNotContain("SCHUL_NM=가락");
            assertThat(rawQuery.get()).doesNotContain("KEY=");
            assertThat(response.displayedCount()).isEqualTo(1);
            assertThat(response.rows().get(0).schoolName()).isEqualTo("가락고등학교");
        }
    }

    @Test
    void adapterTreatsInfo200AsEmptyResult() throws Exception {
        AtomicReference<String> rawQuery = new AtomicReference<>();
        try (StubNeisServer server = StubNeisServer.start(rawQuery, """
                {"RESULT":{"CODE":"INFO-200","MESSAGE":"해당하는 데이터가 없습니다."}}
                """)) {
            NeisSchoolInfoAdapter adapter = new NeisSchoolInfoAdapter(new ObjectMapper(), server.url(), "sampleKey", Duration.ofSeconds(2));

            SchoolInfoSearchResponse response = adapter.search(new SchoolInfoQuery(null, null, 1, 100));

            assertThat(rawQuery.get()).contains("KEY=sampleKey");
            assertThat(response.displayedCount()).isZero();
            assertThat(response.rows()).isEmpty();
        }
    }

    @Test
    void adapterRejectsNeisResultCodesOtherThanInfo000AndInfo200() throws Exception {
        AtomicReference<String> rawQuery = new AtomicReference<>();
        try (StubNeisServer server = StubNeisServer.start(rawQuery, """
                {"RESULT":{"CODE":"ERROR-001","MESSAGE":"서비스 키가 유효하지 않습니다."}}
                """)) {
            NeisSchoolInfoAdapter adapter = new NeisSchoolInfoAdapter(new ObjectMapper(), server.url(), "sampleKey", Duration.ofSeconds(2));

            assertThatThrownBy(() -> adapter.search(new SchoolInfoQuery(null, null, 1, 100)))
                    .isInstanceOf(ExternalIntegrationException.class)
                    .hasMessageContaining("서비스 키");
        }
    }

    record StubNeisServer(HttpServer server, String url) implements AutoCloseable {
        static StubNeisServer start(AtomicReference<String> rawQuery, String body) throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/hub/schoolInfo", exchange -> {
                rawQuery.set(exchange.getRequestURI().getRawQuery());
                byte[] response = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json;charset=UTF-8");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            server.start();
            return new StubNeisServer(server, "http://localhost:" + server.getAddress().getPort() + "/hub/schoolInfo");
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
