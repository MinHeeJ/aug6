import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";

vi.mock("../app/AuthProvider", () => ({
  useAuth: () => ({ user: { roles: ["R01"] } }),
}));

import { DegreeCompletionAchievementPage } from "./SCR-MASTERS-DOCTORAL-GRADUATION-ACHIEVEMENT-MGMT";

describe("SCR-MASTERS-DOCTORAL-GRADUATION-ACHIEVEMENT-MGMT", () => {
  it("keeps the degree-student detail columns and save action reachable from the screen", () => {
    const html = renderToStaticMarkup(<DegreeCompletionAchievementPage />);

    expect(html).toContain(
      'data-testid="degree-completion-achievement-screen"',
    );
    expect(html).toContain("석·박사 배출 실적 관리");
    expect(html).toContain("학위구분");
    expect(html).toContain("학생명");
    expect(html).toContain("논문제목");
    expect(html).toContain("학위수여일");
    expect(html).toContain('data-testid="degree-completion-save-button"');
  });
});
