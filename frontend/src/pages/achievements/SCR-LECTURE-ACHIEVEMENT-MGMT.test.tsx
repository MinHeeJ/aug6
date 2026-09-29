import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { LectureAchievementManagementPage } from "./SCR-LECTURE-ACHIEVEMENT-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return { ...actual, lectureAchievementApi: { list: vi.fn(), save: vi.fn() } };
});

describe("SCR-LECTURE-ACHIEVEMENT-MGMT", () => {
  it("renders the intended direct-route search, list, detail, page-size, and Excel contracts", () => {
    const html = renderToStaticMarkup(<LectureAchievementManagementPage />);
    expect(html).toContain('data-screen-id="SCR-LECTURE-ACHIEVEMENT-MGMT"');
    expect(html).toContain("업적 입력 관리 / 교육영역 / 강의실적 관리");
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
    expect(html).toContain("Excel 다운로드");
    expect(html).toContain('data-testid="lecture-achievement-save-button"');
  });
});
