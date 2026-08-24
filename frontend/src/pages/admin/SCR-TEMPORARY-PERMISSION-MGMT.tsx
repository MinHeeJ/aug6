import type React from "react";
import { RefreshCw, Search, TimerReset } from "lucide-react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  temporaryPermissionApi,
  type ApiErrorField,
  type FunctionType,
  type TemporaryPermission,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type FormState = {
  userId: string;
  workDataRef: string;
  functionType: FunctionType;
  validStartAt: string;
  validEndAt: string;
  changeReason: string;
};

const functionTypes: FunctionType[] = [
  "READ",
  "CREATE",
  "UPDATE",
  "DELETE",
  "EXECUTE",
];
const initialForm: FormState = {
  userId: "",
  workDataRef: "",
  functionType: "READ",
  validStartAt: "",
  validEndAt: "",
  changeReason: "",
};

export function TemporaryPermissionManagementPage() {
  const [userIdFilter, setUserIdFilter] = useState("");
  const [permissions, setPermissions] = useState<TemporaryPermission[]>([]);
  const [selected, setSelected] = useState<TemporaryPermission | null>(null);
  const [form, setForm] = useState<FormState>(initialForm);
  const [page, setPage] = useState(0);
  const [size] = useState(10);
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
      const parsedUserId = userIdFilter.trim()
        ? Number(userIdFilter)
        : undefined;
      const response = await temporaryPermissionApi.listTemporaryPermissions({
        userId: Number.isFinite(parsedUserId) ? parsedUserId : undefined,
        page,
        size,
      });
      setPermissions(response.data?.permissions ?? []);
      setTotalElements(response.data?.totalElements ?? 0);
      setSelected(null);
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page]);

  const selectRow = (permission: TemporaryPermission) => {
    setSelected(permission);
    setFieldErrors({});
    setError(null);
    setSuccessMessage(null);
    setForm({
      userId: String(permission.userId),
      workDataRef: permission.workDataRef,
      functionType: permission.functionType,
      validStartAt: toDateTimeLocal(permission.validStartAt),
      validEndAt: toDateTimeLocal(permission.validEndAt),
      changeReason: permission.changeReason ?? "",
    });
  };

  const resetForm = () => {
    setSelected(null);
    setFieldErrors({});
    setForm(initialForm);
  };

  const save = async () => {
    const confirmed = window.confirm(
      "지정된 교원·업무자료·기능에 임시 권한을 부여하시겠습니까?",
    );
    if (!confirmed) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const response = await temporaryPermissionApi.createTemporaryPermission({
        userId: Number(form.userId),
        workDataRef: form.workDataRef,
        functionType: form.functionType,
        validStartAt: form.validStartAt,
        validEndAt: form.validEndAt,
        changeReason: form.changeReason,
      });
      setSuccessMessage("저장되었습니다");
      if (response.data) {
        setSelected(response.data);
      }
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
        : "임시 권한 정보를 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section
        data-screen-id="SCR-TEMPORARY-PERMISSION-MGMT"
        data-testid="temporary-permission-page"
      >
        <PermissionState
          title="임시 권한 관리 권한이 없습니다"
          message="R09 시스템관리자 또는 단과대학 담당자 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-TEMPORARY-PERMISSION-MGMT"
      data-testid="temporary-permission-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">시스템 관리 / 역할·권한 관리</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              임시 권한 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              특정 교원·업무자료·기능·유효기간에 한정된 임시 예외 권한을
              부여하고 만료 회수 상태를 조회합니다.
            </p>
          </div>
          <button
            className="inline-flex h-10 items-center gap-2 rounded-md bg-lightprimary px-4 py-2 text-sm font-semibold text-primary transition-colors hover:bg-primary hover:text-white"
            data-testid="temporary-permission-refresh-button"
            onClick={() => void load()}
            type="button"
          >
            <RefreshCw size={16} /> 다시 조회
          </button>
        </div>
      </div>

      <div className="sr-only">
        임시 권한 관리 권한이 없습니다 조회된 임시 권한이 없습니다
        저장되었습니다 임시 권한을 불러오는 중입니다
      </div>
      {error ? (
        <ErrorState title="임시 권한 처리 오류" message={error} />
      ) : null}
      {successMessage ? <SuccessState title={successMessage} /> : null}

      <section
        className="rounded-md border border-ld bg-white p-5 shadow-sm"
        data-testid="temporary-permission-search-panel"
      >
        <div className="grid gap-4 lg:grid-cols-[1fr_auto]">
          <label className="text-sm font-medium text-dark">
            대상 교원 ID
            <input
              className="mt-1 w-full rounded-md border border-ld px-3 py-2 text-sm"
              data-testid="temporary-permission-user-id-filter"
              value={userIdFilter}
              onChange={(event) => setUserIdFilter(event.target.value)}
            />
          </label>
          <button
            className="mt-6 inline-flex h-10 items-center justify-center gap-2 rounded-md bg-primary px-4 text-sm font-semibold text-white"
            data-testid="temporary-permission-search-button"
            onClick={() => {
              setPage(0);
              void load();
            }}
            type="button"
          >
            <Search size={16} /> 조회
          </button>
        </div>
      </section>

      <div className="grid grid-cols-12 gap-6">
        <section
          className="col-span-12 rounded-md border border-ld bg-white p-5 shadow-sm xl:col-span-8"
          data-testid="temporary-permission-list-panel"
        >
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold text-dark">임시 권한 목록</h2>
            <span className="text-sm text-muted">총 {totalElements}건</span>
          </div>
          {loading ? (
            <LoadingState title="임시 권한을 불러오는 중입니다" />
          ) : null}
          {!loading && permissions.length === 0 ? (
            <EmptyState
              title="조회된 임시 권한이 없습니다"
              message="대상 교원 ID 조건을 확인하세요."
            />
          ) : null}
          {!loading && permissions.length > 0 ? (
            <div className="mt-4 overflow-x-auto">
              <table className="w-full min-w-[760px] text-left text-sm">
                <thead className="border-b border-ld text-muted">
                  <tr>
                    <th className="px-3 py-2">교원</th>
                    <th className="px-3 py-2">업무자료</th>
                    <th className="px-3 py-2">지정 기능</th>
                    <th className="px-3 py-2">유효기간</th>
                    <th className="px-3 py-2">상태</th>
                  </tr>
                </thead>
                <tbody>
                  {permissions.map((permission) => (
                    <tr
                      className="cursor-pointer border-b border-ld hover:bg-lightprimary"
                      data-testid={`temporary-permission-row-${permission.temporaryPermissionId}`}
                      key={permission.temporaryPermissionId}
                      onClick={() => selectRow(permission)}
                    >
                      <td className="px-3 py-3">
                        {permission.userName ?? permission.userId}
                      </td>
                      <td className="px-3 py-3">{permission.workDataRef}</td>
                      <td className="px-3 py-3">{permission.functionType}</td>
                      <td className="px-3 py-3">
                        {formatRange(
                          permission.validStartAt,
                          permission.validEndAt,
                        )}
                      </td>
                      <td className="px-3 py-3">{permission.status}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
          <div className="mt-4 flex justify-end gap-2">
            <button
              className="rounded-md border border-ld px-3 py-2 text-sm"
              data-testid="temporary-permission-prev-page-button"
              disabled={page === 0}
              onClick={() => setPage((value) => Math.max(0, value - 1))}
              type="button"
            >
              이전
            </button>
            <button
              className="rounded-md border border-ld px-3 py-2 text-sm"
              data-testid="temporary-permission-next-page-button"
              disabled={(page + 1) * size >= totalElements}
              onClick={() => setPage((value) => value + 1)}
              type="button"
            >
              다음
            </button>
          </div>
        </section>

        <section
          className="col-span-12 rounded-md border border-ld bg-white p-5 shadow-sm xl:col-span-4"
          data-testid="temporary-permission-form-panel"
        >
          <div className="flex items-center gap-2">
            <TimerReset className="text-primary" size={18} />
            <h2 className="text-lg font-semibold text-dark">임시 권한 부여</h2>
          </div>
          <p className="mt-2 text-xs text-muted">
            임시 권한은 지정 기능에만 적용되며 사용자 기본 역할이나 상시 권한은
            변경하지 않습니다.
          </p>
          <div className="mt-4 space-y-3">
            <FormInput
              label="대상 교원 ID"
              testId="temporary-permission-user-id-input"
              value={form.userId}
              onChange={(value) =>
                setForm((current) => ({ ...current, userId: value }))
              }
              error={fieldErrors.userId}
            />
            <FormInput
              label="업무자료 식별자"
              testId="temporary-permission-work-data-input"
              value={form.workDataRef}
              onChange={(value) =>
                setForm((current) => ({ ...current, workDataRef: value }))
              }
              error={fieldErrors.workDataRef}
            />
            <label className="block text-sm font-medium text-dark">
              지정 기능
              <select
                className="mt-1 w-full rounded-md border border-ld px-3 py-2 text-sm"
                data-testid="temporary-permission-function-input"
                value={form.functionType}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    functionType: event.target.value as FunctionType,
                  }))
                }
              >
                {functionTypes.map((type) => (
                  <option key={type} value={type}>
                    {type}
                  </option>
                ))}
              </select>
              {fieldErrors.functionType ? (
                <span className="mt-1 block text-xs text-error">
                  {fieldErrors.functionType}
                </span>
              ) : null}
            </label>
            <FormInput
              label="유효기간 시작"
              testId="temporary-permission-start-input"
              type="datetime-local"
              value={form.validStartAt}
              onChange={(value) =>
                setForm((current) => ({ ...current, validStartAt: value }))
              }
              error={fieldErrors.validStartAt}
            />
            <FormInput
              label="유효기간 종료"
              testId="temporary-permission-end-input"
              type="datetime-local"
              value={form.validEndAt}
              onChange={(value) =>
                setForm((current) => ({ ...current, validEndAt: value }))
              }
              error={fieldErrors.validEndAt}
            />
            <FormInput
              label="변경 사유"
              testId="temporary-permission-reason-input"
              value={form.changeReason}
              onChange={(value) =>
                setForm((current) => ({ ...current, changeReason: value }))
              }
              error={fieldErrors.changeReason}
            />
          </div>
          <div className="mt-5 flex flex-wrap gap-2">
            <button
              className="rounded-md border border-ld px-4 py-2 text-sm"
              data-testid="temporary-permission-new-button"
              onClick={resetForm}
              type="button"
            >
              신규
            </button>
            <button
              className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
              data-testid="temporary-permission-save-button"
              disabled={saving}
              onClick={() => void save()}
              type="button"
            >
              {saving ? "저장 중" : "저장"}
            </button>
          </div>
          {selected ? (
            <p className="mt-3 text-xs text-muted">
              선택 권한: {selected.temporaryPermissionId}
            </p>
          ) : null}
        </section>
      </div>
    </section>
  );
}

function FormInput({
  label,
  testId,
  value,
  onChange,
  error,
  type = "text",
}: {
  label: string;
  testId: string;
  value: string;
  onChange: (value: string) => void;
  error?: string;
  type?: string;
}) {
  return (
    <label className="block text-sm font-medium text-dark">
      {label}
      <input
        className="mt-1 w-full rounded-md border border-ld px-3 py-2 text-sm"
        data-testid={testId}
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
      />
      {error ? (
        <span className="mt-1 block text-xs text-error">{error}</span>
      ) : null}
    </label>
  );
}

function toFieldErrorMap(fields: ApiErrorField[]) {
  return fields.reduce<Record<string, string>>((accumulator, field) => {
    accumulator[field.field] = field.message;
    return accumulator;
  }, {});
}

function toDateTimeLocal(value?: string) {
  return value ? value.slice(0, 16) : "";
}

function formatRange(start: string, end: string) {
  return `${start.replace("T", " ")} ~ ${end.replace("T", " ")}`;
}
