import { describe, expect, it, vi } from "vitest";
import { renderToStaticMarkup } from "react-dom/server";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { SignupPage, validateSignupInput } from "./SignupPage";

describe("SignupPage", () => {
  it("renders SCR-SIGNUP fields, validation hints, submit CTA, and login navigation", () => {
    const html = renderToStaticMarkup(<SignupPage />);

    expect(html).toContain("SCR-SIGNUP");
    expect(html).toContain("회원가입");
    expect(html).toContain('name="loginId"');
    expect(html).toContain('name="password"');
    expect(html).toContain('name="passwordConfirm"');
    expect(html).toContain('name="email"');
    expect(html).toContain("영문 소문자 시작, 4~20자");
    expect(html).toContain("8자 이상");
    expect(html).toContain("가입하기");
    expect(html).toContain("/login");
  });

  it("validates required fields, password mismatch, weak password, and email format", () => {
    expect(
      validateSignupInput({
        loginId: "",
        password: "",
        passwordConfirm: "",
        email: "",
      }),
    ).toEqual({
      loginId: "아이디는 필수입니다.",
      email: "이메일은 필수입니다.",
      password: "비밀번호는 필수입니다.",
      passwordConfirm: "비밀번호 확인은 필수입니다.",
    });

    expect(
      validateSignupInput({
        loginId: "signupuser01",
        password: "weakpass",
        passwordConfirm: "different",
        email: "not-an-email",
      }),
    ).toMatchObject({
      password: expect.stringContaining("8자 이상"),
      passwordConfirm: "비밀번호와 비밀번호 확인이 일치하지 않습니다.",
      email: "올바른 이메일 형식이 아닙니다.",
    });
  });

  it("accepts valid signup input without hardcoded sample ids", () => {
    expect(
      validateSignupInput({
        loginId: "newmember99",
        password: "Strong!123",
        passwordConfirm: "Strong!123",
        email: "newmember99@example.test",
      }),
    ).toEqual({});

    const html = renderToStaticMarkup(<SignupPage />);
    expect(html).not.toContain("signup_user01");
    expect(html).not.toContain("USER-1");
  });

  it("wires signup success resend CTA to the submitted email", async () => {
    const onSignup = vi.fn().mockResolvedValue(undefined);
    const onResend = vi
      .fn()
      .mockResolvedValue("인증 메일 재발송 요청이 접수되었습니다.");

    render(<SignupPage onSignup={onSignup} onResend={onResend} />);
    fireEvent.change(screen.getByTestId("signup-login-id-input"), {
      target: { value: "newmember99" },
    });
    fireEvent.change(screen.getByTestId("signup-password-input"), {
      target: { value: "Strong!123" },
    });
    fireEvent.change(screen.getByTestId("signup-password-confirm-input"), {
      target: { value: "Strong!123" },
    });
    fireEvent.change(screen.getByTestId("signup-email-input"), {
      target: { value: "newmember99@example.test" },
    });
    fireEvent.submit(
      screen
        .getByTestId("signup-submit-button")
        .closest("form") as HTMLFormElement,
    );

    await screen.findByTestId("signup-success-container");
    fireEvent.click(screen.getByTestId("signup-resend-button"));

    await waitFor(() =>
      expect(onResend).toHaveBeenCalledWith("newmember99@example.test"),
    );
    expect(
      await screen.findByText("인증 메일 재발송 요청이 접수되었습니다."),
    ).toBeInTheDocument();
  });
});
