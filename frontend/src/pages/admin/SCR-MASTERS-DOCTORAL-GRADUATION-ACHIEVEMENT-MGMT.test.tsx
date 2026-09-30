import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import { MastersDoctoralGraduationAchievementManagementPage } from "./SCR-MASTERS-DOCTORAL-GRADUATION-ACHIEVEMENT-MGMT";

describe("SCR-MASTERS-DOCTORAL-GRADUATION-ACHIEVEMENT-MGMT", () => {
  it("renders the degree completion detail sub-table and authorized entry controls", () => {
    const html = renderToStaticMarkup(
      <MastersDoctoralGraduationAchievementManagementPage />,
    );

    expect(html).toContain(
      'data-testid="masters-doctoral-graduation-achievement-page"',
    );
    expect(html).toContain("석·박사 배출 실적 관리");
    expect(html).toContain('data-testid="degree-completion-student-sub-table"');
    expect(html).toContain('data-testid="degree-completion-degree-type-0"');
    expect(html).toContain('data-testid="degree-completion-student-name-0"');
    expect(html).toContain('data-testid="degree-completion-thesis-title-0"');
    expect(html).toContain('data-testid="degree-completion-awarded-date-0"');
    expect(html).toContain('data-testid="degree-completion-save-button"');
  });
});
