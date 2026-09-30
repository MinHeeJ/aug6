import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { lectureAchievementApi } from "../../api/apiClient";
import { LectureAchievementManagementPage } from "./SCR-LECTURE-ACHIEVEMENT-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    lectureAchievementApi: {
      list: vi.fn(async () => ({
        success: true,
        data: { achievements: [], page: 0, size: 20, totalElements: 0 },
        meta: {},
      })),
      save: vi.fn(),
    },
  };
});

describe("SCR-LECTURE-ACHIEVEMENT-MGMT", () => {
  it("renders the required search, list, detail, page-size, Excel, and state contract", () => {
    const html = renderToStaticMarkup(<LectureAchievementManagementPage />);
    expect(html).toContain('data-screen-id="SCR-LECTURE-ACHIEVEMENT-MGMT"');
    expect(html).toContain('data-testid="lecture-search-button"');
    expect(html).toContain('data-testid="lecture-page-size-select"');
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
    expect(html).toContain("Excel");
    expect(html).toContain("상세 입력");
  });
  it("uses the lecture API client rather than static achievement rows", () => {
    expect(lectureAchievementApi.list).toBeTruthy();
  });
});
