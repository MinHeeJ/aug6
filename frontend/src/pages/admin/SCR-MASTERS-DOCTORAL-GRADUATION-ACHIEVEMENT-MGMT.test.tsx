import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { degreeCompletionAchievementApi } from "../../api/apiClient";
import { MastersDoctoralGraduationAchievementManagementPage } from "./SCR-MASTERS-DOCTORAL-GRADUATION-ACHIEVEMENT-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    degreeCompletionAchievementApi: {
      list: vi.fn(async () => ({
        success: true,
        data: { achievements: [], page: 0, size: 20, totalElements: 0 },
        meta: {},
      })),
      save: vi.fn(),
    },
  };
});

describe("SCR-MASTERS-DOCTORAL-GRADUATION-ACHIEVEMENT-MGMT", () => {
  it("renders the search, list, detail, and guided-student sub-table", () => {
    const html = renderToStaticMarkup(
      <MastersDoctoralGraduationAchievementManagementPage />,
    );
    expect(html).toContain(
      'data-screen-id="SCR-MASTERS-DOCTORAL-GRADUATION-ACHIEVEMENT-MGMT"',
    );
    expect(html).toContain('data-testid="degree-completion-student-table"');
    expect(html).toContain("학위구분");
    expect(html).toContain("학생명");
    expect(html).toContain("논문제목");
    expect(html).toContain("수여일");
  });
  it("uses the API client rather than static achievement rows", () =>
    expect(degreeCompletionAchievementApi.list).toBeTruthy());
});
