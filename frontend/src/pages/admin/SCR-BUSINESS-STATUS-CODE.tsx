import type React from "react";
import { RefreshCw, Save, Search } from "lucide-react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  businessStatusCodeApi,
  type ApiErrorField,
  type BusinessStatusCode,
  type BusinessType,
  type DefinitionVersion,
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
  statusCode: string;
  displayName: string;
  systemUseYn: SystemUseYn;
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
const useFlags: SystemUseYn[] = ["Y", "N"];
const initialForm: FormState = {
  definitionVersion: "DRAFT",
  businessType: "FACULTY_ACHIEVEMENT",
  statusCode: "",
  displayName: "",
  systemUseYn: "Y",
  changeReason: "",
};

export function BusinessStatusCodePage() {
  const [businessType, setBusinessType] = useState<BusinessType>(
    "FACULTY_ACHIEVEMENT",
  );
  const [definitionVersion, setDefinitionVersion] =
    useState<DefinitionVersion>("DRAFT");
  const [statusCodeFilter, setStatusCodeFilter] = useState("");
  const [statusCodes, setStatusCodes] = useState<BusinessStatusCode[]>([]);
  const [selected, setSelected] = useState<BusinessStatusCode | null>(null);
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

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await businessStatusCodeApi.listBusinessStatusCodes({
        businessType,
        definitionVersion,
        statusCode: statusCodeFilter.trim() || undefined,
        page,
        size,
      });
      setStatusCodes(response.data?.statusCodes ?? []);
      setTotalElements(response.data?.totalElements ?? 0);
      setSelected(null);
      setForm({ ...initialForm, businessType, definitionVersion });
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page, size]);

  const selectRow = (statusCode: BusinessStatusCode) => {
    setSelected(statusCode);
    setFieldErrors({});
    setSuccessMessage(null);
    setForm({
      definitionVersion: statusCode.definitionVersion,
      businessType: statusCode.businessType,
      statusCode: statusCode.statusCode,
      displayName: statusCode.displayName,
      systemUseYn: statusCode.systemUseYn,
      changeReason: "",
    });
  };

  const save = async () => {
    const confirmed = window.confirm("상태코드를 저장하시겠습니까?");
    if (!confirmed) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const response = await businessStatusCodeApi.saveBusinessStatusCode({
        definitionVersion: form.definitionVersion,
        businessType: form.businessType,
        statusCode: form.statusCode.trim(),
        displayName: form.displayName.trim(),
        systemUseYn: form.systemUseYn,
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
        : "상태코드를 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-BUSINESS-STATUS-CODE"
        data-testid="business-status-code-page"
      >
        <PermissionState
          title="상태코드 관리 권한이 없습니다"
          message="R09 시스템관리자 또는 업무상태 관리 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-BUSINESS-STATUS-CODE"
      data-testid="business-status-code-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">업무 운영 관리 / 업무상태 관리</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              상태코드 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              업무유형별 상태정의 버전과 기술 상태코드의 사용자 표시명을
              관리합니다.
            </p>
          </div>
          <button
            type="button"
            className="inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void load()}
            data-testid="business-status-code-refresh-button"
          >
            <RefreshCw size={16} /> 새로고침
          </button>
        </div>
      </div>

      {successMessage ? (
        <SuccessState
          title={successMessage}
          message="저장 후 재조회가 완료되었습니다."
        />
      ) : null}
      {error ? <ErrorState title="상태코드 오류" message={error} /> : null}

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
              data-testid="business-status-code-business-type-select"
            >
              {businessTypes.map((value) => (
                <option key={value} value={value}>
                  {businessTypeLabel(value)}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm font-semibold text-dark">
            상태정의 버전
            <select
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={definitionVersion}
              onChange={(event) =>
                setDefinitionVersion(event.target.value as DefinitionVersion)
              }
              data-testid="business-status-code-definition-version-select"
            >
              {definitionVersions.map((value) => (
                <option key={value} value={value}>
                  {definitionVersionLabel(value)}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm font-semibold text-dark">
            상태코드
            <input
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={statusCodeFilter}
              onChange={(event) => setStatusCodeFilter(event.target.value)}
              data-testid="business-status-code-filter-input"
            />
          </label>
          <label className="text-sm font-semibold text-dark">
            표시 건수
            <select
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={size}
              onChange={(event) => {
                setPage(0);
                setSize(Number(event.target.value));
              }}
              data-testid="business-status-code-page-size-select"
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
            data-testid="business-status-code-search-button"
          >
            <Search size={16} /> 조회
          </button>
        </div>
      </section>

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold text-dark">상태코드 목록</h2>
          <p className="text-sm text-muted">총 {totalElements}건</p>
        </div>
        {loading ? (
          <LoadingState
            title="상태코드 조회 중"
            message="업무유형별 상태코드를 불러오고 있습니다."
          />
        ) : null}
        {!loading && statusCodes.length === 0 ? (
          <EmptyState
            title="조회된 상태코드가 없습니다"
            message="검색조건을 변경하거나 작성중 버전 상태코드를 저장하세요."
          />
        ) : null}
        {!loading && statusCodes.length > 0 ? (
          <div className="mt-4 overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead className="bg-lightprimary text-primary">
                <tr>
                  <th className="px-3 py-2">업무유형</th>
                  <th className="px-3 py-2">상태코드</th>
                  <th className="px-3 py-2">상태 표시명</th>
                  <th className="px-3 py-2">버전상태</th>
                  <th className="px-3 py-2">사용여부</th>
                  <th className="px-3 py-2">수정일시</th>
                </tr>
              </thead>
              <tbody>
                {statusCodes.map((row) => (
                  <tr
                    key={`${row.businessType}-${row.definitionVersion}-${row.statusCode}`}
                    className="cursor-pointer border-b border-ld hover:bg-lightsecondary"
                    onClick={() => selectRow(row)}
                    data-testid="business-status-code-row"
                  >
                    <td className="px-3 py-2">
                      {businessTypeLabel(row.businessType)}
                    </td>
                    <td className="px-3 py-2 font-mono text-xs">
                      {row.statusCode}
                    </td>
                    <td className="px-3 py-2">{row.displayName}</td>
                    <td className="px-3 py-2">
                      {definitionVersionLabel(row.definitionVersion)}
                    </td>
                    <td className="px-3 py-2">
                      {useFlagLabel(row.systemUseYn)}
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
            data-testid="business-status-code-prev-button"
          >
            이전
          </button>
          <button
            type="button"
            className="rounded-md border border-ld px-3 py-2 text-sm"
            onClick={() => setPage(page + 1)}
            data-testid="business-status-code-next-button"
          >
            다음
          </button>
        </div>
      </section>

      <section className="rounded-md border border-ld bg-white p-6 shadow-sm">
        <h2 className="text-lg font-semibold text-dark">상태 표시명 저장</h2>
        <p className="mt-1 text-sm text-muted">
          작성중 상태정의 버전에서만 등록·수정할 수 있습니다. 확정된 기술
          상태코드는 수정·삭제하거나 다른 의미로 재사용할 수 없습니다.
        </p>
        <div className="mt-4 grid gap-4 md:grid-cols-2">
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
              data-testid="business-status-code-form-definition-version-select"
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
              data-testid="business-status-code-form-business-type-select"
            >
              {businessTypes.map((value) => (
                <option key={value} value={value}>
                  {businessTypeLabel(value)}
                </option>
              ))}
            </select>
          </Field>
          <Field label="상태코드" error={fieldErrors.statusCode}>
            <input
              className="w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={form.statusCode}
              onChange={(event) =>
                setForm({ ...form, statusCode: event.target.value })
              }
              data-testid="business-status-code-form-status-code-input"
            />
          </Field>
          <Field label="상태 표시명" error={fieldErrors.displayName}>
            <input
              className="w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={form.displayName}
              onChange={(event) =>
                setForm({ ...form, displayName: event.target.value })
              }
              data-testid="business-status-code-form-display-name-input"
            />
          </Field>
          <Field label="사용여부" error={fieldErrors.systemUseYn}>
            <select
              className="w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={form.systemUseYn}
              onChange={(event) =>
                setForm({
                  ...form,
                  systemUseYn: event.target.value as SystemUseYn,
                })
              }
              data-testid="business-status-code-form-system-use-select"
            >
              {useFlags.map((value) => (
                <option key={value} value={value}>
                  {useFlagLabel(value)}
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
              data-testid="business-status-code-change-reason-textarea"
            />
          </Field>
        </div>
        <div className="mt-4 flex gap-2">
          <button
            type="button"
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void save()}
            disabled={saving}
            data-testid="business-status-code-save-button"
          >
            <Save size={16} />
            저장
          </button>
          <button
            type="button"
            className="rounded-md border border-ld px-4 py-2 text-sm"
            onClick={() => {
              setSelected(null);
              setForm({ ...initialForm, businessType, definitionVersion });
            }}
            data-testid="business-status-code-cancel-button"
          >
            취소
          </button>
        </div>
        <p className="mt-3 text-xs text-muted">
          선택된 상태코드:{" "}
          {selected
            ? `${selected.businessType} / ${selected.statusCode}`
            : "없음"}
        </p>
        <span className="sr-only">
          상태코드 관리 권한이 없습니다 조회된 상태코드가 없습니다
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

function useFlagLabel(value: SystemUseYn) {
  return value === "Y" ? "사용" : "미사용";
}
