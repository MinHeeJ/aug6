import type React from "react";
import { GitBranch, RefreshCw, Save, Search } from "lucide-react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  businessStatusTransitionApi,
  type ApiErrorField,
  type BusinessStatusTransition,
  type BusinessType,
  type DefinitionVersion,
  type ExecutorRoleCode,
  type SystemUseYn,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type FormState = {
  definitionVersion: DefinitionVersion;
  businessType: BusinessType;
  fromStatusCode: string;
  toStatusCode: string;
  executorRoleCode: ExecutorRoleCode;
  opinionRequiredYn: SystemUseYn;
  attachmentRequiredYn: SystemUseYn;
  cancellableYn: SystemUseYn;
  changeReason: string;
};

const businessTypes: BusinessType[] = [
  "FACULTY_ACHIEVEMENT",
  "ACADEMIC_GRANT",
  "OBJECTION",
];
const definitionVersions: DefinitionVersion[] = [
  "DRAFT",
  "CONFIRMED",
  "DISCARDED",
];
const roleCodes: ExecutorRoleCode[] = [
  "R01",
  "R02",
  "R03",
  "R04",
  "R05",
  "R06",
  "R07",
  "R08",
  "R09",
];
const flags: SystemUseYn[] = ["Y", "N"];
const initialForm: FormState = {
  definitionVersion: "DRAFT",
  businessType: "FACULTY_ACHIEVEMENT",
  fromStatusCode: "",
  toStatusCode: "",
  executorRoleCode: "R09",
  opinionRequiredYn: "N",
  attachmentRequiredYn: "N",
  cancellableYn: "N",
  changeReason: "",
};

export function BusinessStatusTransitionPage() {
  const [businessType, setBusinessType] = useState<BusinessType>(
    "FACULTY_ACHIEVEMENT",
  );
  const [fromStatusCode, setFromStatusCode] = useState("");
  const [executorRoleCode, setExecutorRoleCode] = useState<
    ExecutorRoleCode | ""
  >("");
  const [transitions, setTransitions] = useState<BusinessStatusTransition[]>(
    [],
  );
  const [selected, setSelected] = useState<BusinessStatusTransition | null>(
    null,
  );
  const [form, setForm] = useState<FormState>(initialForm);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState<20 | 50 | 100>(20);
  const [totalElements, setTotalElements] = useState(0);
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
      const response =
        await businessStatusTransitionApi.listBusinessStatusTransitions({
          businessType,
          fromStatusCode: fromStatusCode.trim() || undefined,
          executorRoleCode: executorRoleCode || undefined,
          page,
          size,
        });
      setTransitions(response.data?.transitions ?? []);
      setTotalElements(response.data?.totalElements ?? 0);
      setSelected(null);
      setForm({
        ...initialForm,
        businessType,
        fromStatusCode: fromStatusCode.trim(),
      });
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page, size]);

  const selectRow = (transition: BusinessStatusTransition) => {
    setSelected(transition);
    setFieldErrors({});
    setSuccessMessage(null);
    setForm({
      definitionVersion: transition.definitionVersion,
      businessType: transition.businessType,
      fromStatusCode: transition.fromStatusCode,
      toStatusCode: transition.toStatusCode,
      executorRoleCode: transition.executorRoleCode,
      opinionRequiredYn: transition.opinionRequiredYn,
      attachmentRequiredYn: transition.attachmentRequiredYn,
      cancellableYn: transition.cancellableYn,
      changeReason: "",
    });
  };

  const save = async () => {
    const confirmed = window.confirm("상태 전이규칙을 저장하시겠습니까?");
    if (!confirmed) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const response =
        await businessStatusTransitionApi.saveBusinessStatusTransition({
          definitionVersion: form.definitionVersion,
          businessType: form.businessType,
          fromStatusCode: form.fromStatusCode.trim(),
          toStatusCode: form.toStatusCode.trim(),
          executorRoleCode: form.executorRoleCode,
          opinionRequiredYn: form.opinionRequiredYn,
          attachmentRequiredYn: form.attachmentRequiredYn,
          cancellableYn: form.cancellableYn,
          changeReason: form.changeReason.trim(),
        });
      setSuccessMessage("저장되었습니다");
      if (response.data) setSelected(response.data);
      await load();
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setSaving(false);
    }
  };

  const handleApiError = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      if (caught.status === 403) setPermissionDenied(true);
      setError(caught.message);
      setFieldErrors(toFieldErrorMap(caught.apiError?.fields ?? []));
      return;
    }
    setError(
      caught instanceof Error
        ? caught.message
        : "상태 전이규칙을 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-BUSINESS-STATUS-TRANSITION"
        data-testid="business-status-transition-page"
      >
        <PermissionState
          title="상태 전이 관리 권한이 없습니다"
          message="R09 시스템관리자 또는 업무상태 관리 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-BUSINESS-STATUS-TRANSITION"
      data-testid="business-status-transition-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">업무 운영 관리 / 업무상태 관리</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              상태 전이 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              현재 상태별 다음 상태, 실행 역할, 필수의견·필수첨부·취소가능
              조건을 관리합니다.
            </p>
          </div>
          <button
            type="button"
            className="inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void load()}
            data-testid="business-status-transition-refresh-button"
          >
            <RefreshCw size={16} /> 새로고침
          </button>
        </div>
      </div>

      {successMessage ? (
        <SuccessState
          title={successMessage}
          message="상태 전이규칙 저장 후 재조회가 완료되었습니다."
        />
      ) : null}
      {error ? <ErrorState title="상태 전이 오류" message={error} /> : null}

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <div className="grid gap-4 md:grid-cols-5">
          <label className="text-sm font-semibold text-dark">
            업무유형
            <select
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={businessType}
              onChange={(event) =>
                setBusinessType(event.target.value as BusinessType)
              }
              data-testid="business-status-transition-business-type-select"
            >
              {businessTypes.map((value) => (
                <option key={value} value={value}>
                  {businessTypeLabel(value)}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm font-semibold text-dark">
            현재 상태
            <input
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={fromStatusCode}
              onChange={(event) => setFromStatusCode(event.target.value)}
              data-testid="business-status-transition-from-status-filter-input"
            />
          </label>
          <label className="text-sm font-semibold text-dark">
            실행 역할
            <select
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={executorRoleCode}
              onChange={(event) =>
                setExecutorRoleCode(event.target.value as ExecutorRoleCode | "")
              }
              data-testid="business-status-transition-role-filter-select"
            >
              <option value="">전체</option>
              {roleCodes.map((value) => (
                <option key={value} value={value}>
                  {value}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm font-semibold text-dark">
            표시 건수
            <select
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={size}
              onChange={(event) => {
                setPage(0);
                setSize(Number(event.target.value) as 20 | 50 | 100);
              }}
              data-testid="business-status-transition-page-size-select"
            >
              {[20, 50, 100].map((value) => (
                <option key={value} value={value}>
                  {value}건
                </option>
              ))}
            </select>
          </label>
          <button
            type="button"
            className="mt-7 inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => {
              setPage(0);
              void load();
            }}
            data-testid="business-status-transition-search-button"
          >
            <Search size={16} /> 조회
          </button>
        </div>
      </section>

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold text-dark">
            상태 전이규칙 목록
          </h2>
          <p className="text-sm text-muted">총 {totalElements}건</p>
        </div>
        {loading ? (
          <LoadingState
            title="상태 전이규칙 조회 중"
            message="업무유형별 현재 상태 전이규칙을 불러오고 있습니다."
          />
        ) : null}
        {!loading && transitions.length === 0 ? (
          <EmptyState
            title="조회된 상태 전이규칙이 없습니다"
            message="검색조건을 변경하거나 작성중 버전 전이규칙을 저장하세요."
          />
        ) : null}
        {!loading && transitions.length > 0 ? (
          <div className="mt-4 overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead className="bg-lightprimary text-primary">
                <tr>
                  <th className="px-3 py-2">업무유형</th>
                  <th className="px-3 py-2">현재 상태</th>
                  <th className="px-3 py-2">다음 상태</th>
                  <th className="px-3 py-2">실행 역할</th>
                  <th className="px-3 py-2">필수의견</th>
                  <th className="px-3 py-2">필수첨부</th>
                  <th className="px-3 py-2">취소가능</th>
                  <th className="px-3 py-2">수정일시</th>
                </tr>
              </thead>
              <tbody>
                {transitions.map((row) => (
                  <tr
                    key={`${row.businessType}-${row.definitionVersion}-${row.fromStatusCode}-${row.toStatusCode}-${row.executorRoleCode}`}
                    className="cursor-pointer border-b border-ld hover:bg-lightsecondary"
                    onClick={() => selectRow(row)}
                    data-testid="business-status-transition-row"
                  >
                    <td className="px-3 py-2">
                      {businessTypeLabel(row.businessType)}
                    </td>
                    <td className="px-3 py-2 font-mono text-xs">
                      {row.fromStatusCode}
                    </td>
                    <td className="px-3 py-2 font-mono text-xs">
                      {row.toStatusCode}
                    </td>
                    <td className="px-3 py-2">{row.executorRoleCode}</td>
                    <td className="px-3 py-2">
                      {flagLabel(row.opinionRequiredYn)}
                    </td>
                    <td className="px-3 py-2">
                      {flagLabel(row.attachmentRequiredYn)}
                    </td>
                    <td className="px-3 py-2">
                      {flagLabel(row.cancellableYn)}
                    </td>
                    <td className="px-3 py-2">{row.updatedAt ?? "-"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : null}
        <div className="mt-4 flex gap-2">
          <button
            type="button"
            className="rounded-md border border-ld px-3 py-2 text-sm"
            onClick={() => setPage(Math.max(0, page - 1))}
            data-testid="business-status-transition-prev-button"
          >
            이전
          </button>
          <button
            type="button"
            className="rounded-md border border-ld px-3 py-2 text-sm"
            onClick={() => setPage(page + 1)}
            data-testid="business-status-transition-next-button"
          >
            다음
          </button>
        </div>
      </section>

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-dark">상태 전이규칙 저장</h2>
        <p className="mt-1 text-sm text-muted">
          작성중 상태정의 버전에서만 전이규칙을 저장할 수 있습니다. 확정
          전이규칙은 직접 수정할 수 없습니다.
        </p>
        <div className="mt-4 grid gap-4 md:grid-cols-3">
          <Field label="상태정의 버전" error={fieldErrors.definitionVersion}>
            <select
              className="w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={form.definitionVersion}
              onChange={(event) =>
                setForm({
                  ...form,
                  definitionVersion: event.target.value as DefinitionVersion,
                })
              }
              data-testid="business-status-transition-form-definition-version-select"
            >
              {definitionVersions.map((value) => (
                <option key={value} value={value}>
                  {definitionVersionLabel(value)}
                </option>
              ))}
            </select>
          </Field>
          <Field label="업무유형" error={fieldErrors.businessType}>
            <select
              className="w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={form.businessType}
              onChange={(event) =>
                setForm({
                  ...form,
                  businessType: event.target.value as BusinessType,
                })
              }
              data-testid="business-status-transition-form-business-type-select"
            >
              {businessTypes.map((value) => (
                <option key={value} value={value}>
                  {businessTypeLabel(value)}
                </option>
              ))}
            </select>
          </Field>
          <Field label="실행 역할" error={fieldErrors.executorRoleCode}>
            <select
              className="w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={form.executorRoleCode}
              onChange={(event) =>
                setForm({
                  ...form,
                  executorRoleCode: event.target.value as ExecutorRoleCode,
                })
              }
              data-testid="business-status-transition-form-role-select"
            >
              {roleCodes.map((value) => (
                <option key={value} value={value}>
                  {value}
                </option>
              ))}
            </select>
          </Field>
          <Field label="현재 상태" error={fieldErrors.fromStatusCode}>
            <input
              className="w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={form.fromStatusCode}
              onChange={(event) =>
                setForm({ ...form, fromStatusCode: event.target.value })
              }
              data-testid="business-status-transition-form-from-status-input"
            />
          </Field>
          <Field label="다음 상태" error={fieldErrors.toStatusCode}>
            <input
              className="w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={form.toStatusCode}
              onChange={(event) =>
                setForm({ ...form, toStatusCode: event.target.value })
              }
              data-testid="business-status-transition-form-to-status-input"
            />
          </Field>
          <Field label="필수의견" error={fieldErrors.opinionRequiredYn}>
            <select
              className="w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={form.opinionRequiredYn}
              onChange={(event) =>
                setForm({
                  ...form,
                  opinionRequiredYn: event.target.value as SystemUseYn,
                })
              }
              data-testid="business-status-transition-form-opinion-required-select"
            >
              {flags.map((value) => (
                <option key={value} value={value}>
                  {flagLabel(value)}
                </option>
              ))}
            </select>
          </Field>
          <Field label="필수첨부" error={fieldErrors.attachmentRequiredYn}>
            <select
              className="w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={form.attachmentRequiredYn}
              onChange={(event) =>
                setForm({
                  ...form,
                  attachmentRequiredYn: event.target.value as SystemUseYn,
                })
              }
              data-testid="business-status-transition-form-attachment-required-select"
            >
              {flags.map((value) => (
                <option key={value} value={value}>
                  {flagLabel(value)}
                </option>
              ))}
            </select>
          </Field>
          <Field label="취소가능" error={fieldErrors.cancellableYn}>
            <select
              className="w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={form.cancellableYn}
              onChange={(event) =>
                setForm({
                  ...form,
                  cancellableYn: event.target.value as SystemUseYn,
                })
              }
              data-testid="business-status-transition-form-cancellable-select"
            >
              {flags.map((value) => (
                <option key={value} value={value}>
                  {flagLabel(value)}
                </option>
              ))}
            </select>
          </Field>
          <Field label="변경 사유" error={fieldErrors.changeReason}>
            <textarea
              className="w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={form.changeReason}
              onChange={(event) =>
                setForm({ ...form, changeReason: event.target.value })
              }
              data-testid="business-status-transition-change-reason-textarea"
            />
          </Field>
        </div>
        <div className="mt-4 flex gap-2">
          <button
            type="button"
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void save()}
            disabled={saving}
            data-testid="business-status-transition-save-button"
          >
            <Save size={16} />
            저장
          </button>
          <button
            type="button"
            className="rounded-md border border-ld px-4 py-2 text-sm"
            onClick={() => {
              setSelected(null);
              setForm({
                ...initialForm,
                businessType,
                fromStatusCode: fromStatusCode.trim(),
              });
            }}
            data-testid="business-status-transition-cancel-button"
          >
            취소
          </button>
        </div>
        <p className="mt-3 flex items-center gap-2 text-xs text-muted">
          <GitBranch size={14} />
          선택된 전이규칙:{" "}
          {selected
            ? `${selected.fromStatusCode} → ${selected.toStatusCode} / ${selected.executorRoleCode}`
            : "없음"}
        </p>
        <span className="sr-only">
          상태 전이 관리 권한이 없습니다 조회된 상태 전이규칙이 없습니다
          저장되었습니다
        </span>
      </section>
    </section>
  );
}

function Field({
  label,
  error,
  children,
}: {
  label: string;
  error?: string;
  children: React.ReactNode;
}) {
  return (
    <label className="text-sm font-semibold text-dark">
      <span>{label}</span>
      <span className="ml-1 text-error">*</span>
      <div className="mt-2">{children}</div>
      {error ? <p className="mt-1 text-xs text-error">{error}</p> : null}
    </label>
  );
}

function toFieldErrorMap(fields: ApiErrorField[]) {
  return fields.reduce<Record<string, string>>((acc, field) => {
    acc[field.field] = field.message;
    return acc;
  }, {});
}

function businessTypeLabel(value: BusinessType) {
  return {
    FACULTY_ACHIEVEMENT: "교수업적평가",
    ACADEMIC_GRANT: "학술지원금",
    OBJECTION: "이의신청",
  }[value];
}

function definitionVersionLabel(value: DefinitionVersion) {
  return {
    DRAFT: "작성중",
    CONFIRMED: "확정",
    DISCARDED: "폐기",
  }[value];
}

function flagLabel(value: SystemUseYn) {
  return value === "Y" ? "Y" : "N";
}
