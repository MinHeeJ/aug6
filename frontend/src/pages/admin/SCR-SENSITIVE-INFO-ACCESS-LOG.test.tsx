import { describe, expect, it } from "vitest";
import {
  createEmptySensitiveInformationAccessLogState,
  getSensitiveInformationAccessLogRouteContract,
  reduceSensitiveInformationAccessLogState,
  sensitiveInformationAccessLogApi,
} from "./SCR-SENSITIVE-INFO-ACCESS-LOG";

describe("SCR-SENSITIVE-INFO-ACCESS-LOG route contract and state handling", () => {
  it("declares sensitive information access log route and relative list operation", () => {
    expect(getSensitiveInformationAccessLogRouteContract()).toEqual({
      route: "/admin/audit/sensitive-information-access-logs",
      screenId: "SCR-SENSITIVE-INFO-ACCESS-LOG",
      operations: ["listSensitiveInformationAccessLogs"],
    });
    expect(
      sensitiveInformationAccessLogApi.paths.list({
        filter: "admin",
        informationType: "PERSONAL_INFORMATION",
        viewerUserId: "1",
        accessResult: "SUCCESS",
        fromDate: "2026-08-01",
        toDate: "2026-08-31",
        page: 0,
        size: 20,
      }),
    ).toBe(
      "/api/admin/audit/sensitive-information-access-logs?page=0&size=20&filter=admin&informationType=PERSONAL_INFORMATION&viewerUserId=1&accessResult=SUCCESS&fromDate=2026-08-01&toDate=2026-08-31",
    );
  });

  it("represents loading empty error and permission states", () => {
    const loading = reduceSensitiveInformationAccessLogState(
      createEmptySensitiveInformationAccessLogState(),
      { type: "loading" },
    );
    expect(loading.status).toBe("loading");
    const empty = reduceSensitiveInformationAccessLogState(loading, {
      type: "loaded",
      logs: [],
    });
    expect(empty.status).toBe("empty");
    const error = reduceSensitiveInformationAccessLogState(empty, {
      type: "error",
      message: "조회 실패",
    });
    expect(error.status).toBe("error");
    const permission = reduceSensitiveInformationAccessLogState(error, {
      type: "permission",
    });
    expect(permission.status).toBe("permission");
  });

  it("uses default 20 rows and exposes only 20/50/100 page size options", () => {
    expect(sensitiveInformationAccessLogApi.paths.list()).toBe(
      "/api/admin/audit/sensitive-information-access-logs?page=0&size=20",
    );
    expect([...sensitiveInformationAccessLogApi.pageSizeOptions]).toEqual([
      20, 50, 100,
    ]);
  });

  it("exposes readonly filters and never includes protected source values or mutation actions", () => {
    expect([
      ...sensitiveInformationAccessLogApi.informationTypeOptions,
    ]).toEqual([
      "",
      "PERSONAL_EVALUATION_RESULT",
      "SCORE_CALCULATION",
      "PERSONAL_INFORMATION",
      "ACCOUNT_INFORMATION",
    ]);
    expect([...sensitiveInformationAccessLogApi.accessResultOptions]).toEqual([
      "",
      "SUCCESS",
      "FAILURE",
    ]);
    expect("create" in sensitiveInformationAccessLogApi).toBe(false);
    expect("update" in sensitiveInformationAccessLogApi).toBe(false);
    expect("delete" in sensitiveInformationAccessLogApi).toBe(false);
    expect(JSON.stringify(sensitiveInformationAccessLogApi)).not.toMatch(
      /protectedPlainValue|accountNumberPlain|residentRegistrationNumber/,
    );
  });
});
