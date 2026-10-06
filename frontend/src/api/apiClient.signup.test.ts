import { afterEach, describe, expect, it, vi } from "vitest";
import {
  ApiClientError,
  authApi,
  checkUserIdAvailability,
  signupApi,
} from "./apiClient";
import { signupApi as sliceSignupApi } from "../pages/signup/signupApi";
import { checkUserIdAvailability as sliceAvailability } from "../pages/availability/availabilityApi";

function response(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

afterEach(() => {
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});

describe("signup API registry", () => {
  it("reuses the slice adapters and posts selected form data with the shared envelope", async () => {
    expect(signupApi).toBe(sliceSignupApi);
    expect(checkUserIdAvailability).toBe(sliceAvailability);
    expect(signupApi.checkUserId).toBe(checkUserIdAvailability);
    const data = { userId: "registryuser", message: "가입이 완료되었습니다." };
    const fetch = vi
      .fn()
      .mockResolvedValue(response({ success: true, data, meta: {} }, 201));
    vi.stubGlobal("fetch", fetch);
    const request = {
      userId: "registryuser",
      password: "RegistryPass9!",
      passwordConfirm: "RegistryPass9!",
      email: "registry@example.test",
    };
    expect((await signupApi.signup(request)).data).toEqual(data);
    expect(fetch).toHaveBeenCalledWith(
      "/api/v1/auth/signup",
      expect.objectContaining({
        method: "POST",
        credentials: "include",
        body: JSON.stringify(request),
      }),
    );
  });

  it("encodes the entered ID rather than interpolating raw query delimiters", async () => {
    const fetch = vi.fn().mockResolvedValue(
      response({
        success: true,
        data: { available: false },
        meta: {},
      }),
    );
    vi.stubGlobal("fetch", fetch);
    expect((await checkUserIdAvailability("selected&id")).data?.available).toBe(
      false,
    );
    expect(fetch).toHaveBeenCalledWith(
      "/api/v1/auth/check-userid?userId=selected%26id",
      expect.any(Object),
    );
  });

  it("preserves 409 field errors for the signup form", async () => {
    const error = {
      code: "CONFLICT",
      message: "이미 등록된 이메일입니다.",
      fields: [{ field: "email", message: "이미 등록된 이메일입니다." }],
    };
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValue(response({ success: false, error, meta: {} }, 409)),
    );
    await expect(
      signupApi.signup({
        userId: "registryuser",
        password: "RegistryPass9!",
        passwordConfirm: "RegistryPass9!",
        email: "registry@example.test",
      }),
    ).rejects.toMatchObject({
      status: 409,
      apiError: error,
    });
  });

  it("preserves the existing login endpoint and unauthorized error type", async () => {
    const fetch = vi.fn().mockResolvedValue(
      response(
        {
          success: false,
          error: { code: "UNAUTHORIZED", message: "인증 실패", fields: [] },
          meta: {},
        },
        401,
      ),
    );
    vi.stubGlobal("fetch", fetch);
    await expect(
      authApi.login("existinguser", "existing-password"),
    ).rejects.toBeInstanceOf(ApiClientError);
    expect(fetch).toHaveBeenCalledWith(
      "/api/auth/login",
      expect.objectContaining({
        method: "POST",
        credentials: "include",
        body: JSON.stringify({
          loginId: "existinguser",
          password: "existing-password",
        }),
      }),
    );
  });
});
