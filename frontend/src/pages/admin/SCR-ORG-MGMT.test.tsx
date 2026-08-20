import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { OrganizationManagementPage } from "./SCR-ORG-MGMT";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    organizationApi: {
      searchOrganizations: vi.fn(async () => ({
        success: true,
        data: [],
        meta: {},
      })),
      getOrganizationTree: vi.fn(async () => ({
        success: true,
        data: [],
        meta: {},
      })),
      listOrganizationParentRelationHistory: vi.fn(async () => ({
        success: true,
        data: [],
        meta: {},
      })),
      saveOrganizationParentRelation: vi.fn(async () => ({
        success: true,
        data: undefined,
        meta: {},
      })),
    },
  };
});

describe("SCR-ORG-MGMT", () => {
  it("renders the organization management route contract and required states", () => {
    const html = renderToStaticMarkup(<OrganizationManagementPage />);

    expect(html).toContain('data-screen-id="SCR-ORG-MGMT"');
    expect(html).toContain("조직 관리");
    expect(html).toContain("검색조건");
    expect(html).toContain("조직 목록");
    expect(html).toContain("사용여부");
    expect(html).toContain("조직 계층");
    expect(html).toContain("선택 조직 관계 편집");
    expect(html).toContain("상위관계 저장");
    expect(html).toContain("조직 관계 변경 이력");
    expect(html).toContain("조직을 선택하세요");
  });

  it("keeps the organization type as a client-side auxiliary filter instead of inventing an API parameter", async () => {
    const { organizationApi } = await import("../../api/apiClient");

    renderToStaticMarkup(<OrganizationManagementPage />);

    expect(organizationApi.searchOrganizations).not.toHaveBeenCalledWith(
      expect.objectContaining({ organizationType: expect.anything() }),
    );
  });
});
