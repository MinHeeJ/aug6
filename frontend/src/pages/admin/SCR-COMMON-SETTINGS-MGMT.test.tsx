import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { CommonSettingsManagementPage } from "./SCR-COMMON-SETTINGS-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    commonSystemSettingsApi: {
      getCommonSystemSettings: vi.fn(async () => ({
        success: true,
        data: {
          settings: [
            {
              settingKey: "SESSION_IDLE_MINUTES",
              settingValue: "30",
              unit: "minutes",
            },
            { settingKey: "PAGE_SIZE", settingValue: "20", unit: "rows" },
            {
              settingKey: "DEFAULT_SEARCH_PERIOD",
              settingValue: "30",
              unit: "days",
            },
            {
              settingKey: "BULK_QUERY_THRESHOLD",
              settingValue: "1000",
              unit: "rows",
            },
            {
              settingKey: "LONG_TASK_NOTICE_THRESHOLD",
              settingValue: "60",
              unit: "seconds",
            },
          ],
        },
        meta: {},
      })),
      saveCommonSystemSettings: vi.fn(async () => ({
        success: true,
        data: { settings: [] },
        meta: {},
      })),
    },
  };
});

describe("SCR-COMMON-SETTINGS-MGMT", () => {
  it("renders common settings route contract and all five global setting labels", () => {
    const html = renderToStaticMarkup(<CommonSettingsManagementPage />);

    expect(html).toContain('data-screen-id="SCR-COMMON-SETTINGS-MGMT"');
    expect(html).toContain("공통 환경설정");
    expect(html).toContain("세션 유휴시간");
    expect(html).toContain("페이지당 조회건수");
    expect(html).toContain("기본 검색기간");
    expect(html).toContain("대량조회 기준건수");
    expect(html).toContain("장시간작업 안내 기준");
  });

  it("declares OQ gate and global-only state wording without user-specific settings", () => {
    const html = renderToStaticMarkup(<CommonSettingsManagementPage />);

    expect(html).toContain("OQ-SET-001");
    expect(html).toContain("전역 설정만 저장합니다");
    expect(html).toContain("공통 환경설정 정보를 불러오는 중입니다");
    expect(html).toContain("공통 환경설정 권한이 없습니다");
    expect(html).toContain("저장되었습니다");
  });
});
