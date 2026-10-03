import { describe, expect, it, vi } from "vitest";
import { apiRequest } from "../../api/apiClient";

describe("BASIC-83 Excel API request wiring", () => {
  it("keeps multipart boundaries intact for the employment-rate Excel endpoint", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      headers: { get: () => "application/json" },
      json: async () => ({ success: true, data: {}, meta: {} }),
    });
    vi.stubGlobal("fetch", fetchMock);

    const formData = new FormData();
    formData.append(
      "file",
      new File(["managementItemCode,achievementDate"], "employment-rate.csv", {
        type: "text/csv",
      }),
    );

    await apiRequest(
      "/api/business/employment-rate-achievements/excel-uploads",
      {
        body: formData,
        method: "POST",
      },
    );

    const [path, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    const headers = init.headers as Headers;

    expect(path).toBe(
      "/api/business/employment-rate-achievements/excel-uploads",
    );
    expect(headers.has("Content-Type")).toBe(false);
    expect(init.body).toBe(formData);
    expect(init.credentials).toBe("include");
  });
});
