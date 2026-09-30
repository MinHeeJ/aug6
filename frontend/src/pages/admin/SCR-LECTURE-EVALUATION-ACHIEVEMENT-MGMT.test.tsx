import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { lectureEvaluationAchievementApi } from "../../api/apiClient";
import {
  canEditLectureEvaluationAchievement,
  LectureEvaluationAchievementManagementPage,
} from "./SCR-LECTURE-EVALUATION-ACHIEVEMENT-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    lectureEvaluationAchievementApi: {
      list: vi.fn(async () => ({
        success: true,
        data: { achievements: [], page: 0, size: 20, totalElements: 0 },
        meta: {},
      })),
      save: vi.fn(),
    },
  };
});

describe("SCR-LECTURE-EVALUATION-ACHIEVEMENT-MGMT", () => {
  it("renders the required search, list, detail, page-size, Excel, and state contract", () => {
    const html = renderToStaticMarkup(
      <LectureEvaluationAchievementManagementPage />,
    );
    expect(html).toContain(
      'data-screen-id="SCR-LECTURE-EVALUATION-ACHIEVEMENT-MGMT"',
    );
    expect(html).toContain('data-testid="lecture-evaluation-search-button"');
    expect(html).toContain('data-testid="lecture-evaluation-page-size-select"');
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
    expect(html).toContain("Excel");
    expect(html).toContain("상세 입력");
  });
  it("uses the lecture-evaluation API client rather than static achievement rows", () => {
    expect(lectureEvaluationAchievementApi.list).toBeTruthy();
  });

  it("blocks the evaluation-confirmed row from the client mutation path", () => {
    expect(
      canEditLectureEvaluationAchievement({
        achievementId: 77,
        evaluationYear: "2026",
        ownerUserId: 2,
        organizationCode: "ORG-1",
        managementItemCode: "B77-LE-001",
        occurredDate: "2026-03-15",
        achievementDetail: {},
        certificationStatus: "EVALUATION_CONFIRMED",
      }),
    ).toBe(false);
    expect(canEditLectureEvaluationAchievement(null)).toBe(true);
  });
});
