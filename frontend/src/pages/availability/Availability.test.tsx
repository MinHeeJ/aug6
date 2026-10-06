import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { SignupPage } from "../signup/SCR-SIGNUP";

function response(data: unknown, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function blurId(value: string) {
  const input = screen.getByLabelText("아이디", { exact: true });
  fireEvent.change(input, { target: { value } });
  fireEvent.blur(input);
  return input;
}

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe("signup focus-out availability", () => {
  it.each([true, false])(
    "renders only the API boolean result (%s) for the entered ID",
    async (available) => {
      const fetch = vi
        .fn()
        .mockResolvedValue(
          response({ success: true, data: { available }, meta: {} }),
        );
      vi.stubGlobal("fetch", fetch);
      render(<SignupPage />);
      blurId("entereduser9");
      await screen.findByText(
        available
          ? "사용 가능한 아이디입니다."
          : "이미 사용 중인 아이디입니다.",
      );
      expect(fetch).toHaveBeenCalledTimes(1);
      expect(fetch.mock.calls[0][0]).toBe(
        "/api/v1/auth/check-userid?userId=entereduser9",
      );
      expect(screen.getByRole("button", { name: "가입하기" })).toBeEnabled();
    },
  );

  it.each(["", "abc", "1abc", "Abcd", "ab-c", "abcdefghijklmnopqrstu"])(
    "shows a local field error for invalid ID %s without querying",
    (id) => {
      const fetch = vi.fn();
      vi.stubGlobal("fetch", fetch);
      render(<SignupPage />);
      const input = blurId(id);
      expect(input).toHaveAttribute("aria-invalid", "true");
      expect(
        screen.getByText(
          "아이디는 영문 소문자로 시작하는 영문 소문자와 숫자 4~20자여야 합니다.",
        ),
      ).toBeVisible();
      expect(fetch).not.toHaveBeenCalled();
    },
  );

  it("replaces a server field error on successful retry of the same ID", async () => {
    const message = "아이디 형식을 확인해 주세요.";
    const fetch = vi
      .fn()
      .mockResolvedValueOnce(
        response(
          {
            success: false,
            error: {
              code: "VALIDATION_ERROR",
              message,
              fields: [{ field: "userId", message }],
            },
            meta: {},
          },
          400,
        ),
      )
      .mockResolvedValueOnce(
        response({ success: true, data: { available: true }, meta: {} }),
      );
    vi.stubGlobal("fetch", fetch);
    render(<SignupPage />);
    const input = blurId("entereduser9");
    await waitFor(() =>
      expect(screen.getByRole("status")).toHaveTextContent(message),
    );
    expect(input).toHaveAttribute("aria-invalid", "true");
    fireEvent.blur(input);
    await screen.findByText("사용 가능한 아이디입니다.");
    expect(input).toHaveAttribute("aria-invalid", "false");
    expect(screen.getByRole("status")).toBeEmptyDOMElement();
    expect(screen.queryByText(message)).not.toBeInTheDocument();
    expect(fetch).toHaveBeenCalledTimes(2);
  });

  it.each([undefined, {}, { available: "false" }, { available: null }])(
    "treats malformed success data as a failed check instead of a taken ID",
    async (data) => {
      vi.stubGlobal(
        "fetch",
        vi.fn().mockResolvedValue(response({ success: true, data, meta: {} })),
      );
      render(<SignupPage />);
      blurId("entereduser9");
      await screen.findByText(
        "아이디 확인에 실패했습니다. 다시 시도해 주세요.",
      );
      expect(
        screen.queryByText("이미 사용 중인 아이디입니다."),
      ).not.toBeInTheDocument();
      expect(
        screen.queryByText("사용 가능한 아이디입니다."),
      ).not.toBeInTheDocument();
    },
  );

  it("shows loading, retries network failures and clears the previous failure on success", async () => {
    let finish!: (value: Response) => void;
    const fetch = vi
      .fn()
      .mockRejectedValueOnce(new TypeError("offline"))
      .mockImplementationOnce(
        () =>
          new Promise<Response>((resolve) => {
            finish = resolve;
          }),
      );
    vi.stubGlobal("fetch", fetch);
    render(<SignupPage />);
    const input = blurId("entereduser9");
    await screen.findByText("아이디 확인에 실패했습니다. 다시 시도해 주세요.");
    fireEvent.blur(input);
    expect(screen.getByText("아이디 확인 중입니다.")).toBeVisible();
    expect(
      screen.queryByText("아이디 확인에 실패했습니다. 다시 시도해 주세요."),
    ).not.toBeInTheDocument();
    await act(async () =>
      finish(response({ success: true, data: { available: false }, meta: {} })),
    );
    expect(screen.getByText("이미 사용 중인 아이디입니다.")).toBeVisible();
    expect(screen.getByRole("status")).toBeEmptyDOMElement();
  });

  it("ignores out-of-order checks after editing and only renders the latest ID result", async () => {
    const pending: Array<(value: Response) => void> = [];
    vi.stubGlobal(
      "fetch",
      vi.fn(() => new Promise<Response>((resolve) => pending.push(resolve))),
    );
    render(<SignupPage />);
    blurId("firstuser9");
    const input = blurId("seconduser9");
    await act(async () =>
      pending[1](
        response({ success: true, data: { available: true }, meta: {} }),
      ),
    );
    await act(async () =>
      pending[0](
        response({ success: true, data: { available: false }, meta: {} }),
      ),
    );
    expect(input).toHaveValue("seconduser9");
    expect(screen.getByText("사용 가능한 아이디입니다.")).toBeVisible();
    expect(
      screen.queryByText("이미 사용 중인 아이디입니다."),
    ).not.toBeInTheDocument();
  });
});
