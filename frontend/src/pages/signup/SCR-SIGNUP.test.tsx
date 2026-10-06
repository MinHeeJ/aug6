import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { SignupPage, passwordMeetsRules } from "./SCR-SIGNUP";

function response(data: unknown, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function fill() {
  fireEvent.change(screen.getByLabelText("아이디"), {
    target: { value: "signupuser" },
  });
  fireEvent.change(screen.getByLabelText("비밀번호", { exact: true }), {
    target: { value: "TestPass9!" },
  });
  fireEvent.change(screen.getByLabelText("비밀번호 확인"), {
    target: { value: "TestPass9!" },
  });
  fireEvent.change(screen.getByLabelText("이메일"), {
    target: { value: "signup@example.test" },
  });
}

describe("SCR-SIGNUP", () => {
  beforeEach(() => {
    window.history.replaceState({}, "", "/signup");
  });

  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it("validates empty input locally without sending requests", () => {
    const fetch = vi.fn();
    vi.stubGlobal("fetch", fetch);
    render(<SignupPage />);
    fireEvent.click(screen.getByRole("button", { name: "가입하기" }));
    expect(screen.getByLabelText("아이디")).toHaveAttribute(
      "aria-invalid",
      "true",
    );
    expect(fetch).not.toHaveBeenCalled();
    expect(
      screen.getByRole("link", { name: "로그인으로 돌아가기" }),
    ).toHaveAttribute("href", "/login");
  });

  it("shows live password and confirmation hints without printing entered secrets", () => {
    render(<SignupPage />);
    fill();
    expect(screen.getByText("비밀번호 규칙을 충족합니다.")).toBeVisible();
    expect(screen.getByText("비밀번호가 일치합니다.")).toBeVisible();
    fireEvent.change(screen.getByLabelText("비밀번호 확인"), {
      target: { value: "different" },
    });
    expect(screen.getByText("비밀번호가 일치하지 않습니다.")).toBeVisible();
    expect(screen.getByLabelText("비밀번호", { exact: true })).toHaveAttribute(
      "type",
      "password",
    );
    expect(screen.queryByText("TestPass9!")).not.toBeInTheDocument();
    expect(passwordMeetsRules("abcdefgh1", "signupuser")).toBe(false);
    expect(passwordMeetsRules("Abcdefg1", "signupuser")).toBe(true);
  });

  it("submits actual form data once, disables pending CTA, acknowledges and navigates without auto-login", async () => {
    let resolve!: (value: Response) => void;
    const fetch = vi.fn(
      (_path: RequestInfo | URL, _init?: RequestInit) =>
        new Promise<Response>((done) => {
          resolve = done;
        }),
    );
    vi.stubGlobal("fetch", fetch);
    const alert = vi.spyOn(window, "alert").mockImplementation(() => undefined);
    render(<SignupPage />);
    fill();
    fireEvent.click(screen.getByRole("button", { name: "가입하기" }));
    expect(screen.getByRole("button", { name: "처리 중" })).toBeDisabled();
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch.mock.calls[0][0]).toBe("/api/v1/auth/signup");
    const init = fetch.mock.calls[0][1] as RequestInit;
    expect(JSON.parse(init.body as string)).toEqual({
      userId: "signupuser",
      password: "TestPass9!",
      passwordConfirm: "TestPass9!",
      email: "signup@example.test",
    });
    await act(async () => {
      resolve(
        response(
          {
            success: true,
            data: { userId: "signupuser", message: "가입이 완료되었습니다." },
            meta: {},
          },
          201,
        ),
      );
    });
    expect(alert).toHaveBeenCalledWith("가입이 완료되었습니다.");
    expect(window.location.pathname).toBe("/login");
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(screen.getByLabelText("비밀번호", { exact: true })).toHaveValue("");
  });

  it.each([
    [400, "passwordConfirm", "비밀번호와 비밀번호 확인이 일치하지 않습니다."],
    [409, "email", "이미 등록된 이메일입니다."],
  ])(
    "renders %i field error and stays on signup",
    async (status, field, message) => {
      vi.stubGlobal(
        "fetch",
        vi.fn().mockResolvedValue(
          response(
            {
              success: false,
              error: {
                code: status === 409 ? "CONFLICT" : "VALIDATION_ERROR",
                message,
                fields: [{ field, message }],
              },
              meta: {},
            },
            status,
          ),
        ),
      );
      render(<SignupPage />);
      fill();
      fireEvent.click(screen.getByRole("button", { name: "가입하기" }));
      await waitFor(() =>
        expect(screen.getByRole("status")).toHaveTextContent(message),
      );
      const label = field === "email" ? "이메일" : "비밀번호 확인";
      expect(screen.getByLabelText(label)).toHaveAttribute(
        "aria-invalid",
        "true",
      );
      expect(window.location.pathname).toBe("/signup");
    },
  );

  it("uses API availability on blur and clears obsolete results on editing", async () => {
    const fetch = vi
      .fn()
      .mockResolvedValue(
        response({ success: true, data: { available: false }, meta: {} }),
      );
    vi.stubGlobal("fetch", fetch);
    render(<SignupPage />);
    const id = screen.getByLabelText("아이디");
    fireEvent.change(id, { target: { value: "signupuser" } });
    fireEvent.blur(id);
    await screen.findByText("이미 사용 중인 아이디입니다.");
    expect(fetch.mock.calls[0][0]).toBe(
      "/api/v1/auth/check-userid?userId=signupuser",
    );
    fireEvent.change(id, { target: { value: "anotheruser" } });
    expect(
      screen.queryByText("이미 사용 중인 아이디입니다."),
    ).not.toBeInTheDocument();
  });

  it("ignores stale availability responses for a previous ID", async () => {
    let resolve!: (value: Response) => void;
    vi.stubGlobal(
      "fetch",
      vi.fn(
        () =>
          new Promise<Response>((done) => {
            resolve = done;
          }),
      ),
    );
    render(<SignupPage />);
    const id = screen.getByLabelText("아이디");
    fireEvent.change(id, { target: { value: "signupuser" } });
    fireEvent.blur(id);
    fireEvent.change(id, { target: { value: "anotheruser" } });
    await act(async () =>
      resolve(
        response({ success: true, data: { available: false }, meta: {} }),
      ),
    );
    expect(
      screen.queryByText("이미 사용 중인 아이디입니다."),
    ).not.toBeInTheDocument();
  });

  it("shows retriable network errors and navigates back using the public link", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("offline")));
    render(<SignupPage />);
    fill();
    fireEvent.click(screen.getByRole("button", { name: "가입하기" }));
    await screen.findByText(
      "회원가입에 실패했습니다. 잠시 후 다시 시도해 주세요.",
    );
    expect(screen.getByRole("button", { name: "가입하기" })).toBeEnabled();
    fireEvent.click(screen.getByRole("link", { name: "로그인으로 돌아가기" }));
    expect(window.location.pathname).toBe("/login");
  });
});
