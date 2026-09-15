import { describe, expect, it } from "vitest";

const achievementListFoundation = import.meta.glob(
  "../../components/achievement/AchievementListFoundation.tsx",
  { eager: true, query: "?raw", import: "default" },
);
const achievementApiFoundation = import.meta.glob(
  "../../api/achievementApi.ts",
  { eager: true, query: "?raw", import: "default" },
);

function sourceOf(modules: Record<string, unknown>): string {
  return Object.values(modules)
    .filter((value): value is string => typeof value === "string")
    .join("\n");
}

describe("교육영역 실적 공통 목록 foundation contract", () => {
  it("provides the required 20/50/100 pagination, Excel download, and Korean empty/error states", () => {
    const source = sourceOf(achievementListFoundation);

    expect(source).toContain('data-testid="achievement-list-foundation"');
    expect(source).toContain("[20, 50, 100]");
    expect(source).toContain("엑셀 다운로드");
    expect(source).toContain("조회된 실적이 없습니다");
    expect(source).toContain("다시 시도");
  });

  it("blocks save before each supplied required field has a value and displays field errors", () => {
    const source = sourceOf(achievementListFoundation);

    expect(source).toContain("필수 입력 항목을 확인해 주세요");
    expect(source).toContain("ApiError");
    expect(source).toContain("fields");
    expect(source).toContain("저장하시겠습니까");
  });

  it("keeps achievement API calls relative and derives every path parameter from selected data", () => {
    const source = sourceOf(achievementApiFoundation);

    expect(source).toContain('apiRequest("/api/business/');
    expect(source).not.toContain("localhost");
    expect(source).not.toMatch(/\/(USER|GROUP|MENU|ORG)-\d/);
  });
});
