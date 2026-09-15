import { describe, expect, it } from "vitest";

const graduateScreen = import.meta.glob("./SCR-GRADUATE-ACHIEVEMENT.tsx", {
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

describe("SCR-GRADUATE-ACHIEVEMENT", () => {
  it("wires the faculty route to the graduate achievement screen", () => {
    expect(sourceOf(appRouter)).toContain('"/faculty/graduate-achievements"');
    expect(sourceOf(graduateScreen)).toContain(
      'data-screen-id="SCR-GRADUATE-ACHIEVEMENT"',
    );
    expect(sourceOf(graduateScreen)).toContain(
      'data-testid="graduate-achievement-page"',
    );
  });

  it("renders searchable list and required student-detail fields with save confirmation", () => {
    const source = sourceOf(graduateScreen);

    expect(source).toContain("석·박사 배출 실적 목록");
    expect(source).toContain("지도학생 상세 입력");
    expect(source).toContain("학위구분 *");
    expect(source).toContain("학생명 *");
    expect(source).toContain("논문제목 *");
    expect(source).toContain("수여일 *");
    expect(source).toContain("[20, 50, 100]");
    expect(source).toContain("석·박사 배출 실적을 저장하시겠습니까?");
  });

  it("uses only the relative graduate-achievement API path", () => {
    const source = sourceOf(achievementApi);

    expect(source).toContain('"/api/business/graduate-achievements"');
    expect(source).not.toContain("localhost");
  });
});
