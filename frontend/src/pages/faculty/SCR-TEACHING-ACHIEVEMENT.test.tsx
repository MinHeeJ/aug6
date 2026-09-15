import { describe, expect, it } from "vitest";

const teachingScreen = import.meta.glob("./SCR-TEACHING-ACHIEVEMENT.tsx", {
  eager: true,
  query: "?raw",
  import: "default",
});
const appRouter = import.meta.glob("../../app/router.tsx", {
  eager: true,
  query: "?raw",
  import: "default",
});
const achievementApi = import.meta.glob("../../api/achievementApi.ts", {
  eager: true,
  query: "?raw",
  import: "default",
});

function sourceOf(modules: Record<string, unknown>): string {
  return Object.values(modules)
    .filter((value): value is string => typeof value === "string")
    .join("\n");
}

describe("SCR-TEACHING-ACHIEVEMENT", () => {
  it("wires the faculty route to the lecture-achievement screen", () => {
    expect(sourceOf(appRouter)).toContain('"/faculty/teaching-achievements"');
    expect(sourceOf(teachingScreen)).toContain(
      'data-screen-id="SCR-TEACHING-ACHIEVEMENT"',
    );
    expect(sourceOf(teachingScreen)).toContain(
      'data-testid="teaching-achievement-page"',
    );
  });

  it("implements the list and detail workflow with required values and save feedback", () => {
    const source = sourceOf(teachingScreen);

    expect(source).toContain("검색조건");
    expect(source).toContain("강의실적 목록");
    expect(source).toContain("상세 입력");
    expect(source).toContain("강좌유형 *");
    expect(source).toContain("학점시수 *");
    expect(source).toContain("[20, 50, 100]");
    expect(source).toContain("저장하시겠습니까");
    expect(source).toContain("저장되었습니다");
    expect(source).toContain("selectRow(row)");
    expect(source).toContain("achievementId: editingAchievementId");
  });

  it("uses only the relative teaching-achievement API path", () => {
    const source = sourceOf(achievementApi);

    expect(source).toContain('"/api/business/teaching-achievements"');
    expect(source).not.toContain("localhost");
  });
});
