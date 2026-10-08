import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiClientError, apiRequest, authApi } from "./apiClient";

afterEach(() => vi.unstubAllGlobals());

function respond(body: unknown, status = 200) {
  const fetchMock = vi.fn().mockResolvedValue({
    ok: status < 400,
    status,
    headers: { get: () => "application/json" },
    json: async () => body,
  });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

describe("shared education achievement transport compatibility", () => {
  it("lets the browser set the multipart boundary while preserving custom headers", async () => {
    const fetchMock = respond({
      success: true,
      data: { uploadId: "selected-upload" },
      meta: {},
    });
    const form = new FormData();
    form.append("file", new File(["workbook"], "achievements.xlsx"));
    await apiRequest(
      "/api/business/employment-rate-achievements/excel-uploads",
      {
        method: "POST",
        body: form,
        headers: new Headers({
          "Content-Type": "application/json",
          "X-Request-Id": "upload-request",
        }),
      },
    );
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.body).toBe(form);
    expect(init.credentials).toBe("include");
    expect(new Headers(init.headers).has("Content-Type")).toBe(false);
    expect(new Headers(init.headers).get("X-Request-Id")).toBe(
      "upload-request",
    );
  });

  it("preserves JSON login payload and session credentials", async () => {
    const fetchMock = respond({ success: true, data: {}, meta: {} });
    await authApi.login("faculty", "test-only-password");
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(fetchMock.mock.calls[0][0]).toBe("/api/auth/login");
    expect(new Headers(init.headers).get("Content-Type")).toBe(
      "application/json",
    );
    expect(init.credentials).toBe("include");
    expect(JSON.parse(init.body as string).loginId).toBe("faculty");
  });

  it.each([
    { managementItemCode: "필수 항목입니다." },
    [{ field: "managementItemCode", message: "필수 항목입니다." }],
  ])(
    "normalizes new object fields and preserves legacy list fields: %j",
    async (fields) => {
      respond(
        {
          success: false,
          error: { code: "VALIDATION_ERROR", message: "입력 오류", fields },
          meta: { requestId: "same-request" },
        },
        400,
      );
      try {
        await apiRequest("/api/business/course-operations", {
          method: "POST",
          body: "{}",
        });
        expect.fail("Expected the API error");
      } catch (error) {
        expect(error).toBeInstanceOf(ApiClientError);
        expect((error as ApiClientError).status).toBe(400);
        expect((error as ApiClientError).apiError).toEqual({
          code: "VALIDATION_ERROR",
          message: "입력 오류",
          fields: [
            { field: "managementItemCode", message: "필수 항목입니다." },
          ],
        });
      }
    },
  );

  it("preserves request metadata and caller-provided JSON headers", async () => {
    const fetchMock = respond({
      success: true,
      data: {},
      meta: { requestId: "save-request" },
    });
    const response = await apiRequest("/api/business/lecture-improvements/42", {
      method: "PUT",
      body: "{}",
      headers: [["X-Request-Id", "save-request"]],
    });
    expect(response.meta.requestId).toBe("save-request");
    expect(
      new Headers(fetchMock.mock.calls[0][1].headers).get("X-Request-Id"),
    ).toBe("save-request");
  });
});
