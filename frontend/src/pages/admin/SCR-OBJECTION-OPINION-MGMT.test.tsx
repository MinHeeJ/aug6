import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { ObjectionOpinionManagementPage } from "./SCR-OBJECTION-OPINION-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    objectionOpinionApi: {
      listObjectionOpinions: vi.fn(async () => ({
        success: true,
        data: {
          opinions: [
            {
              objectionOpinionId: 801,
              objectionId: 9301,
              evaluationYear: "2026",
              applicantUserId: 2,
              applicantOpinionSnapshot: "평가점수 산정 이의",
              objectionContentSnapshot: "논문 실적 누락 확인 요청",
              reviewerOpinion: "추가 검토 필요",
              decisionResult: "NEEDS_REVIEW",
              processedBy: 1,
              processedAt: "2026-09-02T09:00:00",
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
        },
        meta: {},
      })),
      saveObjectionOpinionsTransition: vi.fn(async () => ({
        success: true,
        data: undefined,
        meta: {},
      })),
    },
  };
});

describe("SCR-OBJECTION-OPINION-MGMT", () => {
  it("renders UI contract, filters, snapshot columns, required opinion fields, and states", () => {
    const html = renderToStaticMarkup(<ObjectionOpinionManagementPage />);
    expect(html).toContain('data-screen-id="SCR-OBJECTION-OPINION-MGMT"');
    expect(html).toContain('data-testid="objection-opinion-page"');
    expect(html).toContain("이의신청 의견 관리");
    expect(html).toContain("평가연도");
    expect(html).toContain("결정결과");
    expect(html).toContain("신청자/이의신청번호");
    expect(html).toContain("신청자 의견");
    expect(html).toContain("이의신청 내용");
    expect(html).toContain("검토자 의견");
    expect(html).toContain("추가검토");
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
    const source = ObjectionOpinionManagementPage.toString();
    expect(source).toContain("원평가 점수 직접 변경은 수행하지 않습니다");
    expect(source).toContain("저장하시겠습니까");
    expect(source).toContain("처리되었습니다");
    expect(source).toContain("이의신청 의견 관리 권한이 없습니다");
    expect(source).toContain("조회된 이의신청 의견 대상이 없습니다");
  });

  it("uses relative API client and exposes Excel download action", async () => {
    const { objectionOpinionApi } = await import("../../api/apiClient");
    renderToStaticMarkup(<ObjectionOpinionManagementPage />);
    expect(objectionOpinionApi.listObjectionOpinions).not.toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining("localhost") }),
    );
    expect(ObjectionOpinionManagementPage.toString()).toContain(
      "objection-opinions.csv",
    );
  });
});
