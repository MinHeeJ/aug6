import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
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
  it("renders the authorized direct route screen with its required states and controls", () => {
    const html = renderToStaticMarkup(<LectureAchievementManagementPage />);

    expect(html).toContain('data-screen-id="SCR-LECTURE-ACHIEVEMENT-MGMT');
    expect(html).toContain("강의실적 관리");
    expect(html).toContain("관리번호");
    expect(html).toContain("성명");
    expect(html).toContain("업적발생일");
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
    expect(html).toContain('data-testid="lecture-achievement-save-button"');
    expect(html).toContain('data-testid="lecture-achievement-excel-button"');

    const source = LectureAchievementManagementPage.toString();
    expect(source).toContain("권한이 없습니다");
    expect(source).toContain("조회된 강의실적이 없습니다");
    expect(source).toContain("저장하시겠습니까");
  });

  it("uses the relative lecture-achievement API client", async () => {
    const { lectureAchievementApi } = await import("../../api/apiClient");

    expect(lectureAchievementApi.list.toString()).not.toContain("localhost");
  });
});
