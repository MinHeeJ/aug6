import { useEffect, useRef, useState, type FormEvent } from "react";
import { ApiClientError } from "../../api/apiClient";
import { signupApi, type SignupRequest } from "./signupApi";

const labels: Record<keyof SignupRequest, string> = {
  userId: "아이디",
  password: "비밀번호",
  passwordConfirm: "비밀번호 확인",
  email: "이메일",
};
const userIdPattern = /^[a-z][a-z0-9]{3,19}$/;
const emailPattern = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

export function meetsPasswordRule(value: string, userId: string) {
  const categories = [/[A-Z]/, /[a-z]/, /[0-9]/, /[!-/:-@\[-`{-~]/];
  return (
    value.length >= 8 &&
    value !== userId &&
    categories.filter((pattern) => pattern.test(value)).length >= 3
  );
}

function navigateToLogin(message: string) {
  window.history.replaceState({ signupMessage: message }, "", "/login");
  window.dispatchEvent(new PopStateEvent("popstate"));
}

/** Public card, deliberately outside the protected admin menu model. */
export function SignupPage({
  onSignupSuccess = navigateToLogin,
}: {
  onSignupSuccess?: (message: string) => void;
}) {
  const [form, setForm] = useState<SignupRequest>({
    userId: "",
    password: "",
    passwordConfirm: "",
    email: "",
  });
  const [errors, setErrors] = useState<
    Partial<Record<keyof SignupRequest, string>>
  >({});
  const [message, setMessage] = useState("");
  const [availability, setAvailability] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState("");
  const generation = useRef(0);
  const active = useRef(true);
  const busy = useRef(false);

  useEffect(() => {
    active.current = true;
    return () => {
      active.current = false;
      generation.current += 1;
    };
  }, []);
  useEffect(() => {
    if (success) onSignupSuccess(success);
  }, [success, onSignupSuccess]);

  function change(field: keyof SignupRequest, value: string) {
    setForm((previous) => ({ ...previous, [field]: value }));
    setErrors((previous) => ({ ...previous, [field]: undefined }));
    setMessage("");
    if (field === "userId") {
      generation.current += 1;
      setAvailability("");
    }
  }

  async function checkUserId() {
    const current = ++generation.current;
    const userId = form.userId;
    if (!userIdPattern.test(userId)) {
      setAvailability("");
      setErrors((previous) => ({
        ...previous,
        userId: "영문 소문자로 시작하는 소문자·숫자 4~20자를 입력하세요.",
      }));
      return;
    }
    setAvailability("확인 중...");
    try {
      const response = await signupApi.checkUserIdAvailability(userId);
      if (!active.current || generation.current !== current) return;
      if (typeof response.data?.available !== "boolean")
        throw new Error("Missing availability");
      setAvailability(response.data.available ? "사용 가능" : "이미 사용 중");
      setErrors((previous) => ({ ...previous, userId: undefined }));
    } catch (caught) {
      if (!active.current || generation.current !== current) return;
      setAvailability("");
      setErrors((previous) => ({
        ...previous,
        userId:
          caught instanceof ApiClientError
            ? (caught.apiError?.fields.find((item) => item.field === "userId")
                ?.message ?? caught.message)
            : "아이디 확인에 실패했습니다. 다시 시도해 주세요.",
      }));
    }
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy.current || success) return;
    const invalid: Partial<Record<keyof SignupRequest, string>> = {};
    for (const field of Object.keys(labels) as (keyof SignupRequest)[]) {
      if (!form[field].trim())
        invalid[field] = `${labels[field]} 필수값을 입력해 주세요.`;
    }
    if (!invalid.userId && !userIdPattern.test(form.userId)) {
      invalid.userId =
        "영문 소문자로 시작하는 소문자·숫자 4~20자를 입력하세요.";
    }
    if (
      !invalid.email &&
      (!emailPattern.test(form.email) || form.email.length > 254)
    ) {
      invalid.email = "올바른 이메일 형식이 아닙니다.";
    }
    if (!invalid.passwordConfirm && form.password !== form.passwordConfirm) {
      invalid.passwordConfirm = "비밀번호와 비밀번호 확인이 일치하지 않습니다.";
    }
    if (!invalid.password && !meetsPasswordRule(form.password, form.userId)) {
      invalid.password =
        "8자 이상, 대문자·소문자·숫자·ASCII 특수문자 중 3종 이상을 포함하세요.";
    }
    setErrors(invalid);
    setMessage("");
    if (Object.keys(invalid).length) return;
    busy.current = true;
    generation.current += 1;
    setAvailability("");
    setSubmitting(true);
    try {
      const response = await signupApi.signup(form);
      if (!active.current) return;
      if (!response.data) throw new Error("Missing receipt");
      setForm((previous) => ({
        ...previous,
        password: "",
        passwordConfirm: "",
      }));
      setSuccess(response.data.message);
    } catch (caught) {
      if (!active.current) return;
      if (caught instanceof ApiClientError) {
        const fields: Partial<Record<keyof SignupRequest, string>> = {};
        for (const item of caught.apiError?.fields ?? []) {
          if (Object.prototype.hasOwnProperty.call(labels, item.field)) {
            fields[item.field as keyof SignupRequest] = item.message;
          }
        }
        setErrors(fields);
        setMessage(
          caught.status === 403
            ? "회원가입 접근 권한을 확인해 주세요."
            : caught.message,
        );
      } else {
        setMessage("가입 처리에 실패했습니다. 잠시 후 다시 시도해 주세요.");
      }
    } finally {
      busy.current = false;
      if (active.current) setSubmitting(false);
    }
  }

  return (
    <main
      data-screen-id="SCR-SIGNUP"
      data-testid="signup-page"
      className="flex min-h-screen items-center justify-center bg-lightgray px-5 py-[30px] text-link"
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
          {(Object.keys(labels) as (keyof SignupRequest)[]).map((field) => (
            <div className="mb-4" key={field}>
              <label
                className="block text-sm font-semibold text-link"
                htmlFor={`signup-${field}`}
              >
                {labels[field]}
              </label>
              <input
                id={`signup-${field}`}
                data-testid={`signup-${field.replace(/[A-Z]/g, (letter) => `-${letter.toLowerCase()}`)}-input`}
                name={field}
                type={
                  field.startsWith("password")
                    ? "password"
                    : field === "email"
                      ? "email"
                      : "text"
                }
                autoComplete={
                  field.startsWith("password")
                    ? "new-password"
                    : field === "email"
                      ? "email"
                      : "username"
                }
                className="mt-2 h-10 w-full rounded-lg border border-ld bg-transparent px-3 py-2 text-sm"
                value={form[field]}
                onChange={(event) => change(field, event.target.value)}
                onBlur={
                  field === "userId" ? () => void checkUserId() : undefined
                }
                disabled={submitting || Boolean(success)}
                aria-invalid={Boolean(errors[field])}
                aria-describedby={`signup-${field}-feedback`}
                required
              />
              <div
                id={`signup-${field}-feedback`}
                aria-live="polite"
                className="mt-1 text-xs"
              >
                {errors[field] ? (
                  <p className="text-error">{errors[field]}</p>
                ) : null}
                {field === "userId" && availability ? (
                  <p>{availability}</p>
                ) : null}
                {field === "password" && form.password ? (
                  <p>
                    {meetsPasswordRule(form.password, form.userId)
                      ? "비밀번호 규칙 충족"
                      : "8자 이상, 4범주 중 3종 이상 필요"}
                  </p>
                ) : null}
                {field === "passwordConfirm" && form.passwordConfirm ? (
                  <p>
                    {form.password === form.passwordConfirm
                      ? "비밀번호 일치"
                      : "비밀번호 불일치"}
                  </p>
                ) : null}
              </div>
            </div>
          ))}
          {message ? (
            <p role="alert" className="mb-4 text-sm text-error">
              {message}
            </p>
          ) : null}
          {success ? (
            <p role="status" className="mb-4 text-sm text-success">
              {success}
            </p>
          ) : null}
          <button
            data-testid="signup-submit-button"
            type="submit"
            disabled={submitting || Boolean(success)}
            className={[
              "inline-flex h-10 w-full items-center justify-center rounded-md",
              "bg-primary text-sm text-white disabled:opacity-50",
            ].join(" ")}
          >
            {submitting ? "가입 처리 중..." : "가입하기"}
          </button>
          <a
            data-testid="signup-login-link"
            href="/login"
            className="mt-4 block text-center text-sm text-primary"
          >
            로그인으로 돌아가기
          </a>
        </form>
      </section>
    </main>
  );
}

export default SignupPage;
