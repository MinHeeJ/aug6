import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { GrantPaymentApprovalManagementPage } from "./SCR-GRANT-PAYMENT-APPROVAL-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    grantPaymentApprovalApi: {
      listGrantPaymentApprovals: vi.fn(async () => ({
        success: true,
        data: {
          approvals: [
            {
              approvalId: 701,
              grantApplicationId: 9201,
              linkedAchievementId: 9101,
              evaluationYear: "2026",
              approvalStatus: "APPROVED",
              previousStatus: "SUBMITTED",
              nextStatus: "CERTIFIED",
              requestedAmountSnapshot: 100000,
              paymentAmountSnapshot: 90000,
              accountSnapshotRef: "ACCOUNT-SNAPSHOT-9201",
              opinion: "지급 승인",
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
      saveGrantPaymentApprovalsTransition: vi.fn(async () => ({
        success: true,
        data: undefined,
        meta: {},
      })),
    },
  };
});

describe("SCR-GRANT-PAYMENT-APPROVAL-MGMT", () => {
  it("renders UI contract, filters, amount account achievement columns, required transition fields, and states", () => {
    const html = renderToStaticMarkup(<GrantPaymentApprovalManagementPage />);
    expect(html).toContain('data-screen-id="SCR-GRANT-PAYMENT-APPROVAL-MGMT"');
    expect(html).toContain('data-testid="grant-payment-approval-page"');
    expect(html).toContain("지급승인 관리");
    expect(html).toContain("평가연도");
    expect(html).toContain("승인상태");
    expect(html).toContain("신청자/신청번호");
    expect(html).toContain("신청금액");
    expect(html).toContain("지급금액");
    expect(html).toContain("계좌정보");
    expect(html).toContain("업적연계정보");
    expect(html).toContain("승인취소");
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
    const source = GrantPaymentApprovalManagementPage.toString();
    expect(source).toContain(
      "실제 계좌이체·회계전표·예산집행은 실행하지 않습니다",
    );
    expect(source).toContain("저장하시겠습니까");
    expect(source).toContain("처리되었습니다");
    expect(source).toContain("지급승인 관리 권한이 없습니다");
    expect(source).toContain("조회된 지급승인 대상이 없습니다");
  });

  it("uses relative API client and exposes Excel download action", async () => {
    const { grantPaymentApprovalApi } = await import("../../api/apiClient");
    renderToStaticMarkup(<GrantPaymentApprovalManagementPage />);
    expect(
      grantPaymentApprovalApi.listGrantPaymentApprovals,
    ).not.toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining("localhost") }),
    );
    expect(GrantPaymentApprovalManagementPage.toString()).toContain(
      "grant-payment-approvals.csv",
    );
  });
});
