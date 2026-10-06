// @vitest-environment jsdom
import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiClientError } from "../../api/apiClient";
import { SignupPage, meetsPasswordRule } from "./SCR-SIGNUP";
import { signupApi } from "./signupApi";

function fill() {
  fireEvent.change(screen.getByLabelText("아이디"), {
    target: { value: "newteacher" },
  });
  const password = `Aa1!${crypto.randomUUID()}`;
  fireEvent.change(screen.getByLabelText("비밀번호"), {
    target: { value: password },
  });
  fireEvent.change(screen.getByLabelText("비밀번호 확인"), {
    target: { value: password },
  });
  fireEvent.change(screen.getByLabelText("이메일"), {
    target: { value: "Teacher@Example.Invalid" },
  });
  return password;
}

afterEach(() => {
  cleanup();
  vi.restoreAllMocks();
});

describe("SCR-SIGNUP", () => {
  it("renders the four empty fields and a real return path, with no sample credentials", () => {
    render(<SignupPage onSignupSuccess={vi.fn()} />);
    expect(screen.getByTestId("signup-page")).toBeTruthy();
    expect(
      screen
        .getByRole("link", { name: "로그인으로 돌아가기" })
        .getAttribute("href"),
    ).toBe("/login");
    for (const label of ["아이디", "비밀번호", "비밀번호 확인", "이메일"]) {
      expect((screen.getByLabelText(label) as HTMLInputElement).value).toBe("");
    }
    expect(screen.getByLabelText("비밀번호").getAttribute("type")).toBe(
      "password",
    );
  });

  it("validates missing input without calling the API", () => {
    const signup = vi.spyOn(signupApi, "signup");
    render(<SignupPage />);
    fireEvent.click(screen.getByRole("button", { name: "가입하기" }));
    expect(screen.getAllByText(/필수값을 입력해 주세요/)).toHaveLength(4);
    expect(signup).not.toHaveBeenCalled();
  });

  it("submits the entered values, locks CTA while pending, clears credentials and navigates with receipt", async () => {
    let resolve!: (value: Awaited<ReturnType<typeof signupApi.signup>>) => void;
    const signup = vi.spyOn(signupApi, "signup").mockImplementation(
      () =>
        new Promise((done) => {
          resolve = done;
        }),
    );
    const navigate = vi.fn();
    render(<SignupPage onSignupSuccess={navigate} />);
    const password = fill();
    fireEvent.click(screen.getByRole("button", { name: "가입하기" }));
    expect(
      (
        screen.getByRole("button", {
          name: "가입 처리 중...",
        }) as HTMLButtonElement
      ).disabled,
    ).toBe(true);
    expect(signup).toHaveBeenCalledWith({
      userId: "newteacher",
      password,
      passwordConfirm: password,
      email: "Teacher@Example.Invalid",
    });
    await act(async () =>
      resolve({
        success: true,
        data: { userId: "newteacher", message: "가입이 완료되었습니다." },
        meta: {},
      }),
    );
    expect(screen.getByRole("status").textContent).toBe(
      "가입이 완료되었습니다.",
    );
    expect(navigate).toHaveBeenCalledWith("가입이 완료되었습니다.");
    expect((screen.getByLabelText("비밀번호") as HTMLInputElement).value).toBe(
      "",
    );
    expect(
      (screen.getByLabelText("비밀번호 확인") as HTMLInputElement).value,
    ).toBe("");
  });

  it("renders a field-aware server conflict and allows retry", async () => {
    vi.spyOn(signupApi, "signup").mockRejectedValue(
      new ApiClientError(409, "이미 등록된 이메일입니다.", {
        code: "CONFLICT",
        message: "이미 등록된 이메일입니다.",
        fields: [{ field: "email", message: "이미 등록된 이메일입니다." }],
      }),
    );
    const navigate = vi.fn();
    render(<SignupPage onSignupSuccess={navigate} />);
    fill();
    fireEvent.click(screen.getByRole("button", { name: "가입하기" }));
    await waitFor(() =>
      expect(screen.getByLabelText("이메일").getAttribute("aria-invalid")).toBe(
        "true",
      ),
    );
    expect(screen.getByRole("alert").textContent).toBe(
      "이미 등록된 이메일입니다.",
    );
    expect(
      (screen.getByRole("button", { name: "가입하기" }) as HTMLButtonElement)
        .disabled,
    ).toBe(false);
    expect(navigate).not.toHaveBeenCalled();
  });

  it("ignores a stale focus-out response after the user changes the id", async () => {
    let resolve!: (
      value: Awaited<ReturnType<typeof signupApi.checkUserIdAvailability>>,
    ) => void;
    vi.spyOn(signupApi, "checkUserIdAvailability").mockImplementation(
      () =>
        new Promise((done) => {
          resolve = done;
        }),
    );
    render(<SignupPage />);
    const input = screen.getByLabelText("아이디");
    fireEvent.change(input, { target: { value: "firstteacher" } });
    fireEvent.blur(input);
    expect(screen.getByText("확인 중...")).toBeTruthy();
    fireEvent.change(input, { target: { value: "secondteacher" } });
    await act(async () =>
      resolve({ success: true, data: { available: false }, meta: {} }),
    );
    expect(screen.queryByText("이미 사용 중")).toBeNull();
    expect(screen.queryByText("확인 중...")).toBeNull();
  });

  it("shows availability results only from the API and clears them on edit", async () => {
    const check = vi
      .spyOn(signupApi, "checkUserIdAvailability")
      .mockResolvedValueOnce({
        success: true,
        data: { available: true },
        meta: {},
      })
      .mockResolvedValueOnce({
        success: true,
        data: { available: false },
        meta: {},
      });
    render(<SignupPage />);
    const input = screen.getByLabelText("아이디");
    fireEvent.change(input, { target: { value: "freeuser" } });
    fireEvent.blur(input);
    expect(await screen.findByText("사용 가능")).toBeTruthy();
    expect(check).toHaveBeenCalledWith("freeuser");
    fireEvent.change(input, { target: { value: "takenuser" } });
    expect(screen.queryByText("사용 가능")).toBeNull();
    fireEvent.blur(input);
    expect(await screen.findByText("이미 사용 중")).toBeTruthy();
  });

  it("handles network failure without echoing the thrown diagnostics", async () => {
    vi.spyOn(signupApi, "signup").mockRejectedValue(
      new Error("internal diagnostic"),
    );
    render(<SignupPage />);
    fill();
    fireEvent.click(screen.getByRole("button", { name: "가입하기" }));
    expect((await screen.findByRole("alert")).textContent).toBe(
      "가입 처리에 실패했습니다. 잠시 후 다시 시도해 주세요.",
    );
    expect(screen.queryByText("internal diagnostic")).toBeNull();
  });

  it("uses four ASCII categories and does not count whitespace as special characters", () => {
    expect(meetsPasswordRule("Abcdef12", "teacher")).toBe(true);
    expect(meetsPasswordRule("abcdef1!", "teacher")).toBe(true);
    expect(meetsPasswordRule("abcdef12 ", "teacher")).toBe(false);
    expect(meetsPasswordRule("abcdef12한", "teacher")).toBe(false);
    expect(meetsPasswordRule("teacher1", "teacher1")).toBe(false);
  });
});
