import { useState, type FormEvent } from "react";
import {
  ApiClientError,
  authApi,
  type SignupRequest,
} from "../../api/apiClient";

type FieldErrors = Record<string, string>;

const userIdPattern = /^[a-z][a-z0-9]{3,19}$/;
const emailPattern = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

/**
 * 익명 사용자가 계정을 생성하고 아이디 사용 가능 여부를 확인하는 회원가입 화면이다.
 */
export function SignupPage({
  onNavigateToLogin = navigateToLogin,
}: {
  onNavigateToLogin?: () => void;
}) {
  const [form, setForm] = useState<SignupRequest>({
    userId: "",
    password: "",
    passwordConfirm: "",
    email: "",
  });
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [availability, setAvailability] = useState<
    "idle" | "checking" | "available" | "unavailable"
  >("idle");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [successMessage, setSuccessMessage] = useState("");
  const [requestError, setRequestError] = useState("");

  const updateField = (field: keyof SignupRequest, value: string) => {
    setForm((current) => ({ ...current, [field]: value }));
    setFieldErrors((current) => ({ ...current, [field]: "" }));
    setRequestError("");
    setSuccessMessage("");
    if (field === "userId") {
      setAvailability("idle");
    }
  };

  const checkUserId = async () => {
    const userId = form.userId.trim().toLowerCase();
    if (!userIdPattern.test(userId)) {
      setAvailability("idle");
      setFieldErrors((current) => ({
        ...current,
        userId:
          "아이디는 영문 소문자로 시작하는 4~20자의 영문 소문자와 숫자여야 합니다.",
      }));
      return;
    }

    setAvailability("checking");
    try {
      const response = await authApi.checkUserIdAvailability(userId);
      setAvailability(response.data?.available ? "available" : "unavailable");
      if (!response.data?.available) {
        setFieldErrors((current) => ({
          ...current,
          userId: "이미 사용 중인 아이디입니다.",
        }));
      }
    } catch (error) {
      applyApiError(error, setFieldErrors, setRequestError);
      setAvailability("idle");
    }
  };

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const payload = {
      ...form,
      userId: form.userId.trim().toLowerCase(),
      email: form.email.trim().toLowerCase(),
    };
    const validationErrors = validate(payload);
    if (Object.keys(validationErrors).length > 0) {
      setFieldErrors(validationErrors);
      return;
    }

    setIsSubmitting(true);
    setFieldErrors({});
    setRequestError("");
    try {
      const response = await authApi.signup(payload);
      setSuccessMessage(response.data?.message ?? "가입이 완료되었습니다.");
      onNavigateToLogin();
    } catch (error) {
      applyApiError(error, setFieldErrors, setRequestError);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <main
      className="flex min-h-screen items-center justify-center bg-lightsecondary p-4"
      data-testid="signup-page"
    >
      <section className="w-full max-w-md rounded-md bg-white p-6 shadow-md">
        <h1 className="text-2xl font-semibold text-dark">회원가입</h1>
        <p className="mt-2 text-sm text-muted">
          가입 완료 후 로그인 화면으로 이동합니다.
        </p>
        <form
          className="mt-6 space-y-4"
          onSubmit={submit}
          data-testid="signup-form"
        >
          <Field
            autoComplete="username"
            error={fieldErrors.userId}
            label="아이디"
            name="userId"
            onBlur={() => void checkUserId()}
            onChange={(value) => updateField("userId", value)}
            testId="signup-user-id-input"
            value={form.userId}
          />
          <AvailabilityMessage availability={availability} />
          <Field
            autoComplete="new-password"
            error={fieldErrors.password}
            label="비밀번호"
            name="password"
            onChange={(value) => updateField("password", value)}
            testId="signup-password-input"
            type="password"
            value={form.password}
          />
          <p className="text-xs text-muted">
            8자 이상이며 영문 대문자, 영문 소문자, 숫자, 특수문자 중 3종 이상을
            포함해야 합니다.
          </p>
          <Field
            autoComplete="new-password"
            error={fieldErrors.passwordConfirm}
            label="비밀번호 확인"
            name="passwordConfirm"
            onChange={(value) => updateField("passwordConfirm", value)}
            testId="signup-password-confirm-input"
            type="password"
            value={form.passwordConfirm}
          />
          <Field
            autoComplete="email"
            error={fieldErrors.email}
            label="이메일"
            name="email"
            onChange={(value) => updateField("email", value)}
            testId="signup-email-input"
            type="email"
            value={form.email}
          />
          {requestError ? (
            <p
              className="text-sm text-red-600"
              role="alert"
              data-testid="signup-request-error"
            >
              {requestError}
            </p>
          ) : null}
          {successMessage ? (
            <p
              className="text-sm text-green-700"
              role="status"
              data-testid="signup-success-message"
            >
              {successMessage}
            </p>
          ) : null}
          <div className="flex items-center gap-3">
            <button
              className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
              data-testid="signup-submit-button"
              disabled={isSubmitting}
              type="submit"
            >
              {isSubmitting ? "가입 처리 중" : "가입하기"}
            </button>
            <button
              className="text-sm font-medium text-primary underline"
              data-testid="signup-login-link"
              onClick={onNavigateToLogin}
              type="button"
            >
              로그인으로 돌아가기
            </button>
          </div>
        </form>
      </section>
    </main>
  );
}

function Field({
  autoComplete,
  error,
  label,
  name,
  onBlur,
  onChange,
  testId,
  type = "text",
  value,
}: {
  autoComplete?: string;
  error?: string;
  label: string;
  name: string;
  onBlur?: () => void;
  onChange: (value: string) => void;
  testId: string;
  type?: "email" | "password" | "text";
  value: string;
}) {
  return (
    <div>
      <label className="block text-sm font-medium text-dark" htmlFor={name}>
        {label}
      </label>
      <input
        autoComplete={autoComplete}
        className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm"
        data-testid={testId}
        id={name}
        name={name}
        onBlur={onBlur}
        onChange={(event) => onChange(event.target.value)}
        type={type}
        value={value}
      />
      {error ? <p className="mt-1 text-xs text-red-600">{error}</p> : null}
    </div>
  );
}

function AvailabilityMessage({
  availability,
}: {
  availability: "idle" | "checking" | "available" | "unavailable";
}) {
  if (availability === "checking") {
    return <p className="text-xs text-muted">아이디를 확인하고 있습니다.</p>;
  }
  if (availability === "available") {
    return <p className="text-xs text-green-700">사용 가능한 아이디입니다.</p>;
  }
  if (availability === "unavailable") {
    return <p className="text-xs text-red-600">이미 사용 중인 아이디입니다.</p>;
  }
  return null;
}

function validate(request: SignupRequest): FieldErrors {
  const errors: FieldErrors = {};
  if (!userIdPattern.test(request.userId)) {
    errors.userId =
      "아이디는 영문 소문자로 시작하는 4~20자의 영문 소문자와 숫자여야 합니다.";
  }
  if (!emailPattern.test(request.email)) {
    errors.email = "올바른 이메일 형식이 아닙니다.";
  }
  if (request.password !== request.passwordConfirm) {
    errors.passwordConfirm = "비밀번호와 비밀번호 확인이 일치하지 않습니다.";
  }
  if (!passwordHasThreeKinds(request.password)) {
    errors.password =
      "비밀번호는 8자 이상, 영문·숫자·특수문자 중 3종 이상을 포함해야 합니다.";
  }
  return errors;
}

function passwordHasThreeKinds(password: string) {
  const kinds = [
    /[A-Z]/.test(password),
    /[a-z]/.test(password),
    /\d/.test(password),
    /[^A-Za-z0-9]/.test(password),
  ].filter(Boolean).length;
  return password.length >= 8 && kinds >= 3;
}

function applyApiError(
  error: unknown,
  setFieldErrors: (errors: FieldErrors) => void,
  setRequestError: (message: string) => void,
) {
  if (error instanceof ApiClientError) {
    const fields = Object.fromEntries(
      (error.apiError?.fields ?? []).map((field) => [
        field.field,
        field.message,
      ]),
    );
    setFieldErrors(fields);
    setRequestError(Object.keys(fields).length > 0 ? "" : error.message);
    return;
  }
  setRequestError("회원가입 요청 중 오류가 발생했습니다.");
}

function navigateToLogin() {
  if (typeof window !== "undefined") {
    window.history.replaceState({}, "", "/login");
    window.dispatchEvent(new PopStateEvent("popstate"));
  }
}
