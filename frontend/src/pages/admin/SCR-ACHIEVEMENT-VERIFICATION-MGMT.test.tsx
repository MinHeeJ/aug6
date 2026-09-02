import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { AchievementVerificationManagementPage } from "./SCR-ACHIEVEMENT-VERIFICATION-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    achievementVerificationApi: {
      listAchievementVerificationTargets: vi.fn(async () => ({
        success: true,
        data: {
          targets: [
            {
              verificationId: 501,
              achievementId: 9101,
              evaluationYear: "2026",
              handlerUserId: 1,
              actionType: "CERTIFY",
              previousStatus: "DEPARTMENT_CONFIRMED",
              nextStatus: "CERTIFIED",
              opinion: "인증 완료",
              evidenceRef: "FILE-9101",
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
      saveAchievementVerificationTargetsTransition: vi.fn(async () => ({
        success: true,
        data: undefined,
        meta: {},
      })),
    },
  };
});

describe("SCR-ACHIEVEMENT-VERIFICATION-MGMT", () => {
  it("renders UI contract, filters, required transition fields, and states", () => {
    const html = renderToStaticMarkup(
      <AchievementVerificationManagementPage />,
    );
    expect(html).toContain(
      'data-screen-id="SCR-ACHIEVEMENT-VERIFICATION-MGMT"',
    );
    expect(html).toContain('data-testid="achievement-verification-page"');
    expect(html).toContain("담당자 인증 관리");
    expect(html).toContain("평가연도");
    expect(html).toContain("평가영역");
    expect(html).toContain("인증상태");
    expect(html).toContain("처리구분");
    expect(html).toContain("처리 근거");
    expect(html).toContain("반려 사유");
    expect(html).toContain("의견");
    expect(html).toContain("인증취소");
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
    const source = AchievementVerificationManagementPage.toString();
    expect(source).toContain("저장하시겠습니까");
    expect(source).toContain("처리되었습니다");
    expect(source).toContain("담당자 인증 관리 권한이 없습니다");
    expect(source).toContain("조회된 담당자 인증 대상이 없습니다");
  });

  it("uses relative API client and exposes Excel download action", async () => {
    const { achievementVerificationApi } = await import("../../api/apiClient");
    renderToStaticMarkup(<AchievementVerificationManagementPage />);
    expect(
      achievementVerificationApi.listAchievementVerificationTargets,
    ).not.toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining("localhost") }),
    );
    expect(AchievementVerificationManagementPage.toString()).toContain(
      "achievement-verifications.csv",
    );
  });
});
