import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { DegreeCompletionAchievementManagementPage } from "./SCR-MASTERS-DOCTORAL-GRADUATION-ACHIEVEMENT-MGMT";

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
  it("renders the authorized direct route screen and recipient sub-table controls", () => {
    const html = renderToStaticMarkup(
      <DegreeCompletionAchievementManagementPage />,
    );

    expect(html).toContain(
      'data-screen-id="SCR-MASTERS-DOCTORAL-GRADUATION-ACHIEVEMENT-MGMT',
    );
    expect(html).toContain("석·박사 배출 실적 관리");
    expect(html).toContain("지도학생 세부내역");
    expect(html).toContain("학위구분");
    expect(html).toContain("학생명");
    expect(html).toContain("논문 제목");
    expect(html).toContain("학위수여일");
    expect(html).toContain('data-testid="degree-completion-save-button"');
    expect(html).toContain(
      'data-testid="degree-completion-student-add-button"',
    );
    expect(html).toContain('data-testid="degree-completion-page-size-select"');
  });

  it("uses the relative degree-completion API client", async () => {
    const { degreeCompletionAchievementApi } = await import(
      "../../api/apiClient"
    );

    expect(degreeCompletionAchievementApi.list.toString()).not.toContain(
      "localhost",
    );
  });
});
