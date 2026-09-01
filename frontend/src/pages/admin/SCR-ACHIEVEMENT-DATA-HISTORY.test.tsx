import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import {
  AchievementDataAsOfPage,
  AchievementDataHistoryPage,
} from "./SCR-ACHIEVEMENT-DATA-HISTORY";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    achievementDataHistoryApi: {
      listHistories: vi.fn(async () => ({
        success: true,
        data: {
          histories: [
            {
              historyId: 1,
              achievementType: "BASIC36_RESEARCHER_PROFILE",
              achievementKey: "E1001",
              changeType: "UPDATE",
              fieldName: "degrees",
              beforeValue: "[]",
              afterValue: "[{degreeType=DOCTOR}]",
              changedBy: 4,
              changedByLoginId: "business-owner",
              changedByName: "업무담당자",
              changedAt: "2026-09-01T02:00:00",
              changeReason: "연구자 프로필 탭 저장",
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
        },
        meta: {},
      })),
      listAsOf: vi.fn(async () => ({
        success: true,
        data: {
          snapshots: [
            {
              snapshotId: 1,
              achievementType: "BASIC36_RESEARCHER_PROFILE",
              achievementKey: "E1001",
              employeeNo: "E1001",
              achievementTitle: "연구자 프로필 직접관리 정보",
              achievementStatus: "CERTIFIED",
              snapshotValue: "DOCTOR:한국교원대학교",
              baseAt: "2026-09-01T02:00:00",
              capturedAt: "2026-09-01T02:01:00",
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
        },
        meta: {},
      })),
    },
  };
});

describe("SCR-ACHIEVEMENT-DATA-HISTORY", () => {
  it("renders the history query contract with before and after values", () => {
    const html = renderToStaticMarkup(<AchievementDataHistoryPage />);

    expect(html).toContain('data-screen-id="SCR-ACHIEVEMENT-DATA-HISTORY"');
    expect(html).toContain('data-testid="achievement-data-history-page"');
    expect(html).toContain("업적데이터 변경이력");
    expect(html).toContain("업적 유형");
    expect(html).toContain("업적 식별키");
    expect(html).toContain("교번");
    expect(html).toContain("변경 전 값");
    expect(html).toContain("변경 후 값");
    expect(html).toContain("변경자");
    expect(html).toContain("변경일시");
  });

  it("keeps achievement histories read-only without mutation CTA", () => {
    const html = renderToStaticMarkup(<AchievementDataHistoryPage />);

    expect(html).toContain("조회 전용");
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
    expect(html).not.toContain("저장");
    expect(html).not.toContain(
      'data-testid="achievement-data-history-delete-button"',
    );
  });
});

describe("SCR-ACHIEVEMENT-DATA-AS-OF", () => {
  it("renders the as-of query contract with required base timestamp", () => {
    const html = renderToStaticMarkup(<AchievementDataAsOfPage />);

    expect(html).toContain('data-screen-id="SCR-ACHIEVEMENT-DATA-AS-OF"');
    expect(html).toContain('data-testid="achievement-data-as-of-page"');
    expect(html).toContain("업적데이터 기준시점");
    expect(html).toContain("기준시점");
    expect(html).toContain("snapshot");
    expect(html).toContain("업적 식별키");
    expect(html).toContain("교번");
  });

  it("keeps as-of snapshots read-only and exposes allowed page sizes", () => {
    const html = renderToStaticMarkup(<AchievementDataAsOfPage />);

    expect(html).toContain("조회 전용");
    expect(html).toContain("원본 업적");
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
    expect(html).not.toContain("저장");
    expect(html).not.toContain("삭제 실행");
  });
});
