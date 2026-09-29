import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { MastersDoctoralGraduationAchievementManagementPage } from "./SCR-MASTERS-DOCTORAL-GRADUATION-ACHIEVEMENT-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    degreeCompletionAchievementApi: { list: vi.fn(), save: vi.fn() },
  };
});

describe("SCR-MASTERS-DOCTORAL-GRADUATION-ACHIEVEMENT-MGMT", () => {
  it("renders the direct-route search, list, detail, and degree student sub-table contracts", () => {
    const html = renderToStaticMarkup(
      <MastersDoctoralGraduationAchievementManagementPage />,
    );
    expect(html).toContain(
      'data-screen-id="SCR-MASTERS-DOCTORAL-GRADUATION-ACHIEVEMENT-MGMT"',
    );
    expect(html).toContain("석·박사 배출 실적 관리");
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
    expect(html).toContain("지도학생 세부내역");
    expect(html).toContain('data-testid="degree-completion-students-table"');
    expect(html).toContain('data-testid="degree-completion-save-button"');
  });
});
