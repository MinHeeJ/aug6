import { useEffect, useMemo, useState } from "react";
import { ApiClientError, type EmailVerificationResult } from "../api/apiClient";

type EmailVerificationPageProps = {
  onVerify: (token: string) => Promise<EmailVerificationResult>;
  token?: string;
  onGoLogin?: () => void;
};

type VerificationState = "loading" | "success" | "invalid" | "error";

export function EmailVerificationPage({
  onVerify,
  token,
  onGoLogin,
}: EmailVerificationPageProps) {
  const resolvedToken = useMemo(() => {
    if (token !== undefined) return token;
    if (typeof window === "undefined") return "";
    return new URLSearchParams(window.location.search).get("token") ?? "";
  }, [token]);
  const [state, setState] = useState<VerificationState>("loading");
  const [message, setMessage] = useState("이메일 인증을 확인하고 있습니다.");
  const [result, setResult] = useState<EmailVerificationResult | null>(null);

  useEffect(() => {
    let cancelled = false;
    async function verify() {
      if (!/^[A-Fa-f0-9]{64}$/.test(resolvedToken)) {
        setState("invalid");
        setMessage(
          "인증 링크가 올바르지 않습니다. 메일의 최신 링크를 다시 확인하세요.",
        );
        return;
      }
      try {
        setState("loading");
        setMessage("이메일 인증을 확인하고 있습니다.");
        const verified = await onVerify(resolvedToken);
        if (cancelled) return;
        setResult(verified);
        setState("success");
        setMessage("이메일 인증이 완료되었습니다. 이제 로그인할 수 있습니다.");
      } catch (caught) {
        if (cancelled) return;
        setState(
          caught instanceof ApiClientError &&
            (caught.status === 400 || caught.status === 409)
            ? "invalid"
            : "error",
        );
        setMessage(
          caught instanceof Error
            ? caught.message
            : "이메일 인증 처리 중 오류가 발생했습니다.",
        );
      }
    }
    void verify();
    return () => {
      cancelled = true;
    };
  }, [onVerify, resolvedToken]);

  const goLogin = () => {
    if (onGoLogin) {
      onGoLogin();
      return;
    }
    if (typeof window !== "undefined") {
      window.history.replaceState({}, "", "/login");
      window.dispatchEvent(new PopStateEvent("popstate"));
    }
  };

  return (
    <main
      className="min-h-screen bg-lightgray px-5 py-[30px] text-link"
      data-testid="email-verification-screen"
      data-screen-id="SCR-EMAIL-VERIFICATION"
    >
      <section className="mx-auto max-w-4xl overflow-hidden rounded-md bg-white shadow-md">
        <header className="border-b border-ld bg-lightsecondary px-6 py-5">
          <p className="text-sm font-semibold text-primary">
            SCR-EMAIL-VERIFICATION
          </p>
          <h1 className="mt-2 text-2xl font-semibold text-dark">이메일 인증</h1>
          <p className="mt-2 text-sm text-muted">
            회원가입 인증 링크의 토큰 상태를 확인합니다.
          </p>
        </header>
        <div className="grid grid-cols-12 gap-6 p-6">
          <section className="col-span-12 rounded-md border border-ld bg-white p-6 lg:col-span-7">
            {state === "loading" ? (
              <div
                data-testid="email-verification-loading"
                className="rounded-md bg-lightprimary p-4 text-sm text-primary"
                role="status"
              >
                {message}
              </div>
            ) : null}
            {state === "success" ? (
              <div
                data-testid="email-verification-success"
                className="rounded-md bg-lightsuccess p-4 text-sm text-success"
                role="status"
              >
                <p className="font-semibold">인증 완료</p>
                <p className="mt-2">{message}</p>
                <p className="mt-2">
                  계정 상태: {result?.accountStatus ?? "ACTIVE"} / 이메일 인증:{" "}
                  {result?.emailVerifiedYn ?? "Y"}
                </p>
                {result?.email ? (
                  <p className="mt-2">인증 이메일: {result.email}</p>
                ) : null}
              </div>
            ) : null}
            {state === "invalid" ? (
              <div
                data-testid="email-verification-invalid"
                className="rounded-md bg-lightwarning p-4 text-sm text-warning"
                role="alert"
              >
                <p className="font-semibold">인증 링크를 사용할 수 없습니다</p>
                <p className="mt-2">{message}</p>
                <p className="mt-2">
                  만료·이미 사용됨·최신 메일로 대체됨 상태일 수 있습니다.
                </p>
              </div>
            ) : null}
            {state === "error" ? (
              <div
                data-testid="email-verification-error"
                className="rounded-md bg-lighterror p-4 text-sm text-error"
                role="alert"
              >
                <p className="font-semibold">인증 처리 오류</p>
                <p className="mt-2">{message}</p>
              </div>
            ) : null}
            <div className="mt-5 flex flex-col gap-3 sm:flex-row">
              <button
                data-testid="email-verification-login-link"
                type="button"
                className="btn-primary"
                onClick={goLogin}
              >
                로그인으로 이동
              </button>
            </div>
          </section>
          <aside className="col-span-12 rounded-md border border-ld bg-white p-6 lg:col-span-5">
            <h2 className="card-title mb-4 text-lg font-semibold text-dark">
              안내
            </h2>
            <ul className="space-y-2 text-sm text-muted">
              <li>인증 완료 후 기존 로그인 화면에서 로그인하세요.</li>
              <li>
                메일의 이전 링크는 새 인증 메일이 발급되면 사용할 수 없습니다.
              </li>
              <li>비밀번호나 내부 인증 정보는 화면에 표시하지 않습니다.</li>
            </ul>
          </aside>
        </div>
      </section>
    </main>
  );
}
