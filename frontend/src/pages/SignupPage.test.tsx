import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { renderToStaticMarkup } from "react-dom/server";
import { SignupPage, validateSignupInput } from "./SignupPage";

const validForm = {
  loginId: "newuser",
  password: "Password1",
  email: "user@example.com",
};

describe("SignupPage", () => {
  it("validates login ID, password, and email without exposing SMTP credential fields", () => {
    expect(
      validateSignupInput({ loginId: "", password: "short", email: "bad" }),
    ).toEqual({
      loginId: "로그인 ID를 입력하세요.",
      password: "비밀번호는 8자 이상이며 영문과 숫자를 포함해야 합니다.",
      email: "올바른 이메일 주소를 입력하세요.",
    });

    const html = renderToStaticMarkup(
      <SignupPage
        onSignup={async () => ({
          accountStatus: "PENDING_EMAIL",
          emailVerifiedYn: "N",
        })}
      />,
    );
    expect(html).toContain("SCR-SIGNUP");
    expect(html).toContain("회원가입");
    expect(html).toContain("로그인 ID");
    expect(html).toContain("비밀번호");
    expect(html).toContain("이메일");
    expect(html).not.toContain("smtp-username");
    expect(html).not.toContain("smtp-password");
    expect(html).not.toContain("MAIL_PASSWORD");
    expect(html).not.toContain("Gmail 앱 비밀번호");
  });

  it("submits createSignup and shows pending verification success state", async () => {
    const onSignup = vi.fn().mockResolvedValue({
      userId: 7,
      loginId: "newuser",
      email: "user@example.com",
      accountStatus: "PENDING_EMAIL",
      emailVerifiedYn: "N",
      mailDeliveryStatus: "SENT",
    });
    render(<SignupPage onSignup={onSignup} />);

    fireEvent.change(screen.getByTestId("signup-login-id-input"), {
      target: { value: ` ${validForm.loginId} ` },
    });
    fireEvent.change(screen.getByTestId("signup-password-input"), {
      target: { value: validForm.password },
    });
    fireEvent.change(screen.getByTestId("signup-email-input"), {
      target: { value: " USER@Example.COM " },
    });
    fireEvent.click(screen.getByTestId("signup-submit-button"));

    await waitFor(() =>
      expect(onSignup).toHaveBeenCalledWith({
        loginId: "newuser",
        password: "Password1",
        email: "user@example.com",
      }),
    );
    expect(await screen.findByText(/이메일 인증 대기/)).toBeInTheDocument();
    expect(screen.getByText(/user@example.com/)).toBeInTheDocument();
  });

  it("cancel button returns to login route", () => {
    const onCancel = vi.fn();
    render(
      <SignupPage
        onSignup={async () => ({
          accountStatus: "PENDING_EMAIL",
          emailVerifiedYn: "N",
        })}
        onCancel={onCancel}
      />,
    );

    fireEvent.click(screen.getByTestId("signup-cancel-button"));

    expect(onCancel).toHaveBeenCalled();
  });

  it("requests verification resend with neutral accepted guidance", async () => {
    const onResend = vi.fn().mockResolvedValue({
      status: "ACCEPTED",
      message: "인증 메일 재발송 요청을 접수했습니다.",
    });
    render(
      <SignupPage
        onSignup={async () => ({
          accountStatus: "PENDING_EMAIL",
          emailVerifiedYn: "N",
        })}
        onResend={onResend}
      />,
    );

    fireEvent.change(screen.getByTestId("signup-resend-email-input"), {
      target: { value: " USER@Example.COM " },
    });
    fireEvent.click(screen.getByTestId("signup-resend-button"));

    await waitFor(() =>
      expect(onResend).toHaveBeenCalledWith("user@example.com"),
    );
    expect(
      await screen.findByText("인증 메일 재발송 요청을 접수했습니다."),
    ).toBeInTheDocument();
  });
});
