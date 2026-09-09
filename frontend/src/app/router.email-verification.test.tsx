import { describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { AuthProvider } from "./AuthProvider";
import { AppRouter } from "./router";

const validToken =
  "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

describe("AppRouter email verification route", () => {
  it("renders anonymous SCR-EMAIL-VERIFICATION and calls updateEmailVerification", async () => {
    window.history.replaceState(
      {},
      "",
      `/email-verification?token=${validToken}`,
    );
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce({
        ok: false,
        status: 401,
        headers: { get: () => "application/json" },
        json: async () => ({
          success: false,
          error: { code: "UNAUTHENTICATED", message: "인증 필요", fields: [] },
          meta: {},
        }),
      })
      .mockResolvedValueOnce({
        ok: true,
        status: 200,
        headers: { get: () => "application/json" },
        json: async () => ({
          success: true,
          data: {
            loginId: "newuser",
            email: "user@example.com",
            accountStatus: "ACTIVE",
            emailVerifiedYn: "Y",
          },
          meta: {},
        }),
      });
    vi.stubGlobal("fetch", fetchMock);

    render(
      <AuthProvider>
        <AppRouter />
      </AuthProvider>,
    );

    await waitFor(() =>
      expect(
        screen.getByTestId("email-verification-screen"),
      ).toBeInTheDocument(),
    );
    expect(
      await screen.findByTestId("email-verification-success"),
    ).toHaveTextContent("인증 완료");
    expect(fetchMock.mock.calls[1][0]).toBe(
      "/api/auth/email-verifications/verify",
    );
    expect(fetchMock.mock.calls[1][1]?.body).toBe(
      JSON.stringify({ token: validToken }),
    );
  });
});
