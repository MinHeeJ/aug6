import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { ApiClientError } from "../api/apiClient";
import { EmailVerificationPage } from "./EmailVerificationPage";

const validToken =
  "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

describe("EmailVerificationPage", () => {
  it("shows loading then success and login link state", async () => {
    const onVerify = vi.fn().mockResolvedValue({
      userId: 7,
      loginId: "newuser",
      email: "user@example.com",
      accountStatus: "ACTIVE",
      emailVerifiedYn: "Y",
    });
    const onGoLogin = vi.fn();

    render(
      <EmailVerificationPage
        token={validToken}
        onVerify={onVerify}
        onGoLogin={onGoLogin}
      />,
    );

    expect(screen.getByTestId("email-verification-loading")).toHaveTextContent(
      "이메일 인증을 확인",
    );
    await waitFor(() => expect(onVerify).toHaveBeenCalledWith(validToken));
    expect(
      await screen.findByTestId("email-verification-success"),
    ).toHaveTextContent("인증 완료");
    expect(screen.getByText(/user@example.com/)).toBeInTheDocument();
    fireEvent.click(screen.getByTestId("email-verification-login-link"));
    expect(onGoLogin).toHaveBeenCalled();
  });

  it("shows invalid guidance for malformed, expired, used, or superseded token responses", async () => {
    const onVerify = vi
      .fn()
      .mockRejectedValue(new ApiClientError(400, "만료된 인증 링크입니다."));

    render(<EmailVerificationPage token={validToken} onVerify={onVerify} />);

    expect(
      await screen.findByTestId("email-verification-invalid"),
    ).toHaveTextContent("만료된 인증 링크입니다");
    expect(screen.getByText(/만료·이미 사용됨·최신 메일/)).toBeInTheDocument();
  });

  it("does not call API for malformed local token and shows invalid state", async () => {
    const onVerify = vi.fn();

    render(<EmailVerificationPage token="bad-token" onVerify={onVerify} />);

    expect(
      await screen.findByTestId("email-verification-invalid"),
    ).toHaveTextContent("올바르지 않습니다");
    expect(onVerify).not.toHaveBeenCalled();
  });

  it("shows generic error state for unexpected network errors", async () => {
    const onVerify = vi.fn().mockRejectedValue(new Error("네트워크 오류"));

    render(<EmailVerificationPage token={validToken} onVerify={onVerify} />);

    expect(
      await screen.findByTestId("email-verification-error"),
    ).toHaveTextContent("네트워크 오류");
  });
});
