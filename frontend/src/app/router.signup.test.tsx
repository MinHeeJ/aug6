import { describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { AuthProvider } from "./AuthProvider";
import { AppRouter } from "./router";

describe("AppRouter signup route", () => {
  it("renders anonymous SCR-SIGNUP at /signup without SMTP credential fields", async () => {
    window.history.replaceState({}, "", "/signup");
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: false,
        status: 401,
        headers: { get: () => "application/json" },
        json: async () => ({
          success: false,
          error: { code: "UNAUTHENTICATED", message: "인증 필요", fields: [] },
          meta: {},
        }),
      }),
    );

    render(
      <AuthProvider>
        <AppRouter />
      </AuthProvider>,
    );

    await waitFor(() =>
      expect(screen.getByTestId("signup-screen")).toBeInTheDocument(),
    );
    expect(screen.getByTestId("signup-login-id-input")).toBeInTheDocument();
    expect(screen.getByTestId("signup-password-input")).toBeInTheDocument();
    expect(screen.getByTestId("signup-email-input")).toBeInTheDocument();
    expect(screen.queryByTestId("smtp-username-input")).not.toBeInTheDocument();
    expect(screen.queryByTestId("smtp-password-input")).not.toBeInTheDocument();
  });
});
