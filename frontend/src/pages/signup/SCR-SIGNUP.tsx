import { useEffect, useRef, useState } from "react";
import { ApiClientError } from "../../api/apiClient";
import { signupApi, type SignupRequest } from "./signupApi";

const fields = [
  { name: "userId", label: "아이디", type: "text", autoComplete: "username" },
  {
    name: "password",
    label: "비밀번호",
    type: "password",
    autoComplete: "new-password",
  },
  {
    name: "passwordConfirm",
    label: "비밀번호 확인",
    type: "password",
    autoComplete: "new-password",
  },
  { name: "email", label: "이메일", type: "email", autoComplete: "email" },
] as const;

type FieldErrors = Partial<Record<keyof SignupRequest, string>>;

export function passwordMeetsRules(password: string, userId: string) {
  const categories = [/[A-Z]/, /[a-z]/, /[0-9]/, /[^a-zA-Z0-9\s]/];
  return (
    password.length >= 8 &&
    categories.filter((rule) => rule.test(password)).length >= 3 &&
    password !== userId
  );
}

function navigateToLogin() {
  window.history.pushState({}, "", "/login");
  window.dispatchEvent(new PopStateEvent("popstate"));
}

/** Public form using the login screen's existing theme and the shared API/error envelope. */
export function SignupPage() {
  const [input, setInput] = useState<SignupRequest>({
    userId: "",
    password: "",
    passwordConfirm: "",
    email: "",
  });
  const [errors, setErrors] = useState<FieldErrors>({});
  const [message, setMessage] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [availability, setAvailability] = useState("");
  const idVersion = useRef(0);
  const mounted = useRef(true);
  const busy = useRef(false);

  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
      idVersion.current += 1;
    };
  }, []);

  function update(field: keyof SignupRequest, value: string) {
    setInput((current) => ({ ...current, [field]: value }));
    setErrors((current) => ({ ...current, [field]: undefined }));
    setMessage("");
    if (field === "userId") {
      idVersion.current += 1;
      setAvailability("");
    }
  }

  function showError(caught: unknown, fallback: string) {
    if (caught instanceof ApiClientError) {
      const next: FieldErrors = {};
      for (const error of caught.apiError?.fields ?? []) {
        if (fields.some((field) => field.name === error.field)) {
          next[error.field as keyof SignupRequest] = error.message;
        }
      }
      setErrors((current) => ({ ...current, ...next }));
      setMessage(
        caught.status === 403
          ? "회원가입 요청 권한이 없습니다."
          : caught.message,
      );
    } else {
      setMessage(fallback);
    }
  }

  async function checkUserId() {
    const version = ++idVersion.current;
    // A retry must replace the previous check's field/global failure, not display both outcomes.
    setErrors((current) => ({ ...current, userId: undefined }));
    setMessage("");
    if (!/^[a-z][a-z0-9]{3,19}$/.test(input.userId)) {
      setAvailability("");
      setErrors((current) => ({
        ...current,
        userId:
          "아이디는 영문 소문자로 시작하는 영문 소문자와 숫자 4~20자여야 합니다.",
      }));
      return;
    }
    setAvailability("아이디 확인 중입니다.");
    try {
      const response = await signupApi.checkUserId(input.userId);
      if (mounted.current && idVersion.current === version) {
        setAvailability(
          response.data?.available
            ? "사용 가능한 아이디입니다."
            : "이미 사용 중인 아이디입니다.",
        );
      }
    } catch (caught) {
      if (mounted.current && idVersion.current === version) {
        setAvailability("");
        showError(caught, "아이디 확인에 실패했습니다. 다시 시도해 주세요.");
      }
    }
  }

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy.current) return;
    const required: FieldErrors = {};
    for (const field of fields) {
      if (!input[field.name].trim())
        required[field.name] = "필수값을 입력해 주세요.";
    }
    setErrors(required);
    if (Object.keys(required).length > 0) {
      setMessage("필수값을 입력해 주세요.");
      return;
    }
    busy.current = true;
    idVersion.current += 1;
    setSubmitting(true);
    setMessage("회원가입 처리 중입니다.");
    try {
      const response = await signupApi.signup(input);
      if (!mounted.current) return;
      setInput((current) => ({
        ...current,
        password: "",
        passwordConfirm: "",
      }));
      // Acknowledgement is shown before route replacement, without putting credentials in URL/state.
      window.alert(response.data?.message ?? "가입이 완료되었습니다.");
      navigateToLogin();
    } catch (caught) {
      if (mounted.current)
        showError(
          caught,
          "회원가입에 실패했습니다. 잠시 후 다시 시도해 주세요.",
        );
    } finally {
      busy.current = false;
      if (mounted.current) setSubmitting(false);
    }
  }

  return (
    <main
      className="flex min-h-screen items-center justify-center bg-lightgray px-5 py-[30px] text-link"
      data-screen-id="SCR-SIGNUP"
      data-testid="signup-page"
    >
      <section className="w-full max-w-lg overflow-hidden rounded-md bg-white shadow-md">
        <header className="border-b border-ld bg-lightsecondary px-6 py-5">
          <p className="text-sm font-semibold text-primary">
            한국교원대학교 교수업적평가시스템
          </p>
          <h1 className="mt-2 text-2xl font-semibold text-dark">회원가입</h1>
        </header>
        <form
          className="p-6"
          onSubmit={submit}
          noValidate
          aria-busy={submitting}
        >
          {fields.map((field) => (
            <div className="mb-4" key={field.name}>
              <label
                className="block text-sm font-semibold text-link"
                htmlFor={`signup-${field.name}`}
              >
                {field.label}
              </label>
              <input
                id={`signup-${field.name}`}
                name={field.name}
                type={field.type}
                autoComplete={field.autoComplete}
                data-testid={`signup-${
                  field.name === "userId"
                    ? "user-id"
                    : field.name === "passwordConfirm"
                      ? "password-confirm"
                      : field.name
                }`}
                className="mt-2 h-10 w-full rounded-lg border border-ld px-3 py-2 text-sm text-link"
                value={input[field.name]}
                onChange={(event) => update(field.name, event.target.value)}
                onBlur={
                  field.name === "userId" ? () => void checkUserId() : undefined
                }
                disabled={submitting}
                required
                maxLength={
                  field.name === "email"
                    ? 254
                    : field.name === "userId"
                      ? 20
                      : undefined
                }
                aria-invalid={Boolean(errors[field.name])}
                aria-describedby={`signup-${field.name}-hint`}
              />
              <div
                id={`signup-${field.name}-hint`}
                className="mt-1 text-xs"
                aria-live="polite"
              >
                {errors[field.name] && (
                  <p className="text-error">{errors[field.name]}</p>
                )}
                {field.name === "userId" && (
                  <p className="text-muted">{availability}</p>
                )}
                {field.name === "password" && (
                  <p className="text-muted">
                    {input.password &&
                    passwordMeetsRules(input.password, input.userId)
                      ? "비밀번호 규칙을 충족합니다."
                      : "8자 이상, 대문자·소문자·숫자·특수문자 중 3종 이상을 포함하세요."}
                  </p>
                )}
                {field.name === "passwordConfirm" && input.passwordConfirm && (
                  <p className="text-muted">
                    {input.password === input.passwordConfirm
                      ? "비밀번호가 일치합니다."
                      : "비밀번호가 일치하지 않습니다."}
                  </p>
                )}
              </div>
            </div>
          ))}
          <button
            className="h-10 w-full rounded-md bg-primary px-4 py-2 text-sm font-medium text-white shadow-btn-shadow"
            type="submit"
            disabled={submitting}
            data-testid="signup-submit-button"
          >
            {submitting ? "처리 중" : "가입하기"}
          </button>
          <p className="mt-4 text-sm text-primary" role="status">
            {message}
          </p>
          <a
            className="mt-4 inline-block text-sm text-primary"
            href="/login"
            data-testid="signup-login-link"
            onClick={(event) => {
              if (
                event.button === 0 &&
                !event.ctrlKey &&
                !event.metaKey &&
                !event.shiftKey &&
                !event.altKey
              ) {
                event.preventDefault();
                navigateToLogin();
              }
            }}
          >
            로그인으로 돌아가기
          </a>
        </form>
      </section>
    </main>
  );
}

export default SignupPage;
