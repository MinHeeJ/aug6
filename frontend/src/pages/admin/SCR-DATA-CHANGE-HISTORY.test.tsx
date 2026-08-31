import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { DataChangeHistoryPage } from "./SCR-DATA-CHANGE-HISTORY";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    dataChangeHistoryApi: {
      listDataChangeHistories: vi.fn(async () => ({
        success: true,
        data: {
          histories: [
            {
              historyId: 100,
              targetBusiness: "rejection_reasons",
              targetKey: "FACULTY_ACHIEVEMENT:DEPT_REVIEW_REQUIRED",
              changeType: "UPDATE",
              fieldName: "standard_message",
              beforeValue: "기존 문구",
              afterValue: "학과장 검토 의견이 필요합니다.",
              changedBy: 1,
              changedByLoginId: "admin",
              changedByName: "시스템관리자",
              changedAt: "2026-08-31T09:00:00",
              changeReason: "반려사유 정비",
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
        },
        meta: {},
      })),
    },
  };
});

describe("SCR-DATA-CHANGE-HISTORY", () => {
  it("renders data change history route contract and read-only states", () => {
    const html = renderToStaticMarkup(<DataChangeHistoryPage />);

    expect(html).toContain('data-screen-id="SCR-DATA-CHANGE-HISTORY"');
    expect(html).toContain('data-testid="data-change-history-page"');
    expect(html).toContain("데이터 변경 이력");
    expect(html).toContain("대상 업무·기준정보");
    expect(html).toContain("대상 식별정보");
    expect(html).toContain("처리자");
    expect(html).toContain("변경일시");
    expect(html).toContain("변경 전 값");
    expect(html).toContain("변경 후 값");
    expect(html).toContain("조회 전용");
    expect(html).not.toContain('data-testid="data-change-history-save-button"');
    expect(html).not.toContain(
      'data-testid="data-change-history-delete-button"',
    );
  });

  it("exposes allowed page sizes and avoids absolute API URLs", async () => {
    const { dataChangeHistoryApi } = await import("../../api/apiClient");
    renderToStaticMarkup(<DataChangeHistoryPage />);

    expect(
      dataChangeHistoryApi.listDataChangeHistories,
    ).not.toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining("localhost") }),
    );
    const html = renderToStaticMarkup(<DataChangeHistoryPage />);
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
  });
});
