// @vitest-environment jsdom
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiClientError, signupApi } from "../api/apiClient";
import { signupApi as featureSignupApi } from "../pages/signupimplementation/signupApi";
import { useAuth } from "./AuthProvider";
import { AppRouter } from "./router";

vi.mock("./AuthProvider", () => ({ useAuth: vi.fn() }));

const login = vi.fn(async () => {});

function setAuth(status: ReturnType<typeof useAuth>["status"] = "anonymous") {
  vi.mocked(useAuth).mockReturnValue({
    status,
    user:
      status === "authenticated"
        ? {
            userId: 42,
            loginId: "teacher",
            name: "교원",
            roles: [],
            menus: [],
          }
        : null,
    error: status === "error" ? "세션 조회 실패" : null,
    login,
    logout: vi.fn(async () => {}),
    refresh: vi.fn(async () => {}),
  });
}

function mockResponse(status: number, body: unknown) {
  const fetch = vi.fn().mockResolvedValue(
    new Response(JSON.stringify(body), {
      status,
      headers: { "Content-Type": "application/json" },
    }),
  );
  vi.stubGlobal("fetch", fetch);
  return fetch;
}

beforeEach(() => {
  login.mockClear();
  setAuth();
  window.history.replaceState({}, "", "/signup");
});

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
  window.history.replaceState({}, "", "/login");
});

describe("public signup route wiring", () => {
  it.each(["anonymous", "loading", "error", "authenticated"] as const)(
    "renders the real signup screen with authentication status %s",
    (status) => {
      setAuth(status);
      render(<AppRouter />);
      expect(screen.getByTestId("signup-page")).toBeTruthy();
      for (const label of ["아이디", "비밀번호", "비밀번호 확인", "이메일"]) {
        expect(screen.getByLabelText(label)).toBeTruthy();
      }
      expect(screen.queryByRole("button", { name: "로그인" })).toBeNull();
      expect(
        screen
          .getByRole("link", { name: "로그인으로 돌아가기" })
          .getAttribute("href"),
      ).toBe("/login");
    },
  );

  it("keeps protected routes behind the existing anonymous login fallback", () => {
    window.history.replaceState({}, "", "/admin/users");
    render(<AppRouter />);
    expect(screen.getByRole("button", { name: "로그인" })).toBeTruthy();
    expect(
      screen
        .getByRole("link", { name: "회원가입", exact: true })
        .getAttribute("href"),
    ).toBe("/signup");
    expect(screen.queryByTestId("signup-page")).toBeNull();
  });

  it("does not broaden the public path to unrelated auth routes", () => {
    window.history.replaceState({}, "", "/signup/unrelated");
    render(<AppRouter />);
    expect(screen.getByRole("button", { name: "로그인" })).toBeTruthy();
    expect(screen.queryByTestId("signup-page")).toBeNull();
  });

  it("retains the existing menu guard for an authenticated user without access", () => {
    setAuth("authenticated");
    window.history.replaceState({}, "", "/admin/users");
    render(<AppRouter />);
    expect(
      screen.getByText("사용자 관리 화면 접근 권한이 없습니다."),
    ).toBeTruthy();
    expect(screen.queryByTestId("signup-page")).toBeNull();
  });

  it("uses the registered API and returns to login without automatically logging in", async () => {
    const fetch = mockResponse(201, {
      success: true,
      data: { userId: "newteacher", message: "가입이 완료되었습니다." },
      meta: {},
    });
    render(<AppRouter />);
    const password = `Aa1!${crypto.randomUUID()}`;
    const values = {
      아이디: "newteacher",
      비밀번호: password,
      "비밀번호 확인": password,
      이메일: "Teacher@Example.Invalid",
    };
    for (const [label, value] of Object.entries(values)) {
      fireEvent.change(screen.getByLabelText(label), { target: { value } });
    }
    fireEvent.click(screen.getByRole("button", { name: "가입하기" }));
    await waitFor(() => expect(window.location.pathname).toBe("/login"));
    expect(await screen.findByRole("button", { name: "로그인" })).toBeTruthy();
    expect(screen.getByRole("status").textContent).toBe(
      "가입이 완료되었습니다.",
    );
    expect(window.history.state.signupMessage).toBe("가입이 완료되었습니다.");
    expect(login).not.toHaveBeenCalled();
    expect(fetch).toHaveBeenCalledTimes(1);
    const [path, init] = fetch.mock.calls[0];
    expect(path).toBe("/api/v1/auth/signup");
    expect(init.method).toBe("POST");
    expect(JSON.parse(init.body)).toEqual({
      userId: "newteacher",
      password,
      passwordConfirm: password,
      email: "Teacher@Example.Invalid",
    });
  });
});

describe("shared signup API registration", () => {
  it("reuses the feature client rather than maintaining a second implementation", () => {
    expect(signupApi).toBe(featureSignupApi);
  });

  it("encodes the supplied userId and preserves the availability response envelope", async () => {
    const body = { success: true, data: { available: false }, meta: {} };
    const fetch = mockResponse(200, body);
    const userId = "teacher&next=value";
    expect(await signupApi.checkUserIdAvailability(userId)).toEqual(body);
    const [path, init] = fetch.mock.calls[0];
    expect(path).toBe(
      `/api/v1/auth/check-userid?${new URLSearchParams({ userId })}`,
    );
    expect(init.body).toBeUndefined();
  });

  it.each([400, 409])(
    "preserves status %s and field errors for the signup screen",
    async (status) => {
      const error = {
        code: status === 409 ? "CONFLICT" : "VALIDATION_ERROR",
        message: "이메일을 확인하세요.",
        fields: [{ field: "email", message: "이메일을 확인하세요." }],
      };
      mockResponse(status, { success: false, error, meta: {} });
      const request = {
        userId: "newteacher",
        password: `Aa1!${crypto.randomUUID()}`,
        passwordConfirm: "",
        email: "Teacher@Example.Invalid",
      };
      request.passwordConfirm = request.password;
      const result = signupApi.signup(request);
      await expect(result).rejects.toBeInstanceOf(ApiClientError);
      await expect(result).rejects.toMatchObject({ status, apiError: error });
    },
  );
});
