import type React from "react";
import { useState } from "react";
import {
  ApiClientError,
  type EmailVerificationResendResult,
  type SignupPayload,
  type SignupResult,
} from "../api/apiClient";

type SignupPageProps = {
  onSignup: (payload: SignupPayload) => Promise<SignupResult>;
  onCancel?: () => void;
  onResend?: (email: string) => Promise<EmailVerificationResendResult>;
};

export type SignupValidationErrors = Partial<
  Record<"loginId" | "password" | "email", string>
>;

const EMAIL_PATTERN = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;

export function validateSignupInput(
  payload: SignupPayload,
): SignupValidationErrors {
  const errors: SignupValidationErrors = {};
  if (!payload.loginId.trim()) {
    errors.loginId = "로그인 ID를 입력하세요.";
  }
  if (!/^(?=.*[A-Za-z])(?=.*\d).{8,255}$/.test(payload.password.trim())) {
    errors.password = "비밀번호는 8자 이상이며 영문과 숫자를 포함해야 합니다.";
  }
  if (
    !EMAIL_PATTERN.test(payload.email.trim()) ||
    payload.email.trim().length > 320
  ) {
    errors.email = "올바른 이메일 주소를 입력하세요.";
  } else if (payload.email.trim().toLowerCase() === "admin@kndadmin.com") {
    errors.email = "admin@kndadmin.com은 신규 회원가입에 사용할 수 없습니다.";
  }
  return errors;
}

export function SignupPage({ onSignup, onCancel, onResend }: SignupPageProps) {
  const [form, setForm] = useState<SignupPayload>({
    loginId: "",
    password: "",
    email: "",
  });
  const [resendEmail, setResendEmail] = useState("");
  const [fieldErrors, setFieldErrors] = useState<SignupValidationErrors>({});
  const [resendError, setResendError] = useState<string | null>(null);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [result, setResult] = useState<SignupResult | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [resending, setResending] = useState(false);

  const updateField =
    (field: keyof SignupPayload) =>
    (event: React.ChangeEvent<HTMLInputElement>) => {
      setForm((current) => ({ ...current, [field]: event.target.value }));
      setFieldErrors((current) => ({ ...current, [field]: undefined }));
      setStatusMessage(null);
    };

  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const normalized: SignupPayload = {
      loginId: form.loginId.trim(),
      password: form.password.trim(),
      email: form.email.trim().toLowerCase(),
    };
    const errors = validateSignupInput(normalized);
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) {
      setStatusMessage("입력값을 확인하세요.");
      return;
    }
    try {
      setSubmitting(true);
      setStatusMessage("회원가입 신청을 처리하고 있습니다.");
      const response = await onSignup(normalized);
      setResult(response);
      setStatusMessage("가입 신청이 완료되었습니다. 인증 메일을 확인하세요.");
    } catch (caught) {
      if (caught instanceof ApiClientError && caught.apiError?.fields?.length) {
        setFieldErrors(
          Object.fromEntries(
            caught.apiError.fields.map((field) => [field.field, field.message]),
          ) as SignupValidationErrors,
        );
      }
      setStatusMessage(
        caught instanceof Error
          ? caught.message
          : "회원가입 신청 중 오류가 발생했습니다.",
      );
    } finally {
      setSubmitting(false);
    }
  };

  const cancel = () => {
    if (onCancel) {
      onCancel();
      return;
    }
    if (typeof window !== "undefined") {
      window.history.replaceState({}, "", "/login");
      window.dispatchEvent(new PopStateEvent("popstate"));
    }
  };

  const requestResend = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const email = resendEmail.trim().toLowerCase();
    if (!EMAIL_PATTERN.test(email) || email.length > 320) {
      setResendError("올바른 이메일 주소를 입력하세요.");
      return;
    }
    if (!onResend) {
      setResendError("인증 메일 재발송 API 연결이 필요합니다.");
      return;
    }
    try {
      setResending(true);
      setResendError(null);
      const response = await onResend(email);
      setStatusMessage(
        response.message || "인증 메일 재발송 요청을 접수했습니다.",
      );
    } catch (caught) {
      if (caught instanceof ApiClientError && caught.apiError?.fields?.length) {
        const emailError = caught.apiError.fields.find(
          (field) => field.field === "email",
        );
        setResendError(emailError?.message ?? caught.apiError.message);
      } else {
        setResendError(
          caught instanceof Error
            ? caught.message
            : "인증 메일 재발송 요청 중 오류가 발생했습니다.",
        );
      }
    } finally {
      setResending(false);
    }
  };

  return (
    <main
      className="min-h-screen bg-lightgray px-5 py-[30px] text-link"
      data-testid="signup-screen"
      data-screen-id="SCR-SIGNUP"
    >
      <section className="mx-auto max-w-5xl overflow-hidden rounded-md bg-white shadow-md">
        <header className="border-b border-ld bg-lightsecondary px-6 py-5">
          <p className="text-sm font-semibold text-primary">SCR-SIGNUP</p>
          <h1 className="mt-2 text-2xl font-semibold text-dark">회원가입</h1>
          <p className="mt-2 text-sm text-muted">
            사용 중인 이메일 주소로 가입 신청 후 인증 메일 링크를 확인하세요.
          </p>
        </header>
        <div className="grid grid-cols-12 gap-6 p-6">
          <form
            className="col-span-12 rounded-md border border-ld bg-white p-6 lg:col-span-7"
            onSubmit={submit}
          >
            <label
              className="mb-4 block text-sm font-semibold text-ld"
              htmlFor="signup-login-id"
            >
              로그인 ID<span className="ms-1 text-error">*</span>
              <input
                id="signup-login-id"
                data-testid="signup-login-id-input"
                className="mt-2 flex h-10 w-full rounded-lg border border-ld bg-transparent px-3 py-2 text-sm text-ld focus-visible:border-primary focus-visible:outline-0"
                value={form.loginId}
                onChange={updateField("loginId")}
                aria-invalid={Boolean(fieldErrors.loginId)}
              />
              {fieldErrors.loginId ? (
                <span className="mt-1 block text-xs text-error">
                  {fieldErrors.loginId}
                </span>
              ) : null}
            </label>
            <label
              className="mb-4 block text-sm font-semibold text-ld"
              htmlFor="signup-password"
            >
              비밀번호<span className="ms-1 text-error">*</span>
              <input
                id="signup-password"
                data-testid="signup-password-input"
                type="password"
                className="mt-2 flex h-10 w-full rounded-lg border border-ld bg-transparent px-3 py-2 text-sm text-ld focus-visible:border-primary focus-visible:outline-0"
                value={form.password}
                onChange={updateField("password")}
                aria-invalid={Boolean(fieldErrors.password)}
              />
              {fieldErrors.password ? (
                <span className="mt-1 block text-xs text-error">
                  {fieldErrors.password}
                </span>
              ) : null}
            </label>
            <label
              className="mb-4 block text-sm font-semibold text-ld"
              htmlFor="signup-email"
            >
              이메일<span className="ms-1 text-error">*</span>
              <input
                id="signup-email"
                data-testid="signup-email-input"
                type="email"
                className="mt-2 flex h-10 w-full rounded-lg border border-ld bg-transparent px-3 py-2 text-sm text-ld focus-visible:border-primary focus-visible:outline-0"
                value={form.email}
                onChange={updateField("email")}
                aria-invalid={Boolean(fieldErrors.email)}
              />
              {fieldErrors.email ? (
                <span className="mt-1 block text-xs text-error">
                  {fieldErrors.email}
                </span>
              ) : null}
            </label>
            <div className="flex flex-col gap-3 sm:flex-row">
              <button
                data-testid="signup-submit-button"
                className="btn-primary"
                type="submit"
                disabled={submitting}
              >
                {submitting ? "처리 중" : "가입 신청"}
              </button>
              <button
                data-testid="signup-cancel-button"
                className="btn-secondary"
                type="button"
                onClick={cancel}
              >
                취소
              </button>
            </div>
            <div
              className="mt-4 rounded-md bg-lightprimary p-4 text-sm text-primary"
              role="status"
            >
              {statusMessage ?? "로그인 ID, 비밀번호, 이메일을 입력하세요."}
            </div>
          </form>
          <aside className="col-span-12 rounded-md border border-ld bg-white p-6 lg:col-span-5">
            <h2 className="card-title mb-4 text-lg font-semibold text-dark">
              이메일 인증 안내
            </h2>
            {result ? (
              <div className="rounded-md bg-lightsuccess p-4 text-sm text-success">
                <p className="font-semibold">이메일 인증 대기</p>
                <p className="mt-2">
                  {result.email ?? form.email.trim().toLowerCase()} 주소로 인증
                  메일 발송을 시도했습니다.
                </p>
                <p className="mt-2">계정 상태: {result.accountStatus}</p>
              </div>
            ) : (
              <div className="rounded-md bg-lightsecondary p-4 text-sm text-link">
                <p>신규 계정은 이메일 인증 완료 전까지 로그인할 수 없습니다.</p>
                <p className="mt-2">
                  SMTP 계정 또는 비밀번호는 회원가입 화면에서 입력받지 않습니다.
                </p>
              </div>
            )}
            <form
              className="mt-4 rounded-md border border-ld p-4 text-sm"
              onSubmit={requestResend}
            >
              <label
                className="block font-semibold text-ld"
                htmlFor="signup-resend-email"
              >
                인증 메일 재발송
                <input
                  id="signup-resend-email"
                  data-testid="signup-resend-email-input"
                  type="email"
                  className="mt-2 flex h-10 w-full rounded-lg border border-ld bg-transparent px-3 py-2 text-sm text-ld focus-visible:border-primary focus-visible:outline-0"
                  value={resendEmail}
                  onChange={(event) => {
                    setResendEmail(event.target.value);
                    setResendError(null);
                  }}
                  aria-invalid={Boolean(resendError)}
                />
              </label>
              {resendError ? (
                <p className="mt-2 text-xs text-error">{resendError}</p>
              ) : null}
              <button
                data-testid="signup-resend-button"
                className="btn-secondary mt-3"
                type="submit"
                disabled={resending}
              >
                {resending ? "요청 중" : "재발송 요청"}
              </button>
              <p className="mt-2 text-xs text-muted">
                계정 존재 여부를 드러내지 않는 중립 응답으로 처리됩니다.
              </p>
            </form>
          </aside>
        </div>
      </section>
    </main>
  );
}
