import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AuthProvider } from "./AuthProvider";
import { AppRouter } from "./router";

function jsonResponse(data: unknown, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function anonymousResponse() {
  return jsonResponse(
    {
      success: false,
      error: {
        code: "UNAUTHORIZED",
        message: "로그인이 필요합니다.",
        fields: [],
      },
      meta: {},
    },
    401,
  );
}

function renderRouter(path: string) {
  window.history.replaceState({}, "", path);
  return render(
    <AuthProvider>
      <AppRouter />
    </AuthProvider>,
  );
}

function navigate(path: string) {
  act(() => {
    window.history.pushState({}, "", path);
    window.dispatchEvent(new PopStateEvent("popstate"));
  });
}

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
  window.history.replaceState({}, "", "/login");
});

describe("public signup route wiring", () => {
  it("opens signup from the existing login entry without submitting login", async () => {
    const fetch = vi.fn().mockResolvedValue(anonymousResponse());
    vi.stubGlobal("fetch", fetch);
    renderRouter("/login");
    const link = await screen.findByRole("link", {
      name: "회원가입",
      exact: true,
    });
    expect(link).toHaveAttribute("href", "/signup");
    fireEvent.click(link);
    expect(window.location.pathname).toBe("/signup");
    expect(await screen.findByTestId("signup-page")).toBeVisible();
    expect(fetch.mock.calls.map(([path]) => path)).toEqual(["/api/auth/me"]);
  });

  it("renders the real signup page directly and returns to the existing login page", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(anonymousResponse()));
    renderRouter("/signup");
    expect(await screen.findByTestId("signup-page")).toBeVisible();
    fireEvent.click(screen.getByRole("link", { name: "로그인으로 돌아가기" }));
    expect(window.location.pathname).toBe("/login");
    expect(
      await screen.findByRole("button", { name: "로그인", exact: true }),
    ).toBeVisible();
    expect(screen.queryByTestId("signup-page")).not.toBeInTheDocument();
    navigate("/signup");
    expect(await screen.findByTestId("signup-page")).toBeVisible();
  });

  it("connects availability and signup to the real form, then renders login without auto-login", async () => {
    const fetch = vi.fn(async (path: RequestInfo | URL) => {
      if (path === "/api/auth/me") return anonymousResponse();
      if (String(path).startsWith("/api/v1/auth/check-userid?")) {
        return jsonResponse({
          success: true,
          data: { available: true },
          meta: {},
        });
      }
      if (path === "/api/v1/auth/signup") {
        return jsonResponse(
          {
            success: true,
            data: { userId: "routeuser", message: "가입이 완료되었습니다." },
            meta: {},
          },
          201,
        );
      }
      throw new Error(`Unexpected request: ${path}`);
    });
    vi.stubGlobal("fetch", fetch);
    const alert = vi.spyOn(window, "alert").mockImplementation(() => undefined);
    renderRouter("/signup");
    await screen.findByTestId("signup-page");
    fireEvent.change(screen.getByLabelText("아이디"), {
      target: { value: "routeuser" },
    });
    fireEvent.blur(screen.getByLabelText("아이디"));
    expect(await screen.findByText("사용 가능한 아이디입니다.")).toBeVisible();
    fireEvent.change(screen.getByLabelText("비밀번호", { exact: true }), {
      target: { value: "RoutePass9!" },
    });
    fireEvent.change(screen.getByLabelText("비밀번호 확인"), {
      target: { value: "RoutePass9!" },
    });
    fireEvent.change(screen.getByLabelText("이메일"), {
      target: { value: "route@example.test" },
    });
    fireEvent.click(screen.getByRole("button", { name: "가입하기" }));
    expect(
      await screen.findByRole("button", { name: "로그인", exact: true }),
    ).toBeVisible();
    expect(alert).toHaveBeenCalledWith("가입이 완료되었습니다.");
    expect(window.location.pathname).toBe("/login");
    expect(fetch.mock.calls.map(([path]) => path)).toEqual([
      "/api/auth/me",
      "/api/v1/auth/check-userid?userId=routeuser",
      "/api/v1/auth/signup",
    ]);
  });

  it("keeps unrelated protected routes behind login for anonymous users", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(anonymousResponse()));
    renderRouter("/admin/users");
    expect(
      await screen.findByRole("button", { name: "로그인", exact: true }),
    ).toBeVisible();
    expect(screen.queryByTestId("signup-page")).not.toBeInTheDocument();
    navigate("/signup-extra");
    await waitFor(() => {
      expect(
        screen.getByRole("button", { name: "로그인", exact: true }),
      ).toBeVisible();
      expect(screen.queryByTestId("signup-page")).not.toBeInTheDocument();
    });
  });
});
