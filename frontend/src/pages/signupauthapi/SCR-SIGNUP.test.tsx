import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { SignupPage } from "./SCR-SIGNUP";

describe("SCR-SIGNUP", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("checks user ID availability when the user leaves the field", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse({
        success: true,
        data: { available: true },
        meta: {},
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    render(<SignupPage />);
    fireEvent.change(screen.getByLabelText("아이디"), {
      target: { value: "newuser1" },
    });
    fireEvent.blur(screen.getByLabelText("아이디"));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/v1/auth/check-userid?userId=newuser1",
        expect.objectContaining({ credentials: "include" }),
      );
    });
    expect(await screen.findByText("사용 가능한 아이디입니다.")).toBeTruthy();
  });

  it("submits the four required values and navigates to login after success", async () => {
    const onNavigateToLogin = vi.fn();
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse({
        success: true,
        data: { userId: "newuser1", message: "가입이 완료되었습니다." },
        meta: {},
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    render(<SignupPage onNavigateToLogin={onNavigateToLogin} />);
    fillValidSignupForm();
    fireEvent.click(screen.getByTestId("signup-submit-button"));

    await waitFor(() => {
      expect(fetchMock).toHaveBeenCalledWith(
        "/api/v1/auth/signup",
        expect.objectContaining({
          body: JSON.stringify({
            userId: "newuser1",
            password: "Abcdef1!",
            passwordConfirm: "Abcdef1!",
            email: "newuser1@example.com",
          }),
          method: "POST",
        }),
      );
    });
    expect(
      await screen.findByTestId("signup-success-message"),
    ).toHaveTextContent("가입이 완료되었습니다.");
    expect(onNavigateToLogin).toHaveBeenCalledOnce();
  });

  it("renders the server passwordConfirm field error without exposing password values", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse(
        {
          success: false,
          error: {
            code: "VALIDATION_ERROR",
            message: "입력값이 올바르지 않습니다.",
            fields: [
              {
                field: "passwordConfirm",
                message: "비밀번호와 비밀번호 확인이 일치하지 않습니다.",
              },
            ],
          },
          meta: {},
        },
        400,
      ),
    );
    vi.stubGlobal("fetch", fetchMock);

    render(<SignupPage />);
    fillValidSignupForm();
    fireEvent.click(screen.getByTestId("signup-submit-button"));

    expect(
      await screen.findByText("비밀번호와 비밀번호 확인이 일치하지 않습니다."),
    ).toBeTruthy();
    expect(screen.queryByText("Abcdef1!")).toBeNull();
  });
});

function fillValidSignupForm() {
  fireEvent.change(screen.getByLabelText("아이디"), {
    target: { value: "newuser1" },
  });
  fireEvent.change(screen.getByLabelText("비밀번호"), {
    target: { value: "Abcdef1!" },
  });
  fireEvent.change(screen.getByLabelText("비밀번호 확인"), {
    target: { value: "Abcdef1!" },
  });
  fireEvent.change(screen.getByLabelText("이메일"), {
    target: { value: "NewUser1@Example.com" },
  });
}

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    headers: { "Content-Type": "application/json" },
    status,
  });
}
