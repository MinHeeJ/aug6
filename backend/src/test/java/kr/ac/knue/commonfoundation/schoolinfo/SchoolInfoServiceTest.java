package kr.ac.knue.commonfoundation.schoolinfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class SchoolInfoServiceTest {
    @Test
    void serviceDependsOnSchoolInfoPortAndPreservesDisplayedRowsOnly() {
        RecordingSchoolInfoPort port = new RecordingSchoolInfoPort(new SchoolInfoSearchResponse(
                1,
                100,
                1,
                List.of(new SchoolInfoRow("서울특별시교육청", "가락고등학교", "고등학교", "서울", "공립", "서울 송파구 송이로 42", "02-0000-0000"))));
        SchoolInfoService service = new SchoolInfoService(port);

        SchoolInfoSearchResponse response = service.search(new SchoolInfoQuery("가락", "B10", 1, 100));

        assertThat(port.lastQuery).isEqualTo(new SchoolInfoQuery("가락", "B10", 1, 100));
        assertThat(response.displayedCount()).isEqualTo(response.rows().size());
        assertThat(response.rows()).hasSize(1);
    }

    @Test
    void serviceAllowsEmptyApiKeyPathByNotRejectingBlankSearchConditions() {
        RecordingSchoolInfoPort port = new RecordingSchoolInfoPort(new SchoolInfoSearchResponse(1, 100, 0, List.of()));
        SchoolInfoService service = new SchoolInfoService(port);

        SchoolInfoSearchResponse response = service.search(new SchoolInfoQuery("", "", 1, 100));

        assertThat(response.rows()).isEmpty();
        assertThat(port.lastQuery.schoolName()).isEmpty();
    }

    @Test
    void servicePropagatesExternalIntegrationExceptionThroughCommonHandlerLayer() {
        SchoolInfoPort port = query -> { throw new ExternalIntegrationException("NEIS 연결 시간이 초과되었습니다."); };
        SchoolInfoService service = new SchoolInfoService(port);

        assertThatThrownBy(() -> service.search(new SchoolInfoQuery(null, null, 1, 100)))
                .isInstanceOf(ExternalIntegrationException.class)
                .hasMessageContaining("초과");
    }

    static class RecordingSchoolInfoPort implements SchoolInfoPort {
        private final SchoolInfoSearchResponse response;
        private SchoolInfoQuery lastQuery;

        RecordingSchoolInfoPort(SchoolInfoSearchResponse response) {
            this.response = response;
        }

        @Override
        public SchoolInfoSearchResponse search(SchoolInfoQuery query) {
            this.lastQuery = query;
            return response;
        }
    }
}
