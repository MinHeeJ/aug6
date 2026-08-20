import type React from "react";
import { CalendarDays, RefreshCw, Save } from "lucide-react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  evaluationYearApi,
  type ApiErrorField,
  type EvaluationYearPreparation,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type Yn = "Y" | "N";

type FormState = {
  currentEvaluationYear: string;
  defaultSearchYear: string;
  changeReason: string;
  targetYear: string;
  copyRequestedYn: Yn;
  resetRequestedYn: Yn;
  preparationReason: string;
};

const emptyForm: FormState = {
  currentEvaluationYear: "",
  defaultSearchYear: "",
  changeReason: "",
  targetYear: "",
  copyRequestedYn: "N",
  resetRequestedYn: "N",
  preparationReason: "",
};

export function EvaluationYearManagementPage() {
  const [preparations, setPreparations] = useState<EvaluationYearPreparation[]>(
    [],
  );
  const [selected, setSelected] = useState<EvaluationYearPreparation | null>(
    null,
  );
  const [form, setForm] = useState<FormState>(emptyForm);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await evaluationYearApi.getEvaluationYearSettings();
      const data = response.data;
      const rows = data?.preparations ?? [];
      setPreparations(rows);
      setSelected(null);
      setForm({
        ...emptyForm,
        currentEvaluationYear: data?.currentEvaluationYear
          ? String(data.currentEvaluationYear)
          : "",
        defaultSearchYear: data?.defaultSearchYear
          ? String(data.defaultSearchYear)
          : "",
        changeReason: "",
      });
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, []);

  const selectPreparation = (preparation: EvaluationYearPreparation) => {
    setSelected(preparation);
    setFieldErrors({});
    setSuccessMessage(null);
    setForm((value) => ({
      ...value,
      targetYear: String(preparation.targetYear),
      copyRequestedYn: preparation.copyRequestedYn,
      resetRequestedYn: preparation.resetRequestedYn,
      preparationReason: "",
    }));
  };

  const resetPreparation = () => {
    setSelected(null);
    setFieldErrors({});
    setForm((value) => ({
      ...value,
      targetYear: "",
      copyRequestedYn: "N",
      resetRequestedYn: "N",
      preparationReason: "",
    }));
  };

  const save = async () => {
    const localErrors = validateForm(form);
    setFieldErrors(localErrors);
    if (Object.keys(localErrors).length > 0) return;
    const confirmed = window.confirm(
      "기준연도 설정과 대상연도 준비 상태를 저장합니까?",
    );
    if (!confirmed) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      await evaluationYearApi.saveEvaluationYearSettings({
        currentEvaluationYear: Number(form.currentEvaluationYear),
        defaultSearchYear: Number(form.defaultSearchYear),
        changeReason: form.changeReason,
        preparations: [
          {
            targetYear: Number(form.targetYear),
            copyRequestedYn: form.copyRequestedYn,
            resetRequestedYn: form.resetRequestedYn,
            changeReason: form.preparationReason,
          },
        ],
      });
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
        return;
      }
      setError(caught.message);
      setFieldErrors(toFieldErrorMap(caught.apiError?.fields ?? []));
      return;
    }
    setError(
      caught instanceof Error
        ? caught.message
        : "기준연도 설정을 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section data-screen-id="SCR-EVALUATION-YEAR-MGMT">
        <PermissionState
          title="기준연도 관리 권한이 없습니다"
          message="R09 시스템관리자 또는 해당 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-EVALUATION-YEAR-MGMT"
      data-testid="evaluation-year-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">시스템 관리 / 환경설정 관리</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              기준연도 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              현재 평가연도와 사용자 화면의 기본 조회연도를 관리하고 대상연도별
              기준정보 복사·초기화 준비 상태를 저장합니다. 기존 평가자료를
              삭제하거나 변경하지 않습니다.
            </p>
          </div>
          <button
            className="inline-flex h-10 items-center gap-2 rounded-md bg-lightprimary px-4 py-2 text-sm font-semibold text-primary transition-colors hover:bg-primary hover:text-white"
            onClick={() => void load()}
            type="button"
            data-testid="evaluation-year-refresh-button"
          >
            <RefreshCw size={16} /> 다시 조회
          </button>
        </div>
      </div>

      <div className="sr-only">
        기준연도 설정을 불러오는 중입니다 조회된 대상연도 준비 상태가 없습니다
        기준연도 관리 권한이 없습니다 저장되었습니다 복사와 초기화는 동시에
        요청할 수 없습니다
      </div>
      {error ? (
        <ErrorState title="기준연도 설정 처리 오류" message={error} />
      ) : null}
      {successMessage ? <SuccessState title={successMessage} /> : null}

      <div className="grid grid-cols-12 gap-6">
        <section
          className="col-span-12 rounded-md bg-white p-6 shadow-md xl:col-span-5 dark:bg-darkgray"
          data-testid="evaluation-year-settings-panel"
        >
          <div className="mb-4 flex items-center gap-2 text-dark dark:text-white">
            <CalendarDays size={18} />
            <h2 className="text-lg font-semibold">연도 설정</h2>
          </div>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-1">
            <YearInput
              label="현재 평가연도"
              value={form.currentEvaluationYear}
              onChange={(value) =>
                setForm((state) => ({ ...state, currentEvaluationYear: value }))
              }
              error={fieldErrors.currentEvaluationYear}
              testId="evaluation-year-current-input"
            />
            <YearInput
              label="기본 조회연도"
              value={form.defaultSearchYear}
              onChange={(value) =>
                setForm((state) => ({ ...state, defaultSearchYear: value }))
              }
              error={fieldErrors.defaultSearchYear}
              testId="evaluation-year-default-input"
            />
          </div>
          <label className="mt-4 block text-sm font-medium text-link">
            변경 사유
            <textarea
              className="mt-2 min-h-20 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={form.changeReason}
              onChange={(event) =>
                setForm((value) => ({
                  ...value,
                  changeReason: event.target.value,
                }))
              }
              data-testid="evaluation-year-change-reason-textarea"
            />
            {fieldErrors.changeReason ? (
              <p className="mt-1 text-xs text-error">
                {fieldErrors.changeReason}
              </p>
            ) : null}
          </label>
        </section>

        <section
          className="col-span-12 rounded-md bg-white p-6 shadow-md xl:col-span-7 dark:bg-darkgray"
          data-testid="evaluation-year-preparation-list-panel"
        >
          <div className="mb-4 flex items-center justify-between gap-2">
            <h2 className="text-lg font-semibold text-dark dark:text-white">
              대상연도 준비 상태
            </h2>
            <span className="text-sm text-muted">
              총 {preparations.length}건
            </span>
          </div>
          {loading ? (
            <LoadingState title="기준연도 설정을 불러오는 중입니다" />
          ) : preparations.length === 0 ? (
            <EmptyState title="조회된 대상연도 준비 상태가 없습니다" />
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-ld text-sm">
                <thead className="bg-lightgray text-left text-xs uppercase text-muted">
                  <tr>
                    <th className="px-4 py-3">대상연도</th>
                    <th className="px-4 py-3">복사 요청</th>
                    <th className="px-4 py-3">초기화 요청</th>
                    <th className="px-4 py-3">최종 변경</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-ld">
                  {preparations.map((preparation) => (
                    <tr
                      key={preparation.targetYear}
                      className={`cursor-pointer hover:bg-lightprimary/40 ${selected?.targetYear === preparation.targetYear ? "bg-lightprimary/70" : ""}`}
                      onClick={() => selectPreparation(preparation)}
                      data-testid={`evaluation-year-row-${preparation.targetYear}`}
                    >
                      <td className="px-4 py-3 font-medium text-dark">
                        {preparation.targetYear}
                      </td>
                      <td className="px-4 py-3">
                        {preparation.copyRequestedYn === "Y"
                          ? "요청"
                          : "미요청"}
                      </td>
                      <td className="px-4 py-3">
                        {preparation.resetRequestedYn === "Y"
                          ? "요청"
                          : "미요청"}
                      </td>
                      <td className="px-4 py-3 text-muted">
                        {formatDateTime(preparation.updatedAt)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </div>

      <section
        className="rounded-md bg-white p-6 shadow-md dark:bg-darkgray"
        data-testid="evaluation-year-preparation-edit-panel"
      >
        <div className="mb-4 flex items-center gap-2 text-dark dark:text-white">
          <Save size={18} />
          <h2 className="text-lg font-semibold">준비 상태 편집</h2>
        </div>
        <div className="grid gap-4 md:grid-cols-3">
          <YearInput
            label="대상연도"
            value={form.targetYear}
            onChange={(value) =>
              setForm((state) => ({ ...state, targetYear: value }))
            }
            error={fieldErrors.targetYear}
            testId="evaluation-year-target-input"
          />
          <YnSelect
            label="기준정보 복사"
            value={form.copyRequestedYn}
            onChange={(value) =>
              setForm((state) => ({ ...state, copyRequestedYn: value }))
            }
            testId="evaluation-year-copy-select"
          />
          <YnSelect
            label="초기화"
            value={form.resetRequestedYn}
            onChange={(value) =>
              setForm((state) => ({ ...state, resetRequestedYn: value }))
            }
            testId="evaluation-year-reset-select"
          />
        </div>
        {fieldErrors.copyRequestedYn ? (
          <p className="mt-2 text-xs text-error">
            {fieldErrors.copyRequestedYn}
          </p>
        ) : null}
        {fieldErrors.resetRequestedYn ? (
          <p className="mt-2 text-xs text-error">
            {fieldErrors.resetRequestedYn}
          </p>
        ) : null}
        <label className="mt-4 block text-sm font-medium text-link">
          준비 상태 변경 사유
          <textarea
            className="mt-2 min-h-20 w-full rounded-md border border-ld px-3 py-2 text-sm"
            value={form.preparationReason}
            onChange={(event) =>
              setForm((value) => ({
                ...value,
                preparationReason: event.target.value,
              }))
            }
            data-testid="evaluation-year-preparation-reason-textarea"
          />
          {fieldErrors.preparationReason ? (
            <p className="mt-1 text-xs text-error">
              {fieldErrors.preparationReason}
            </p>
          ) : null}
        </label>
        <p className="mt-4 rounded-md bg-lightsecondary px-4 py-3 text-sm text-muted">
          복사와 초기화는 동시에 요청할 수 없습니다. 기준연도 관리는 준비 상태만
          저장하며 기존 평가자료를 삭제하거나 변경하지 않습니다.
        </p>
        <div className="mt-4 flex gap-2">
          <button
            className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
            disabled={saving}
            onClick={() => void save()}
            type="button"
            data-testid="evaluation-year-save-button"
          >
            {saving ? "저장 중" : "저장"}
          </button>
          <button
            className="rounded-md border border-ld px-4 py-2 text-sm font-semibold text-link"
            onClick={resetPreparation}
            type="button"
            data-testid="evaluation-year-cancel-button"
          >
            취소
          </button>
        </div>
      </section>
    </section>
  );
}

function YearInput({
  label,
  value,
  onChange,
  error,
  testId,
  optional = false,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  error?: string;
  testId: string;
  optional?: boolean;
}) {
  return (
    <label className="block text-sm font-medium text-link">
      {label}
      {optional ? "" : <span className="ms-1 text-error">*</span>}
      <input
        className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
        inputMode="numeric"
        pattern="[0-9]*"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        data-testid={testId}
      />
      {error ? <p className="mt-1 text-xs text-error">{error}</p> : null}
    </label>
  );
}

function YnSelect({
  label,
  value,
  onChange,
  testId,
}: {
  label: string;
  value: Yn;
  onChange: (value: Yn) => void;
  testId: string;
}) {
  return (
    <label className="block text-sm font-medium text-link">
      {label}
      <select
        className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
        value={value}
        onChange={(event) => onChange(event.target.value as Yn)}
        data-testid={testId}
      >
        <option value="N">미요청</option>
        <option value="Y">요청</option>
      </select>
    </label>
  );
}

function validateForm(form: FormState) {
  const errors: Record<string, string> = {};
  if (!isFourDigitYear(form.currentEvaluationYear))
    errors.currentEvaluationYear = "현재 평가연도를 4자리로 입력하세요.";
  if (!isFourDigitYear(form.defaultSearchYear))
    errors.defaultSearchYear = "기본 조회연도를 4자리로 입력하세요.";
  if (!form.changeReason.trim())
    errors.changeReason = "변경 사유를 입력하세요.";
  if (!isFourDigitYear(form.targetYear))
    errors.targetYear = "대상연도를 4자리로 입력하세요.";
  if (form.copyRequestedYn === "Y" && form.resetRequestedYn === "Y")
    errors.resetRequestedYn = "복사와 초기화는 동시에 요청할 수 없습니다.";
  if (!form.preparationReason.trim())
    errors.preparationReason = "준비 상태 변경 사유를 입력하세요.";
  return errors;
}

function isFourDigitYear(value: string) {
  return /^\d{4}$/.test(value);
}

function toFieldErrorMap(fields: ApiErrorField[]) {
  return fields.reduce<Record<string, string>>((accumulator, field) => {
    const normalized = field.field
      .replace(/^preparations\[\d+\]\./, "")
      .replace(
        "changeReason",
        field.field.startsWith("preparations")
          ? "preparationReason"
          : "changeReason",
      );
    accumulator[normalized] = field.message;
    return accumulator;
  }, {});
}

function formatDateTime(value: string | undefined | null) {
  if (!value) return "-";
  return value.replace("T", " ").slice(0, 16);
}
