import type React from "react";
import { useState } from "react";
import { ApiClientError, signupApi } from "../api/apiClient";

type FieldName = "loginId" | "password" | "passwordConfirm" | "email";
export type SignupFieldErrors = Partial<Record<FieldName, string>>;

export type SignupFormValues = Record<FieldName, string>;

export function validateSignupInput(
  values: SignupFormValues,
): SignupFieldErrors {
  const errors: SignupFieldErrors = {};
  if (!values.loginId.trim()) errors.loginId = "아이디는 필수입니다.";
  else if (!/^[a-z][a-z0-9]{3,19}$/.test(values.loginId.trim())) {
    errors.loginId = "아이디는 영문 소문자로 시작하고 4~20자로 입력해주세요.";
  }
  if (!values.email.trim()) errors.email = "이메일은 필수입니다.";
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(values.email.trim())) {
    errors.email = "올바른 이메일 형식이 아닙니다.";
  }
  if (!values.password) errors.password = "비밀번호는 필수입니다.";
  else if (isWeakPassword(values.loginId, values.password)) {
    errors.password =
      "비밀번호는 8자 이상, 3종 이상, 동일문자 3회 금지 조건을 만족해야 합니다.";
  }
  if (!values.passwordConfirm)
    errors.passwordConfirm = "비밀번호 확인은 필수입니다.";
  else if (values.password !== values.passwordConfirm) {
    errors.passwordConfirm = "비밀번호와 비밀번호 확인이 일치하지 않습니다.";
  }
  return errors;
}

function isWeakPassword(loginId: string, password: string) {
  const categories = [/[A-Z]/, /[a-z]/, /[0-9]/, /[^A-Za-z0-9]/].filter(
    (pattern) => pattern.test(password),
  ).length;
  return (
    password.length < 8 ||
    categories < 3 ||
    password === loginId ||
    /(.)\1\1/.test(password)
  );
}

export function describeSignupFailure(caught: unknown): string {
  if (caught instanceof ApiClientError && caught.apiError?.code) {
    return caught.apiError.message;
  }
  return caught instanceof Error
    ? caught.message
    : "회원가입 처리 중 오류가 발생했습니다.";
}

type SignupPageProps = {
  onSignup?: (values: SignupFormValues) => Promise<void>;
  onCheckLoginId?: (loginId: string) => Promise<string>;
  onCheckEmail?: (email: string) => Promise<string>;
  onResend?: (email: string) => Promise<string>;
};

export function SignupPage({
  onSignup = async (values) => {
    await signupApi.create(values);
  },
  onCheckLoginId = async (loginId) => {
    const response = await signupApi.checkLoginId(loginId);
    return response.data?.message ?? "사용 가능한 아이디입니다.";
  },
  onCheckEmail = async (email) => {
    const response = await signupApi.checkEmail(email);
    return response.data?.message ?? "사용 가능한 이메일입니다.";
  },
  onResend = async (email) => {
    await signupApi.resendVerification(email);
    return "인증 메일 재발송 요청이 접수되었습니다.";
  },
}: SignupPageProps) {
  const [values, setValues] = useState<SignupFormValues>({
    loginId: "",
    password: "",
    passwordConfirm: "",
    email: "",
  });
  const [fieldErrors, setFieldErrors] = useState<SignupFieldErrors>({});
  const [message, setMessage] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const update =
    (field: FieldName) => (event: React.ChangeEvent<HTMLInputElement>) => {
      setValues((current) => ({ ...current, [field]: event.target.value }));
      setFieldErrors((current) => ({ ...current, [field]: undefined }));
      setMessage(null);
    };

  const applyApiError = (caught: unknown) => {
    if (caught instanceof ApiClientError && caught.apiError?.fields?.length) {
      setFieldErrors(
        Object.fromEntries(
          caught.apiError.fields.map((field) => [field.field, field.message]),
        ) as SignupFieldErrors,
      );
    }
    setMessage(describeSignupFailure(caught));
  };

  const checkLoginId = async () => {
    const loginIdError = validateSignupInput({
      ...values,
      password: "Strong!123",
      passwordConfirm: "Strong!123",
    }).loginId;
    if (loginIdError) {
      setFieldErrors((current) => ({ ...current, loginId: loginIdError }));
      return;
    }
    try {
      setMessage(await onCheckLoginId(values.loginId.trim()));
    } catch (caught) {
      applyApiError(caught);
    }
  };

  const checkEmail = async () => {
    const emailError = validateSignupInput({
      ...values,
      password: "Strong!123",
      passwordConfirm: "Strong!123",
    }).email;
    if (emailError) {
      setFieldErrors((current) => ({ ...current, email: emailError }));
      return;
    }
    try {
      setMessage(await onCheckEmail(values.email.trim()));
    } catch (caught) {
      applyApiError(caught);
    }
  };

  const submit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const errors = validateSignupInput(values);
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) {
      setMessage("입력값을 확인하세요.");
      return;
    }
    try {
      setSubmitting(true);
      setMessage("회원가입 처리 중입니다.");
      await onSignup(values);
      setSuccess(true);
      setMessage("인증 메일이 발송되었습니다. 이메일을 확인해주세요.");
    } catch (caught) {
      applyApiError(caught);
    } finally {
      setSubmitting(false);
    }
  };

  const resend = async () => {
    try {
      setMessage(await onResend(values.email.trim()));
    } catch (caught) {
      applyApiError(caught);
    }
  };

  if (success) {
    return (
      <main className="min-h-screen bg-lightgray px-5 py-[30px] text-link">
        <section
          className="mx-auto max-w-xl rounded-md bg-white p-6 shadow-md"
          data-testid="signup-success-container"
        >
          <p className="card-subtitle">SCR-SIGNUP</p>
          <h1 className="card-title text-xl font-semibold text-dark">
            회원가입 접수
          </h1>
          <div
            className="mt-4 rounded-md bg-lightsuccess p-4 text-sm text-success"
            role="status"
          >
            {message ?? "인증 메일이 발송되었습니다. 이메일을 확인해주세요."}
          </div>
          <p className="mt-4 text-sm text-muted">
            이메일 인증을 완료하면 가입한 계정으로 로그인할 수 있습니다. 메일을
            받지 못했다면 재발송을 요청하세요.
          </p>
          <div className="mt-6 flex flex-wrap gap-3">
            <button
              className="inline-flex h-10 items-center rounded-md border border-primary px-4 text-sm font-medium text-primary"
              data-testid="signup-resend-button"
              type="button"
              onClick={() => void resend()}
            >
              인증 메일 재발송
            </button>
            <a
              className="inline-flex h-10 items-center rounded-md bg-primary px-4 text-sm font-medium text-white"
              href="/login"
            >
              로그인으로 돌아가기
            </a>
          </div>
        </section>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-lightgray px-5 py-[30px] text-link">
      <section
        className="mx-auto max-w-xl rounded-md bg-white p-6 shadow-md"
        data-testid="signup-page-container"
      >
        <p className="card-subtitle">SCR-SIGNUP</p>
        <h1 className="card-title mb-4 text-xl font-semibold text-dark">
          회원가입
        </h1>
        <form onSubmit={submit}>
          <SignupInput
            id="signup-login-id"
            name="loginId"
            label="아이디"
            value={values.loginId}
            onChange={update("loginId")}
            onBlur={checkLoginId}
            error={fieldErrors.loginId}
            hint="영문 소문자 시작, 4~20자"
          />
          <SignupInput
            id="signup-password"
            name="password"
            label="비밀번호"
            type="password"
            value={values.password}
            onChange={update("password")}
            error={fieldErrors.password}
            hint="8자 이상, 영문 대소문자·숫자·특수문자 중 3종 이상"
          />
          <SignupInput
            id="signup-password-confirm"
            name="passwordConfirm"
            label="비밀번호 확인"
            type="password"
            value={values.passwordConfirm}
            onChange={update("passwordConfirm")}
            error={fieldErrors.passwordConfirm}
          />
          <SignupInput
            id="signup-email"
            name="email"
            label="이메일"
            value={values.email}
            onChange={update("email")}
            onBlur={checkEmail}
            error={fieldErrors.email}
            hint="인증 메일을 받을 주소를 입력하세요."
          />
          <button
            className="mt-2 inline-flex h-10 w-full items-center justify-center rounded-md bg-primary px-4 text-sm font-medium text-white disabled:opacity-50"
            data-testid="signup-submit-button"
            type="submit"
            disabled={submitting}
          >
            {submitting ? "처리 중" : "가입하기"}
          </button>
        </form>
        <div
          className="mt-4 rounded-md bg-lightprimary p-4 text-sm text-primary"
          role="status"
        >
          {message ?? "입력 후 가입하기를 누르면 인증 메일이 발송됩니다."}
        </div>
        <a
          className="mt-4 inline-flex text-sm font-medium text-primary"
          href="/login"
        >
          로그인으로 돌아가기
        </a>
      </section>
    </main>
  );
}

type SignupInputProps = {
  id: string;
  name: FieldName;
  label: string;
  value: string;
  type?: string;
  hint?: string;
  error?: string;
  onChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
  onBlur?: () => void;
};

function SignupInput({
  id,
  name,
  label,
  value,
  type = "text",
  hint,
  error,
  onChange,
  onBlur,
}: SignupInputProps) {
  return (
    <label className="mb-4 block text-sm font-semibold text-ld" htmlFor={id}>
      {label}
      <span className="ms-1 text-error">*</span>
      <input
        id={id}
        name={name}
        type={type}
        className="mt-2 flex h-10 w-full rounded-lg border border-ld bg-transparent px-3 py-2 text-sm text-ld focus-visible:border-primary focus-visible:outline-0"
        data-testid={`${id}-input`}
        value={value}
        onChange={onChange}
        onBlur={() => void onBlur?.()}
        aria-invalid={Boolean(error)}
      />
      {hint ? (
        <span className="mt-1 block text-xs text-muted">{hint}</span>
      ) : null}
      {error ? (
        <span className="mt-1 block text-xs text-error">{error}</span>
      ) : null}
    </label>
  );
}
