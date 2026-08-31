import { describe, expect, it, vi } from "vitest";
import {
  apiRequest,
  areaElementSystemApi,
  evaluationAreaApi,
  evaluationElementApi,
  evaluationManagementItemApi,
  menuPermissionApi,
  organizationApi,
} from "./apiClient";

describe("apiRequest", () => {
  it("uses relative /api paths without localhost or docker service names", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      headers: { get: () => "application/json" },
      json: async () => ({ success: true, data: { ok: true }, meta: {} }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await apiRequest("/api/health");

    const requestedUrl = fetchMock.mock.calls[0][0] as string;
    expect(requestedUrl).toBe("/api/health");
    expect(requestedUrl).not.toContain("localhost");
    expect(requestedUrl).not.toContain("backend:8080");
  });

  it("passes accessAllowed as a server-side menu permission filter", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      headers: { get: () => "application/json" },
      json: async () => ({
        success: true,
        data: { permissions: [] },
        meta: {},
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await menuPermissionApi.listMenuPermissions({
      targetType: "ROLE",
      targetId: "R09",
      accessAllowed: "DENY",
      page: 1,
      size: 10,
    });

    const requestedUrl = fetchMock.mock.calls[0][0] as string;
    expect(requestedUrl).toContain("targetType=ROLE");
    expect(requestedUrl).toContain("targetId=R09");
    expect(requestedUrl).toContain("accessAllowed=DENY");
    expect(requestedUrl).toContain("page=1");
  });

  it("retrieves organization parent-relation history through the relative history endpoint", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      headers: { get: () => "application/json" },
      json: async () => ({ success: true, data: [], meta: {} }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await organizationApi.listOrganizationParentRelationHistory(
      "KNUE-DEPT-COMP",
    );

    expect(fetchMock.mock.calls[0][0]).toBe(
      "/api/admin/organizations/KNUE-DEPT-COMP/parent-relations/history?page=0&size=10",
    );
  });

  it("uses the relative evaluation area contract endpoints with dynamic filters", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      headers: { get: () => "application/json" },
      json: async () => ({
        success: true,
        data: { evaluationAreas: [] },
        meta: {},
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await evaluationAreaApi.listEvaluationAreas({
      ruleVersionId: 10,
      activeYn: "Y",
      keyword: "교육",
      page: 0,
      size: 20,
    });
    await evaluationAreaApi.saveEvaluationArea({
      ruleVersionId: 10,
      areaCode: "EDUCATION",
      areaName: "교육",
      sortOrder: 1,
      activeYn: "Y",
      periodApplyMethod: "YEAR",
      changeReason: "평가영역 정비",
    });

    expect(fetchMock.mock.calls[0][0]).toBe(
      "/api/admin/evaluation-areas?page=0&size=20&ruleVersionId=10&activeYn=Y&keyword=%EA%B5%90%EC%9C%A1",
    );
    expect(fetchMock.mock.calls[1][0]).toBe("/api/admin/evaluation-areas/save");
  });

  it("uses the relative evaluation element endpoints with dynamic filters", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      headers: { get: () => "application/json" },
      json: async () => ({
        success: true,
        data: { evaluationElements: [] },
        meta: {},
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await evaluationElementApi.listEvaluationElements({
      ruleVersionId: 10,
      areaCode: "EDUCATION",
      itemCode: "LECTURE",
      evaluationYear: "2026",
      activeYn: "Y",
      keyword: "출석",
      page: 0,
      size: 20,
    });
    await evaluationElementApi.saveEvaluationElement({
      ruleVersionId: 10,
      areaCode: "EDUCATION",
      itemCode: "LECTURE",
      evaluationYear: "2026",
      elementCode: "ATTENDANCE",
      elementName: "출석",
      sortOrder: 1,
      activeYn: "Y",
      changeReason: "평가요소 정비",
    });

    expect(fetchMock.mock.calls[0][0]).toBe(
      "/api/admin/evaluation-elements?page=0&size=20&ruleVersionId=10&areaCode=EDUCATION&itemCode=LECTURE&evaluationYear=2026&activeYn=Y&keyword=%EC%B6%9C%EC%84%9D",
    );
    expect(fetchMock.mock.calls[1][0]).toBe(
      "/api/admin/evaluation-elements/save",
    );
  });

  it("uses the relative evaluation management item endpoints with dynamic filters", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      headers: { get: () => "application/json" },
      json: async () => ({
        success: true,
        data: { evaluationManagementItems: [] },
        meta: {},
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await evaluationManagementItemApi.listEvaluationManagementItems({
      ruleVersionId: 10,
      areaCode: "EDUCATION",
      itemCode: "LECTURE",
      evaluationYear: "2026",
      elementCode: "ATTENDANCE",
      activeYn: "Y",
      keyword: "증빙",
      page: 0,
      size: 20,
    });
    await evaluationManagementItemApi.saveEvaluationManagementItem({
      ruleVersionId: 10,
      areaCode: "EDUCATION",
      itemCode: "LECTURE",
      evaluationYear: "2026",
      elementCode: "ATTENDANCE",
      managementItemCode: "EVIDENCE",
      managementItemName: "증빙파일",
      sortOrder: 1,
      activeYn: "Y",
      teacherEditableYn: "Y",
      requiredYn: "Y",
      dataType: "FILE",
      changeReason: "관리항목 정비",
    });

    expect(fetchMock.mock.calls[0][0]).toBe(
      "/api/admin/evaluation-management-items?page=0&size=20&ruleVersionId=10&areaCode=EDUCATION&itemCode=LECTURE&evaluationYear=2026&elementCode=ATTENDANCE&activeYn=Y&keyword=%EC%A6%9D%EB%B9%99",
    );
    expect(fetchMock.mock.calls[1][0]).toBe(
      "/api/admin/evaluation-management-items/save",
    );
  });

  it("uses the relative area element system endpoints with dynamic filters", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      headers: { get: () => "application/json" },
      json: async () => ({
        success: true,
        data: { areaElementSystems: [] },
        meta: {},
      }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await areaElementSystemApi.listAreaElementSystems({
      ruleVersionId: 10,
      areaCode: "EDUCATION",
      itemCode: "LECTURE",
      evaluationYear: "2026",
      elementCode: "ATTENDANCE",
      activeYn: "Y",
      keyword: "학과",
      page: 0,
      size: 20,
    });
    await areaElementSystemApi.saveAreaElementSystem({
      ruleVersionId: 10,
      areaCode: "EDUCATION",
      itemCode: "LECTURE",
      evaluationYear: "2026",
      elementCode: "ATTENDANCE",
      targetScope: "DEPARTMENT",
      activeYn: "Y",
      changeReason: "영역별 평가요소 체계 정비",
    });

    expect(fetchMock.mock.calls[0][0]).toBe(
      "/api/admin/area-element-systems?page=0&size=20&ruleVersionId=10&areaCode=EDUCATION&itemCode=LECTURE&evaluationYear=2026&elementCode=ATTENDANCE&activeYn=Y&keyword=%ED%95%99%EA%B3%BC",
    );
    expect(fetchMock.mock.calls[1][0]).toBe(
      "/api/admin/area-element-systems/save",
    );
  });
});
