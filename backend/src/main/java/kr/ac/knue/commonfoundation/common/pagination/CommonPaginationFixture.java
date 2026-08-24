package kr.ac.knue.commonfoundation.common.pagination;

import java.util.List;
import java.util.Set;

public final class CommonPaginationFixture {
    public static final int DEFAULT_SIZE = 20;
    public static final List<Integer> SELECTABLE_SIZES = List.of(20, 50, 100);

    private static final Set<Integer> SELECTABLE_SIZE_SET = Set.copyOf(SELECTABLE_SIZES);

    private CommonPaginationFixture() {
    }

    public static int defaultSize() {
        return DEFAULT_SIZE;
    }

    public static List<Integer> selectableSizes() {
        return SELECTABLE_SIZES;
    }

    public static boolean supportsSize(int size) {
        return SELECTABLE_SIZE_SET.contains(size);
    }

    public static int requireSupportedSize(int size) {
        if (!supportsSize(size)) {
            throw new IllegalArgumentException("지원하지 않는 목록 표시 건수입니다: " + size);
        }
        return size;
    }
}
