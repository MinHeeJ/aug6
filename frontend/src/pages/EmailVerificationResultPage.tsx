import type React from "react";
import { useMemo, useState } from "react";
import { ApiClientError, signupApi } from "../api/apiClient";

type VerificationStatus = "success" | "expired" | "invalid";

type EmailVerificationResultPageProps = {
  initialStatus?: string | null;
  onResend?: (email: string) => Promise<string>;
};

export function normalizeVerificationStatus(
  status: string | null | undefined,
): VerificationStatus {
  return status === "success" || status === "expired" || status === "invalid"
    ? status
    : "invalid";
}

export function EmailVerificationResultPage({
  initialStatus,
  onResend = async (email) => {
    const response = await signupApi.resendVerification(email);
    return response.data?.message ?? "인증 메일 재발송 요청이 접수되었습니다.";
  },
}: EmailVerificationResultPageProps) {
  const status = useMemo(
    () => normalizeVerificationStatus(initialStatus ?? statusFromLocation()),
    [initialStatus],
  );
  const [email, setEmail] = useState("");
  const [message, setMessage] = useState<string | null>(null);
  const [fieldError, setFieldError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const submitResend = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const normalizedEmail = email.trim();
    setFieldError(null);
    setMessage(null);
    if (!normalizedEmail) {
      setFieldError("이메일은 필수입니다.");
      return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalizedEmail)) {
      setFieldError("올바른 이메일 형식이 아닙니다.");
      return;
    }
    try {
      setSubmitting(true);
      setMessage(await onResend(normalizedEmail));
    } catch (caught) {
      if (caught instanceof ApiClientError && caught.apiError?.fields?.length) {
        setFieldError(caught.apiError.fields[0]?.message ?? null);
      }
      setMessage(
        caught instanceof ApiClientError
          ? (caught.apiError?.message ?? caught.message)
          : "인증 메일 재발송 처리 중 오류가 발생했습니다.",
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <main className="min-h-screen bg-lightgray px-5 py-[30px] text-link">
      <section
        className="mx-auto max-w-xl rounded-md bg-white p-6 shadow-md"
        data-testid="email-verification-result-container"
      >
        <p className="card-subtitle">SCR-EMAIL-VERIFY-RESULT</p>
        <h1 className="card-title text-xl font-semibold text-dark">
          이메일 인증 결과
        </h1>
        {status === "success" ? <SuccessResult /> : null}
        {status === "expired" ? (
          <ExpiredResult
            email={email}
            fieldError={fieldError}
            message={message}
            submitting={submitting}
            onEmailChange={(nextEmail) => {
              setEmail(nextEmail);
              setFieldError(null);
              setMessage(null);
            }}
            onSubmit={submitResend}
          />
        ) : null}
        {status === "invalid" ? <InvalidResult /> : null}
      </section>
    </main>
  );
}

function SuccessResult() {
  return (
    <div className="mt-5" data-testid="email-verification-success-panel">
      <div
        className="rounded-md bg-lightsuccess p-4 text-sm text-success"
        role="status"
      >
        이메일 인증이 완료되었습니다. 로그인해주세요.
      </div>
      <a
        className="mt-6 inline-flex h-10 items-center rounded-md bg-primary px-4 text-sm font-medium text-white"
        data-testid="email-verification-login-link"
        href="/login"
      >
        로그인 페이지로 이동
      </a>
    </div>
  );
}

function ExpiredResult({
  email,
  fieldError,
  message,
  submitting,
  onEmailChange,
  onSubmit,
}: {
  email: string;
  fieldError: string | null;
  message: string | null;
  submitting: boolean;
  onEmailChange: (email: string) => void;
  onSubmit: (event: React.FormEvent<HTMLFormElement>) => void;
}) {
  return (
    <div className="mt-5" data-testid="email-verification-expired-panel">
      <div
        className="rounded-md bg-lightwarning p-4 text-sm text-warning"
        role="alert"
      >
        인증 링크가 만료되었습니다. 인증 메일 재발송을 요청해주세요.
      </div>
      <form className="mt-5" onSubmit={onSubmit}>
        <label
          className="block text-sm font-semibold text-ld"
          htmlFor="email-verification-resend-email"
        >
          이메일<span className="ms-1 text-error">*</span>
          <input
            id="email-verification-resend-email"
            name="email"
            className="mt-2 flex h-10 w-full rounded-lg border border-ld bg-transparent px-3 py-2 text-sm text-ld focus-visible:border-primary focus-visible:outline-0"
            data-testid="email-verification-resend-email-input"
            value={email}
            onChange={(event) => onEmailChange(event.target.value)}
            aria-invalid={Boolean(fieldError)}
          />
        </label>
        {fieldError ? (
          <p className="mt-1 text-xs text-error">{fieldError}</p>
        ) : null}
        <button
          className="mt-4 inline-flex h-10 w-full items-center justify-center rounded-md bg-primary px-4 text-sm font-medium text-white disabled:opacity-50"
          data-testid="email-verification-resend-button"
          type="submit"
          disabled={submitting}
        >
          {submitting ? "재발송 요청 중" : "인증 메일 재발송"}
        </button>
      </form>
      {message ? (
        <div
          className="mt-4 rounded-md bg-lightprimary p-4 text-sm text-primary"
          role="status"
        >
          {message}
        </div>
      ) : null}
    </div>
  );
}

function InvalidResult() {
  return (
    <div className="mt-5" data-testid="email-verification-invalid-panel">
      <div
        className="rounded-md bg-lighterror p-4 text-sm text-error"
        role="alert"
      >
        유효하지 않은 인증 링크입니다.
      </div>
      <a
        className="mt-6 inline-flex h-10 items-center rounded-md bg-primary px-4 text-sm font-medium text-white"
        data-testid="email-verification-signup-link"
        href="/signup"
      >
        회원가입 페이지로 이동
      </a>
    </div>
  );
}

function statusFromLocation() {
  if (typeof window === "undefined") {
    return null;
  }
  return new URLSearchParams(window.location.search).get("status");
}
