import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { KorusFacultySyncPage } from "./SCR-KORUS-FACULTY-SYNC";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    korusFacultySyncApi: {
      listResults: vi.fn(async () => ({
        success: true,
        data: {
          results: [
            {
              resultId: 700,
              runId: 70,
              requestId: "REQ-KORUS-001",
              employeeNo: "E1001",
              name: "홍길동",
              organizationCode: "KNUE-DEPT-COMP",
              rankName: "교수",
              appointmentId: "E1001-APPT",
              syncStatus: "FAILED",
              errorMessage: "조직 매핑 실패",
              createdAt: "2026-01-02T03:04:05",
            },
          ],
          page: 0,
          pageSize: 20,
          totalElements: 1,
        },
        meta: {},
      })),
      createRun: vi.fn(async () => ({
        success: true,
        data: { requestId: "REQ-B36-KORUS-RUN", runType: "MANUAL" },
        meta: { requestId: "REQ-B36-KORUS-RUN" },
      })),
      retryResult: vi.fn(async () => ({
        success: true,
        data: { requestId: "REQ-B36-KORUS-RETRY", runType: "RETRY" },
        meta: { requestId: "REQ-B36-KORUS-RETRY" },
      })),
    },
  };
});

describe("SCR-KORUS-FACULTY-SYNC", () => {
  it("renders the KORUS faculty sync route contract and required states", () => {
    const html = renderToStaticMarkup(<KorusFacultySyncPage />);

    expect(html).toContain('data-screen-id="SCR-KORUS-FACULTY-SYNC"');
    expect(html).toContain('data-testid="korus-faculty-sync-page"');
    expect(html).toContain(
      "교수업적평가 / 연계 관리 / KORUS 교원 기본정보 연계",
    );
    expect(html).toContain("대상기간 시작");
    expect(html).toContain("대상기간 종료");
    expect(html).toContain("request_id");
    expect(html).toContain("employee_no");
    expect(html).toContain("organization_code");
    expect(html).toContain("오류내용");
    expect(html).toContain("KORUS 원천 스냅샷은 수정할 수 없습니다");
  });

  it("exposes manual sync, failed retry, pagination sizes, and relative API client usage", async () => {
    const { korusFacultySyncApi } = await import("../../api/apiClient");
    const html = renderToStaticMarkup(<KorusFacultySyncPage />);

    expect(html).toContain("수동 동기화");
    expect(html).toContain("재처리");
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
    expect(korusFacultySyncApi.listResults).not.toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining("localhost") }),
    );
  });
});
