import type React from "react";
import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { ApiClientError, authApi, type CurrentUser } from "../api/apiClient";

type AuthStatus = "loading" | "authenticated" | "anonymous" | "error";

type AuthContextValue = {
  status: AuthStatus;
  user: CurrentUser | null;
  error: string | null;
  login: (loginId: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refresh: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>("loading");
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [error, setError] = useState<string | null>(null);

  const refresh = async () => {
    try {
      setStatus("loading");
      const response = await authApi.me();
      setUser(response.data ?? null);
      setError(null);
      setStatus("authenticated");
    } catch (caught) {
      if (caught instanceof ApiClientError && caught.status === 401) {
        setUser(null);
        setError(null);
        setStatus("anonymous");
        return;
      }
      setError(
        caught instanceof Error ? caught.message : "인증 상태 확인 실패",
      );
      setStatus("error");
    }
  };

  useEffect(() => {
    void refresh();
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      status,
      user,
      error,
      login: async (loginId: string, password: string) => {
        await authApi.login(loginId, password);
        const currentUser = await authApi.me();
        setUser(currentUser.data ?? null);
        setError(null);
        setStatus("authenticated");
      },
      logout: async () => {
        await authApi.logout();
        setUser(null);
        setStatus("anonymous");
      },
      refresh,
    }),
    [status, user, error],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth는 AuthProvider 내부에서만 사용할 수 있습니다.");
  }
  return context;
}
