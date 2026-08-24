package kr.ac.knue.commonfoundation.common.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CommonPaginationFixtureTest {
    @Test
    void commonListsDefaultToTwentyItems() {
        assertThat(CommonPaginationFixture.defaultSize()).isEqualTo(20);
    }

    @Test
    void commonListsExposeOnlyTwentyFiftyAndOneHundredAsSelectableSizes() {
        assertThat(CommonPaginationFixture.selectableSizes()).containsExactly(20, 50, 100);
    }

    @Test
    void commonListsRejectUnsupportedPageSizes() {
        assertThat(CommonPaginationFixture.supportsSize(10)).isFalse();
        assertThat(CommonPaginationFixture.supportsSize(20)).isTrue();
        assertThat(CommonPaginationFixture.supportsSize(50)).isTrue();
        assertThat(CommonPaginationFixture.supportsSize(100)).isTrue();
        assertThat(CommonPaginationFixture.supportsSize(101)).isFalse();
        assertThatThrownBy(() -> CommonPaginationFixture.requireSupportedSize(30))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("지원하지 않는 목록 표시 건수");
    }
}
