import { describe, expect, it } from "vitest";

const screen = import.meta.glob("./SCR-STUDENT-GUIDANCE-ACHIEVEMENT.tsx", {
  eager: true,
  query: "?raw",
  import: "default",
});
const router = import.meta.glob("../../app/router.tsx", {
  eager: true,
  query: "?raw",
  import: "default",
});
const api = import.meta.glob("../../api/achievementApi.ts", {
  eager: true,
  query: "?raw",
  import: "default",
});
const text = (source: Record<string, unknown>) =>
  Object.values(source)
    .filter((v): v is string => typeof v === "string")
    .join("\n");

describe("SCR-STUDENT-GUIDANCE-ACHIEVEMENT", () => {
  it("provides a protected student guidance route and full Excel workflow controls", () => {
    expect(text(router)).toContain('"/faculty/student-guidance-achievements"');
    const content = text(screen);
    expect(content).toContain(
      'data-screen-id="SCR-STUDENT-GUIDANCE-ACHIEVEMENT"',
    );
    expect(content).toContain("Excel 일괄등록");
    expect(content).toContain("검증 및 반영");
    expect(content).toContain("[20, 50, 100]");
  });
  it("uses only relative API calls with data-driven student guidance paths", () => {
    const content = text(api);
    expect(content).toContain('"/api/business/student-guidance-achievements"');
    expect(content).not.toContain("localhost");
  });
});
