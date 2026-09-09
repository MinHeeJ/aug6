import { describe, expect, it, vi } from "vitest";
import { renderToStaticMarkup } from "react-dom/server";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import {
  EmailVerificationResultPage,
  normalizeVerificationStatus,
} from "./EmailVerificationResultPage";

function renderStatus(status: string | null) {
  return renderToStaticMarkup(
    <EmailVerificationResultPage initialStatus={status} />,
  );
}

describe("EmailVerificationResultPage", () => {
  it("renders success guidance and login CTA for SCR-EMAIL-VERIFY-RESULT", () => {
    const html = renderStatus("success");

    expect(html).toContain("SCR-EMAIL-VERIFY-RESULT");
    expect(html).toContain("이메일 인증 결과");
    expect(html).toContain("이메일 인증이 완료되었습니다. 로그인해주세요.");
    expect(html).toContain("로그인 페이지로 이동");
    expect(html).toContain("/login");
  });

  it("renders expired guidance with editable email resend form", () => {
    const html = renderStatus("expired");

    expect(html).toContain("인증 링크가 만료되었습니다.");
    expect(html).toContain('name="email"');
    expect(html).toContain("인증 메일 재발송");
    expect(html).not.toContain("user@example.test");
  });

  it("renders invalid guidance and signup CTA for missing or invalid status", () => {
    expect(normalizeVerificationStatus("unknown")).toBe("invalid");
    expect(normalizeVerificationStatus(null)).toBe("invalid");

    const html = renderStatus("invalid");
    expect(html).toContain("유효하지 않은 인증 링크입니다.");
    expect(html).toContain("회원가입 페이지로 이동");
    expect(html).toContain("/signup");
  });

  it("submits expired-result resend form with user-entered email and shows API message", async () => {
    const onResend = vi
      .fn()
      .mockResolvedValue("인증 메일 재발송 요청이 접수되었습니다.");

    render(
      <EmailVerificationResultPage
        initialStatus="expired"
        onResend={onResend}
      />,
    );
    fireEvent.change(
      screen.getByTestId("email-verification-resend-email-input"),
      {
        target: { value: "pending@example.edu" },
      },
    );
    fireEvent.submit(
      screen
        .getByTestId("email-verification-resend-button")
        .closest("form") as HTMLFormElement,
    );

    await waitFor(() =>
      expect(onResend).toHaveBeenCalledWith("pending@example.edu"),
    );
    expect(
      await screen.findByText("인증 메일 재발송 요청이 접수되었습니다."),
    ).toBeInTheDocument();
  });
});
