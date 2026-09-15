import { describe, expect, it } from "vitest";

const teachingEvaluationScreen = import.meta.glob(
  "./SCR-TEACHING-EVALUATION-ACHIEVEMENT.tsx",
  { eager: true, query: "?raw", import: "default" },
);
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

describe("SCR-TEACHING-EVALUATION-ACHIEVEMENT", () => {
  it("wires the faculty route to the teaching evaluation screen", () => {
    const routerSource = sourceOf(appRouter);
    const screenSource = sourceOf(teachingEvaluationScreen);

    expect(routerSource).toContain(
      '"/faculty/teaching-evaluation-achievements"',
    );
    expect(screenSource).toContain(
      'data-screen-id="SCR-TEACHING-EVALUATION-ACHIEVEMENT"',
    );
    expect(screenSource).toContain(
      'data-testid="teaching-evaluation-achievement-page"',
    );
  });

  it("implements the shared search, list, detail, tab, validation and save-feedback contract", () => {
    const source = sourceOf(teachingEvaluationScreen);

    expect(source).toContain("검색조건");
    expect(source).toContain("강의평가 실적 목록");
    expect(source).toContain("상세 입력");
    expect(source).toContain("첨부파일");
    expect(source).toContain("필수 입력 항목을 확인해 주세요");
    expect(source).toContain("저장하시겠습니까");
    expect(source).toContain("저장되었습니다");
    expect(source).toContain("[20, 50, 100]");
  });

  it("uses only the relative teaching evaluation API path", () => {
    const source = sourceOf(achievementApi);

    expect(source).toContain(
      '"/api/business/teaching-evaluation-achievements"',
    );
    expect(source).not.toContain("localhost");
  });
});
