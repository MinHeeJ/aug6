import { describe, expect, it } from "vitest";
import {
  createEmptyFilePolicyState,
  filePolicyApi,
  getFilePolicyRouteContract,
  reduceFilePolicyState,
  validateFilePolicyForm,
} from "./SCR-FILE-POLICY-MGMT";

describe("SCR-FILE-POLICY-MGMT contract and UI behavior", () => {
  it("declares route and relative API operations for file policy management", () => {
    expect(getFilePolicyRouteContract()).toEqual({
      route: "/admin/file-policies",
      screenId: "SCR-FILE-POLICY-MGMT",
      operations: ["listFilePolicies", "saveFilePolicy"],
      menuPath: "시스템 관리 > 시스템 환경설정 > 파일정책 관리",
    });
    expect(
      filePolicyApi.paths.list({ page: 0, size: 20, businessType: "FACULTY" }),
    ).toBe("/api/admin/file-policies?page=0&size=20&businessType=FACULTY");
    expect(filePolicyApi.paths.save()).toBe("/api/admin/file-policies-save");
  });

  it("uses default page size 20 and supports 50 and 100 selectable sizes", () => {
    expect(filePolicyApi.paths.list()).toBe(
      "/api/admin/file-policies?page=0&size=20",
    );
    expect(filePolicyApi.paths.list({ size: 50 })).toContain("size=50");
    expect(filePolicyApi.paths.list({ size: 100 })).toContain("size=100");
  });

  it("represents loading empty error permission and success states", () => {
    const loading = reduceFilePolicyState(createEmptyFilePolicyState(), {
      type: "loading",
    });
    expect(loading.status).toBe("loading");
    const empty = reduceFilePolicyState(loading, {
      type: "loaded",
      policies: [],
      totalElements: 0,
    });
    expect(empty.status).toBe("empty");
    const error = reduceFilePolicyState(empty, {
      type: "error",
      message: "조회 실패",
    });
    expect(error.status).toBe("error");
    const permission = reduceFilePolicyState(error, { type: "permission" });
    expect(permission.status).toBe("permission");
    const success = reduceFilePolicyState(permission, {
      type: "success",
      message: "파일정책이 저장되었습니다.",
    });
    expect(success.status).toBe("success");
    expect(success.message).toBe("파일정책이 저장되었습니다.");
  });

  it("marks required fields and blocks save when mandatory inputs are missing", () => {
    const errors = validateFilePolicyForm({
      businessType: "",
      allowedExtensions: "",
      maxFileSizeMb: 0,
      maxFilesPerItem: 0,
      maxTotalSizeMb: undefined,
      maxFilenameLength: 0,
      malwareScanEnabled: true,
    });

    expect(errors.businessType).toBe("업무구분을 입력하세요.");
    expect(errors.allowedExtensions).toBe("허용 확장자를 입력하세요.");
    expect(errors.maxFileSizeMb).toBe(
      "단일 파일 최대용량은 1MB 이상이어야 합니다.",
    );
    expect(errors.maxFilesPerItem).toBe(
      "건당 첨부개수는 1개 이상이어야 합니다.",
    );
    expect(errors.maxFilenameLength).toBe(
      "파일명 길이는 1자 이상이어야 합니다.",
    );
  });
});
