import type React from "react";
import { RefreshCw, Search, ShieldCheck } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import {
  ApiClientError,
  privacyPolicyApi,
  type ApiErrorField,
  type PrivacyFieldPolicy,
  type PrivacyGrade,
  type YesNo,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

export type PrivacyPolicyFormState = {
  fieldKey: string;
  privacyGrade: PrivacyGrade;
  encryptionRequiredYn: YesNo;
  maskingRule: string;
  logExclusionYn: YesNo;
  changeReason: string;
};

type FormState = PrivacyPolicyFormState;

const privacyGrades: PrivacyGrade[] = [
  "PUBLIC",
  "PERSONAL",
  "SENSITIVE",
  "ACCOUNT",
];
const yesNoValues: YesNo[] = ["Y", "N"];
export const pageSizes = [20, 50, 100] as const;

export function getPrivacyPolicyRouteContract() {
  return {
    route: "/admin/privacy/policies",
    screenId: "SCR-PRIVACY-POLICY-MGMT",
    operations: ["listPrivacyFieldPolicies", "savePrivacyFieldPolicies"],
    pageSizes: [...pageSizes],
    defaultPageSize: 20,
  };
}

export function validatePrivacyPolicyForm(form: PrivacyPolicyFormState) {
  const errors: Record<string, string> = {};
  if (!form.fieldKey.trim()) {
    errors.fieldKey = "개인정보 필드를 입력하세요.";
  }
  if (!form.changeReason.trim()) {
    errors.changeReason = "변경 사유를 입력하세요.";
  }
  return errors;
}

export function buildPrivacyPolicySavePayload(form: PrivacyPolicyFormState) {
  return {
    fieldKey: form.fieldKey,
    privacyGrade: form.privacyGrade,
    encryptionRequiredYn: form.encryptionRequiredYn,
    maskingRule: form.maskingRule || null,
    logExclusionYn: form.logExclusionYn,
    changeReason: form.changeReason,
  };
}
const initialForm: FormState = {
  fieldKey: "",
  privacyGrade: "PERSONAL",
  encryptionRequiredYn: "N",
  maskingRule: "",
  logExclusionYn: "N",
  changeReason: "",
};

export function PrivacyPolicyManagementPage() {
  const [fieldKey, setFieldKey] = useState("");
  const [privacyGrade, setPrivacyGrade] = useState("");
  const [encryptionRequiredYn, setEncryptionRequiredYn] = useState("");
  const [policies, setPolicies] = useState<PrivacyFieldPolicy[]>([]);
  const [selected, setSelected] = useState<PrivacyFieldPolicy | null>(null);
  const [form, setForm] = useState<FormState>(initialForm);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const selectedLabel = useMemo(
    () => selected?.fieldKey ?? "선택된 개인정보 필드 정책 없음",
    [selected],
  );

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await privacyPolicyApi.listPrivacyFieldPolicies({
        fieldKey,
        privacyGrade,
        encryptionRequiredYn,
        page,
        size,
      });
      setPolicies(response.data?.policies ?? []);
      setTotalElements(response.data?.totalElements ?? 0);
      setSelected(null);
      setForm(initialForm);
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page, size]);

  const selectRow = (policy: PrivacyFieldPolicy) => {
    setSelected(policy);
    setFieldErrors({});
    setSuccessMessage(null);
    setForm({
      fieldKey: policy.fieldKey,
      privacyGrade: policy.privacyGrade,
      encryptionRequiredYn: policy.encryptionRequiredYn,
      maskingRule: policy.maskingRule ?? "",
      logExclusionYn: policy.logExclusionYn,
      changeReason: "",
    });
  };

  const resetForm = () => {
    if (selected) {
      selectRow(selected);
      return;
    }
    setForm(initialForm);
  };

  const save = async () => {
    const nextFieldErrors = validatePrivacyPolicyForm(form);
    setFieldErrors(nextFieldErrors);
    if (Object.keys(nextFieldErrors).length > 0) {
      setError("필수 입력 항목을 확인하세요.");
      return;
    }
    const confirmed = window.confirm(
      `${form.fieldKey} 보호정책을 저장하시겠습니까? 실제 개인정보 원문값은 저장하지 않습니다.`,
    );
    if (!confirmed) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      await privacyPolicyApi.savePrivacyFieldPolicies([
        buildPrivacyPolicySavePayload(form),
      ]);
      setSuccessMessage("저장되었습니다");
      await load();
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setSaving(false);
    }
  };

  const handleApiError = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      if (caught.status === 403) {
        setPermissionDenied(true);
        setError(caught.message);
        return;
      }
      setError(caught.message);
      setFieldErrors(toFieldErrorMap(caught.apiError?.fields ?? []));
      return;
    }
    setError(
      caught instanceof Error
        ? caught.message
        : "개인정보 보호정책 정보를 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-PRIVACY-POLICY-MGMT"
        data-testid="privacy-policy-page"
      >
        <PermissionState
          title="개인정보 항목 관리 권한이 없습니다"
          message="R09 시스템관리자 또는 해당 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-PRIVACY-POLICY-MGMT"
      data-testid="privacy-policy-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">보안·감사 관리 / 개인정보 관리</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              개인정보 항목 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              개인정보 필드별 등급, 암호화, 마스킹, 일반 로그 제외 정책을
              관리합니다. 실제 사용자 개인정보 원문값은 조회하거나 수정하지
              않습니다.
            </p>
          </div>
          <ShieldCheck className="h-10 w-10 text-primary" aria-hidden="true" />
        </div>
      </div>

      <section className="rounded-md border border-ld bg-white p-5 shadow-md">
        <div className="grid gap-3 lg:grid-cols-5">
          <label className="text-sm font-medium text-dark">
            필드명/field_key 검색
            <input
              className="mt-1 w-full rounded-md border border-ld px-3 py-2"
              data-testid="privacy-policy-field-key-filter-input"
              value={fieldKey}
              onChange={(event) => setFieldKey(event.target.value)}
            />
          </label>
          <label className="text-sm font-medium text-dark">
            등급 선택
            <select
              className="mt-1 w-full rounded-md border border-ld px-3 py-2"
              data-testid="privacy-policy-grade-filter-select"
              value={privacyGrade}
              onChange={(event) => setPrivacyGrade(event.target.value)}
            >
              <option value="">전체</option>
              {privacyGrades.map((grade) => (
                <option key={grade} value={grade}>
                  {grade}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm font-medium text-dark">
            암호화 Y/N
            <select
              className="mt-1 w-full rounded-md border border-ld px-3 py-2"
              data-testid="privacy-policy-encryption-filter-select"
              value={encryptionRequiredYn}
              onChange={(event) => setEncryptionRequiredYn(event.target.value)}
            >
              <option value="">전체</option>
              {yesNoValues.map((value) => (
                <option key={value} value={value}>
                  {value}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm font-medium text-dark">
            표시 건수
            <select
              className="mt-1 w-full rounded-md border border-ld px-3 py-2"
              data-testid="privacy-policy-page-size-select"
              value={size}
              onChange={(event) => {
                setPage(0);
                setSize(Number(event.target.value));
              }}
            >
              {pageSizes.map((value) => (
                <option key={value} value={value}>
                  {value}건
                </option>
              ))}
            </select>
          </label>
          <div className="flex items-end gap-2">
            <button
              className="inline-flex w-full items-center justify-center rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
              data-testid="privacy-policy-search-button"
              onClick={() => {
                setPage(0);
                void load();
              }}
              type="button"
            >
              <Search className="mr-2 h-4 w-4" aria-hidden="true" />
              조회
            </button>
            <button
              className="rounded-md border border-ld px-3 py-2 text-sm"
              data-testid="privacy-policy-refresh-button"
              onClick={() => void load()}
              type="button"
            >
              <RefreshCw className="h-4 w-4" aria-hidden="true" />
            </button>
          </div>
        </div>
      </section>

      {successMessage ? <SuccessState title={successMessage} /> : null}
      {error ? (
        <ErrorState title="개인정보 보호정책 오류" message={error} />
      ) : null}
      {loading ? <LoadingState title="정책 목록 조회 중" /> : null}

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1.35fr)_minmax(360px,0.65fr)]">
        <section className="rounded-md border border-ld bg-white p-5 shadow-md">
          <div className="mb-4 flex items-center justify-between">
            <div>
              <h2 className="text-lg font-semibold text-dark">
                개인정보 필드 정책 목록
              </h2>
              <p className="text-sm text-muted">
                총 {totalElements}건 · 기본 20건 표시
              </p>
            </div>
          </div>
          {!loading && policies.length === 0 ? (
            <EmptyState
              title="조회된 보호정책이 없습니다"
              message="검색조건을 변경하거나 필드 정책을 먼저 등록하세요."
            />
          ) : null}
          {policies.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-ld text-sm">
                <thead className="bg-lightsecondary text-left text-xs font-semibold uppercase tracking-wide text-muted">
                  <tr>
                    <th className="px-3 py-3">field_key</th>
                    <th className="px-3 py-3">등급</th>
                    <th className="px-3 py-3">암호화</th>
                    <th className="px-3 py-3">마스킹 규칙</th>
                    <th className="px-3 py-3">로그 제외</th>
                    <th className="px-3 py-3">수정일시</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-ld">
                  {policies.map((policy) => (
                    <tr
                      key={policy.policyId}
                      className="cursor-pointer hover:bg-lightprimary"
                      data-testid={`privacy-policy-row-${policy.policyId}`}
                      onClick={() => selectRow(policy)}
                    >
                      <td className="px-3 py-3 font-medium text-dark">
                        {policy.fieldKey}
                      </td>
                      <td className="px-3 py-3">{policy.privacyGrade}</td>
                      <td className="px-3 py-3">
                        {policy.encryptionRequiredYn}
                      </td>
                      <td className="px-3 py-3">{policy.maskingRule || "-"}</td>
                      <td className="px-3 py-3">{policy.logExclusionYn}</td>
                      <td className="px-3 py-3 text-muted">
                        {policy.updatedAt ?? "-"}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
          <div className="mt-4 flex items-center justify-between text-sm text-muted">
            <button
              className="rounded-md border border-ld px-3 py-1 disabled:opacity-40"
              data-testid="privacy-policy-prev-page-button"
              disabled={page === 0}
              onClick={() => setPage((current) => Math.max(0, current - 1))}
              type="button"
            >
              이전
            </button>
            <span>{page + 1} 페이지</span>
            <button
              className="rounded-md border border-ld px-3 py-1 disabled:opacity-40"
              data-testid="privacy-policy-next-page-button"
              disabled={(page + 1) * size >= totalElements}
              onClick={() => setPage((current) => current + 1)}
              type="button"
            >
              다음
            </button>
          </div>
        </section>

        <section
          className="rounded-md border border-ld bg-white p-5 shadow-md"
          data-testid="privacy-policy-editor-panel"
        >
          <h2 className="text-lg font-semibold text-dark">선택 정책 편집</h2>
          <p className="mt-1 text-sm text-muted">{selectedLabel}</p>
          <div className="mt-4 space-y-4">
            <label className="block text-sm font-medium text-dark">
              개인정보 필드 <span className="text-error">*</span>
              <input
                className="mt-1 w-full rounded-md border border-ld bg-lightsecondary px-3 py-2"
                data-testid="privacy-policy-field-key-input"
                readOnly
                value={form.fieldKey}
              />
              {fieldErrors.fieldKey ? (
                <FieldError message={fieldErrors.fieldKey} />
              ) : null}
            </label>
            <label className="block text-sm font-medium text-dark">
              등급 <span className="text-error">*</span>
              <select
                className="mt-1 w-full rounded-md border border-ld px-3 py-2"
                data-testid="privacy-policy-grade-select"
                value={form.privacyGrade}
                onChange={(event) =>
                  setForm({
                    ...form,
                    privacyGrade: event.target.value as PrivacyGrade,
                  })
                }
              >
                {privacyGrades.map((grade) => (
                  <option key={grade} value={grade}>
                    {grade}
                  </option>
                ))}
              </select>
              {fieldErrors.privacyGrade ? (
                <FieldError message={fieldErrors.privacyGrade} />
              ) : null}
            </label>
            <div className="grid gap-3 sm:grid-cols-2">
              <label className="block text-sm font-medium text-dark">
                암호화 여부 <span className="text-error">*</span>
                <select
                  className="mt-1 w-full rounded-md border border-ld px-3 py-2"
                  data-testid="privacy-policy-encryption-select"
                  value={form.encryptionRequiredYn}
                  onChange={(event) =>
                    setForm({
                      ...form,
                      encryptionRequiredYn: event.target.value as YesNo,
                    })
                  }
                >
                  {yesNoValues.map((value) => (
                    <option key={value} value={value}>
                      {value}
                    </option>
                  ))}
                </select>
              </label>
              <label className="block text-sm font-medium text-dark">
                일반 로그 제외 <span className="text-error">*</span>
                <select
                  className="mt-1 w-full rounded-md border border-ld px-3 py-2"
                  data-testid="privacy-policy-log-exclusion-select"
                  value={form.logExclusionYn}
                  onChange={(event) =>
                    setForm({
                      ...form,
                      logExclusionYn: event.target.value as YesNo,
                    })
                  }
                >
                  {yesNoValues.map((value) => (
                    <option key={value} value={value}>
                      {value}
                    </option>
                  ))}
                </select>
              </label>
            </div>
            <label className="block text-sm font-medium text-dark">
              마스킹 규칙
              <input
                className="mt-1 w-full rounded-md border border-ld px-3 py-2"
                data-testid="privacy-policy-masking-rule-input"
                value={form.maskingRule}
                onChange={(event) =>
                  setForm({ ...form, maskingRule: event.target.value })
                }
              />
            </label>
            <label className="block text-sm font-medium text-dark">
              변경 사유 <span className="text-error">*</span>
              <textarea
                className="mt-1 min-h-24 w-full rounded-md border border-ld px-3 py-2"
                data-testid="privacy-policy-change-reason-input"
                value={form.changeReason}
                onChange={(event) =>
                  setForm({ ...form, changeReason: event.target.value })
                }
              />
              {fieldErrors.changeReason ? (
                <FieldError message={fieldErrors.changeReason} />
              ) : null}
            </label>
            <div className="rounded-md bg-lightsecondary p-3 text-sm text-muted">
              실제 사용자 개인정보 값 조회·수정, 역할별 개인정보 조회·출력 권한
              설정은 이 화면 범위가 아닙니다.
            </div>
            <div className="flex gap-2">
              <button
                className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
                data-testid="privacy-policy-save-button"
                disabled={saving || !form.fieldKey}
                onClick={() => void save()}
                type="button"
              >
                저장 전 확인
              </button>
              <button
                className="rounded-md border border-ld px-4 py-2 text-sm"
                data-testid="privacy-policy-cancel-button"
                onClick={resetForm}
                type="button"
              >
                취소
              </button>
            </div>
          </div>
        </section>
      </div>
    </section>
  );
}

function FieldError({ message }: { message: string }) {
  return <p className="mt-1 text-xs text-error">{message}</p>;
}

function toFieldErrorMap(fields: ApiErrorField[]) {
  return fields.reduce<Record<string, string>>((accumulator, field) => {
    const key = field.field.includes(".")
      ? field.field.slice(field.field.lastIndexOf(".") + 1)
      : field.field;
    accumulator[key] = field.message;
    return accumulator;
  }, {});
}
