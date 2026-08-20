import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { CodeUsageManagementPage } from "./SCR-CODE-USAGE-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    codeUsageApi: {
      listDetailCodeUsageSettings: vi.fn(async () => ({
        success: true,
        data: {
          settings: [
            {
              groupId: "PROC_STATUS",
              codeValue: "OPEN",
              codeName: "진행",
              systemUseYn: "Y",
              validStartDate: "2026-01-01",
              validEndDate: "2099-12-31",
              status: "ACTIVE",
              selectableForNewInput: true,
            },
            {
              groupId: "PROC_STATUS",
              codeValue: "ENDED",
              codeName: "종료됨",
              systemUseYn: "N",
              validStartDate: "2020-01-01",
              validEndDate: "2026-08-19",
              status: "INACTIVE",
              selectableForNewInput: false,
            },
          ],
          selectableOptions: [
            {
              groupId: "PROC_STATUS",
              codeValue: "OPEN",
              codeName: "진행",
              systemUseYn: "Y",
              validStartDate: "2026-01-01",
              validEndDate: "2099-12-31",
              status: "ACTIVE",
              selectableForNewInput: true,
            },
          ],
          page: 0,
          size: 10,
          totalElements: 2,
        },
        meta: {},
      })),
      saveDetailCodeUsageSettings: vi.fn(async () => ({
        success: true,
        data: [],
        meta: {},
      })),
    },
  };
});

describe("SCR-CODE-USAGE-MGMT", () => {
  it("renders code usage management contract and required state text", () => {
    const html = renderToStaticMarkup(<CodeUsageManagementPage />);

    expect(html).toContain('data-screen-id="SCR-CODE-USAGE-MGMT"');
    expect(html).toContain("코드 사용 관리");
    expect(html).toContain("코드그룹");
    expect(html).toContain("코드 사용 설정 목록");
    expect(html).toContain("코드값");
    expect(html).toContain("코드명");
    expect(html).toContain("적용 시작");
    expect(html).toContain("적용 종료");
    expect(html).toContain("코드 사용 설정을 불러오는 중입니다");
    expect(html).toContain("조회된 코드 사용 설정이 없습니다");
    expect(html).toContain("코드 사용 관리 권한이 없습니다");
    expect(html).toContain("저장되었습니다");
  });

  it("declares regression wording for ended-code exclusion and historical code-name retention", () => {
    const html = renderToStaticMarkup(<CodeUsageManagementPage />);

    expect(html).toContain(
      "종료 또는 미사용 코드는 신규 입력 선택값에서 제외됩니다",
    );
    expect(html).toContain(
      "과거 자료 조회에서는 저장된 코드값의 코드명을 유지합니다",
    );
  });
});
