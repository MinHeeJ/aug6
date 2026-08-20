import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { EvaluationYearManagementPage } from "./SCR-EVALUATION-YEAR-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    evaluationYearApi: {
      getEvaluationYearSettings: vi.fn(async () => ({
        success: true,
        data: {
          currentEvaluationYear: 2026,
          defaultSearchYear: 2025,
          preparations: [
            {
              targetYear: 2027,
              copyRequestedYn: "Y",
              resetRequestedYn: "N",
              updatedAt: "2026-08-19T09:00:00",
            },
          ],
        },
        meta: {},
      })),
      saveEvaluationYearSettings: vi.fn(async () => ({
        success: true,
        data: {
          currentEvaluationYear: 2026,
          defaultSearchYear: 2025,
          preparations: [],
        },
        meta: {},
      })),
    },
  };
});

describe("SCR-EVALUATION-YEAR-MGMT", () => {
  it("renders evaluation year management contract and required states", () => {
    const html = renderToStaticMarkup(<EvaluationYearManagementPage />);

    expect(html).toContain('data-screen-id="SCR-EVALUATION-YEAR-MGMT"');
    expect(html).toContain("기준연도 관리");
    expect(html).toContain("현재 평가연도");
    expect(html).toContain("기본 조회연도");
    expect(html).toContain("대상연도 준비 상태");
    expect(html).toContain("기준연도 설정을 불러오는 중입니다");
    expect(html).toContain("조회된 대상연도 준비 상태가 없습니다");
    expect(html).toContain("기준연도 관리 권한이 없습니다");
    expect(html).toContain("저장되었습니다");
  });

  it("declares guard wording for reference copy reset and existing evaluation data", () => {
    const html = renderToStaticMarkup(<EvaluationYearManagementPage />);

    expect(html).toContain("기존 평가자료를 삭제하거나 변경하지 않습니다");
    expect(html).toContain("복사와 초기화는 동시에 요청할 수 없습니다");
  });
});
