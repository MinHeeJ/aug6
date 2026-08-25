import { describe, expect, it } from "vitest";
import {
  createEmptyManualManagementState,
  getManualManagementRouteContract,
  manualManagementApi,
  reduceManualManagementState,
  type ManualRow,
} from "./SCR-MANUAL-MGMT";

describe("SCR-MANUAL-MGMT contract", () => {
  it("maps route to manual API operations", () => {
    expect(getManualManagementRouteContract()).toEqual({
      route: "/admin/manuals",
      screenId: "SCR-MANUAL-MGMT",
      operations: ["listManuals", "createManual", "downloadManualFile"],
    });
  });

  it("builds relative list and selected-row download paths without sample ids", () => {
    expect(
      manualManagementApi.paths.list({
        manualType: "USER",
        targetUser: "R09",
        effectiveDate: "2026-08-25",
        page: 0,
        size: 20,
      }),
    ).toBe(
      "/api/admin/manuals?page=0&size=20&manualType=USER&targetUser=R09&effectiveDate=2026-08-25",
    );
    expect(manualManagementApi.paths.download(203)).toBe(
      "/api/admin/manuals/203/download",
    );
  });

  it("preserves original file name and file content in create payload", () => {
    expect(
      manualManagementApi.toCreatePayload({
        manualType: "ADMIN",
        version: " 2.0 ",
        targetUser: " R09 ",
        effectiveDate: "2026-09-01",
        originalFileName: " admin-manual.txt ",
        fileContent: "manual body",
        changeReason: " 등록 ",
      }),
    ).toEqual({
      manualType: "ADMIN",
      version: "2.0",
      targetUser: "R09",
      effectiveDate: "2026-09-01",
      originalFileName: "admin-manual.txt",
      fileContent: "manual body",
      changeReason: "등록",
    });
  });

  it("represents previous and latest versions from API rows", () => {
    const latest: ManualRow = {
      manualId: 202,
      manualType: "USER",
      version: "1.1",
      targetUser: "R09",
      effectiveDate: "2026-08-01",
      originalFileName: "user-manual-v1.1.txt",
      latest: true,
    };
    const previous: ManualRow = {
      ...latest,
      manualId: 201,
      version: "1.0",
      latest: false,
    };
    const state = reduceManualManagementState(
      createEmptyManualManagementState(),
      {
        type: "loaded",
        manuals: [latest, previous],
      },
    );

    expect(state.status).toBe("loaded");
    expect(state.manuals.map((row) => row.latest)).toEqual([true, false]);
  });
});
