import { describe, expect, it, vi } from "vitest";
import { apiRequest, menuPermissionApi } from "./apiClient";

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
});
