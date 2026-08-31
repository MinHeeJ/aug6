import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it, vi } from "vitest";
import { EvaluationOrganizationMappingPage } from "./SCR-EVALUATION-ORG-MAPPING";

vi.mock("../../api/apiClient", async () => {
  const actual = await vi.importActual<typeof import("../../api/apiClient")>(
    "../../api/apiClient",
  );
  return {
    ...actual,
    evaluationOrganizationMappingApi: {
      listEvaluationOrganizationMappings: vi.fn(async () => ({
        success: true,
        data: {
          mappings: [
            {
              mappingId: 101,
              userId: 2,
              loginId: "teacher",
              userName: "홍길동",
              organizationCode: "COLL-EDU",
              organizationName: "교육대학",
              businessType: "FACULTY_ACHIEVEMENT",
              dataScope: "COLLEGE",
              changeReason: "평가조직 업무권한 연결",
            },
          ],
          page: 0,
          size: 20,
          totalElements: 1,
        },
        meta: {},
      })),
      saveEvaluationOrganizationMapping: vi.fn(async () => ({
        success: true,
        data: undefined,
        meta: {},
      })),
    },
  };
});

describe("SCR-EVALUATION-ORG-MAPPING", () => {
  it("renders evaluation organization mapping route contract and required state text", () => {
    const html = renderToStaticMarkup(<EvaluationOrganizationMappingPage />);

    expect(html).toContain('data-screen-id="SCR-EVALUATION-ORG-MAPPING"');
    expect(html).toContain(
      'data-testid="evaluation-organization-mapping-page"',
    );
    expect(html).toContain("평가조직 매핑");
    expect(html).toContain("업무유형");
    expect(html).toContain("조직코드");
    expect(html).toContain("사용자 ID");
    expect(html).toContain("데이터 범위");
    expect(html).toContain("저장되었습니다");
    expect(html).toContain("조회된 평가조직 매핑이 없습니다");
    expect(html).toContain("평가조직 매핑 권한이 없습니다");
  });

  it("uses relative business mapping API and does not hardcode sample IDs in API calls", async () => {
    const { evaluationOrganizationMappingApi } = await import(
      "../../api/apiClient"
    );
    renderToStaticMarkup(<EvaluationOrganizationMappingPage />);

    expect(
      evaluationOrganizationMappingApi.listEvaluationOrganizationMappings,
    ).not.toHaveBeenCalledWith(
      expect.objectContaining({ url: expect.stringContaining("localhost") }),
    );
    const html = renderToStaticMarkup(<EvaluationOrganizationMappingPage />);
    expect(html).toContain("20건");
    expect(html).toContain("50건");
    expect(html).toContain("100건");
    expect(html).not.toContain(
      "/api/business/evaluation-organization-mappings/USER-1",
    );
  });
});
