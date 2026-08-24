import type React from "react";
import { LockKeyhole, RefreshCw, Search } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import {
  ApiClientError,
  functionPermissionApi,
  type ApiErrorField,
  type FunctionPermission,
  type FunctionType,
  type PermissionAllowed,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type FormState = {
  screenId: string;
  roleCode: string;
  functionType: FunctionType;
  permissionAllowed: PermissionAllowed;
  targetDataStatus: string;
  changeReason: string;
};

const functionTypes: FunctionType[] = [
  "READ",
  "CREATE",
  "UPDATE",
  "DELETE",
  "EXECUTE",
];
const permissionValues: PermissionAllowed[] = ["ALLOW", "DENY"];
const initialForm: FormState = {
  screenId: "SCR-FUNCTION-PERMISSION-MGMT",
  roleCode: "R09",
  functionType: "READ",
  permissionAllowed: "ALLOW",
  targetDataStatus: "",
  changeReason: "",
};

export function FunctionPermissionManagementPage() {
  const [screenId, setScreenId] = useState("SCR-FUNCTION-PERMISSION-MGMT");
  const [roleCode, setRoleCode] = useState("R09");
  const [permissions, setPermissions] = useState<FunctionPermission[]>([]);
  const [selected, setSelected] = useState<FunctionPermission | null>(null);
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
  const [evaluationMessage, setEvaluationMessage] = useState<string | null>(
    null,
  );

  const selectedLabel = useMemo(() => {
    if (!selected) return "선택된 기능 권한 없음";
    return `${selected.screenId} / ${selected.roleCode} / ${selected.functionType}`;
  }, [selected]);

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await functionPermissionApi.listFunctionPermissions({
        screenId,
        roleCode,
        page,
        size,
      });
      setPermissions(response.data?.permissions ?? []);
      setTotalElements(response.data?.totalElements ?? 0);
      setSelected(null);
      setForm({ ...initialForm, screenId, roleCode });
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page]);

  const selectRow = (permission: FunctionPermission) => {
    setSelected(permission);
    setFieldErrors({});
    setSuccessMessage(null);
    setEvaluationMessage(null);
    setForm({
      screenId: permission.screenId,
      roleCode: permission.roleCode,
      functionType: permission.functionType,
      permissionAllowed: permission.permissionAllowed,
      targetDataStatus: "",
      changeReason: "",
    });
  };

  const resetForm = () => {
    if (selected) {
      selectRow(selected);
      return;
    }
    setForm({ ...initialForm, screenId, roleCode });
  };

  const save = async () => {
    const confirmed = window.confirm(
      `${form.screenId} / ${form.roleCode} / ${form.functionType} 기능 권한을 ${form.permissionAllowed}로 저장하시겠습니까?`,
    );
    if (!confirmed) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      setEvaluationMessage(null);
      const response = await functionPermissionApi.saveFunctionPermissions({
        screenId: form.screenId,
        roleCode: form.roleCode,
        functionType: form.functionType,
        permissionAllowed: form.permissionAllowed,
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

  const evaluate = async () => {
    try {
      setError(null);
      setFieldErrors({});
      setSuccessMessage(null);
      const response = await functionPermissionApi.evaluateFunctionPermission({
        screenId: form.screenId,
        roleCode: form.roleCode,
        functionType: form.functionType,
        targetDataStatus: form.targetDataStatus,
      });
      setEvaluationMessage(
        response.data?.allowed
          ? "기능 실행이 허용되었습니다"
          : "기능 실행이 차단되었습니다",
      );
    } catch (caught) {
      handleApiError(caught);
      if (caught instanceof ApiClientError && caught.status === 403) {
        setEvaluationMessage("기능 실행이 차단되었습니다");
      }
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
        : "기능 권한 정보를 처리하지 못했습니다.",
    );
  };

  if (permissionDenied && !evaluationMessage) {
    return (
      <section
        data-screen-id="SCR-FUNCTION-PERMISSION-MGMT"
        data-testid="function-permission-page"
      >
        <PermissionState
          title="기능 권한 관리 권한이 없습니다"
          message="R09 시스템관리자 또는 해당 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-FUNCTION-PERMISSION-MGMT"
      data-testid="function-permission-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">시스템 관리 / 역할·권한 관리</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              기능 권한 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              화면×역할×기능구분별 허용 여부를 조회·저장하고 서버 기능 판정을
              확인합니다.
            </p>
          </div>
          <button
            className="inline-flex h-10 items-center gap-2 rounded-md bg-lightprimary px-4 py-2 text-sm font-semibold text-primary transition-colors hover:bg-primary hover:text-white"
            data-testid="function-permission-refresh-button"
            onClick={() => void load()}
            type="button"
          >
            <RefreshCw size={16} /> 다시 조회
          </button>
        </div>
      </div>

      <div className="sr-only">
        기능 권한 관리 권한이 없습니다 조회된 기능 권한이 없습니다
        저장되었습니다 기능 실행이 차단되었습니다
      </div>
      {error ? (
        <ErrorState title="기능 권한 처리 오류" message={error} />
      ) : null}
      {successMessage ? <SuccessState title={successMessage} /> : null}
      {evaluationMessage ? <SuccessState title={evaluationMessage} /> : null}

      <section
        className="rounded-md border border-ld bg-white p-5 shadow-sm"
        data-testid="function-permission-search-panel"
      >
        <div className="grid gap-4 lg:grid-cols-[1fr_1fr_auto]">
          <label className="text-sm font-medium text-dark">
            화면 ID
            <input
              className="mt-1 w-full rounded-md border border-ld px-3 py-2 text-sm"
              data-testid="function-permission-screen-id-input"
              value={screenId}
              onChange={(event) => setScreenId(event.target.value)}
            />
          </label>
          <label className="text-sm font-medium text-dark">
            역할 코드
            <input
              className="mt-1 w-full rounded-md border border-ld px-3 py-2 text-sm"
              data-testid="function-permission-role-code-input"
              value={roleCode}
              onChange={(event) => setRoleCode(event.target.value)}
            />
          </label>
          <button
            className="mt-6 inline-flex h-10 items-center justify-center gap-2 rounded-md bg-primary px-4 text-sm font-semibold text-white"
            data-testid="function-permission-search-button"
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
          data-testid="function-permission-list-panel"
        >
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold text-dark">기능 권한 목록</h2>
            <span className="text-sm text-muted">총 {totalElements}건</span>
          </div>
          {loading ? <LoadingState title="기능 권한 조회 중" /> : null}
          {!loading && permissions.length === 0 ? (
            <EmptyState
              title="조회된 기능 권한이 없습니다"
              message="화면 ID와 역할 코드를 확인한 뒤 조회하세요."
            />
          ) : null}
          {!loading && permissions.length > 0 ? (
            <div className="mt-4 overflow-x-auto">
              <table className="min-w-full divide-y divide-ld text-sm">
                <thead className="bg-lightsecondary text-left text-muted">
                  <tr>
                    <th className="px-3 py-2">화면</th>
                    <th className="px-3 py-2">역할</th>
                    <th className="px-3 py-2">기능구분</th>
                    <th className="px-3 py-2">허용 여부</th>
                    <th className="px-3 py-2">변경일시</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-ld">
                  {permissions.map((permission) => (
                    <tr
                      className="cursor-pointer hover:bg-lightprimary/40"
                      data-testid={`function-permission-row-${permission.functionPermissionId}`}
                      key={permission.functionPermissionId}
                      onClick={() => selectRow(permission)}
                    >
                      <td className="px-3 py-2 font-medium text-dark">
                        {permission.screenName ?? permission.screenId}
                      </td>
                      <td className="px-3 py-2">
                        {permission.roleCode}{" "}
                        {permission.roleName ? `(${permission.roleName})` : ""}
                      </td>
                      <td className="px-3 py-2">{permission.functionType}</td>
                      <td className="px-3 py-2">
                        <span
                          className={`rounded-full px-2 py-1 text-xs font-semibold ${permission.permissionAllowed === "ALLOW" ? "bg-lightsuccess text-success" : "bg-lighterror text-error"}`}
                        >
                          {permission.permissionAllowed === "ALLOW"
                            ? "허용"
                            : "차단"}
                        </span>
                      </td>
                      <td className="px-3 py-2 text-muted">
                        {permission.updatedAt ?? "-"}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : null}
          <div className="mt-4 flex justify-end gap-2">
            <button
              className="rounded-md border border-ld px-3 py-2 text-sm disabled:opacity-40"
              data-testid="function-permission-prev-page-button"
              disabled={page === 0}
              onClick={() => setPage((value) => Math.max(0, value - 1))}
              type="button"
            >
              이전
            </button>
            <button
              className="rounded-md border border-ld px-3 py-2 text-sm disabled:opacity-40"
              data-testid="function-permission-next-page-button"
              disabled={(page + 1) * size >= totalElements}
              onClick={() => setPage((value) => value + 1)}
              type="button"
            >
              다음
            </button>
          </div>
        </section>

        <aside
          className="col-span-12 rounded-md border border-ld bg-white p-5 shadow-sm xl:col-span-4"
          data-testid="function-permission-editor-panel"
        >
          <div className="flex items-center gap-2">
            <LockKeyhole className="text-primary" size={18} />
            <h2 className="text-lg font-semibold text-dark">저장·판정</h2>
          </div>
          <p className="mt-2 text-sm text-muted">{selectedLabel}</p>
          <div className="mt-4 space-y-4">
            <Field label="화면 ID" error={fieldErrors.screenId}>
              <input
                className="w-full rounded-md border border-ld px-3 py-2 text-sm"
                data-testid="function-permission-form-screen-id-input"
                value={form.screenId}
                onChange={(event) =>
                  setForm({ ...form, screenId: event.target.value })
                }
              />
            </Field>
            <Field label="역할 코드" error={fieldErrors.roleCode}>
              <input
                className="w-full rounded-md border border-ld px-3 py-2 text-sm"
                data-testid="function-permission-form-role-code-input"
                value={form.roleCode}
                onChange={(event) =>
                  setForm({ ...form, roleCode: event.target.value })
                }
              />
            </Field>
            <Field label="기능구분" error={fieldErrors.functionType}>
              <select
                className="w-full rounded-md border border-ld px-3 py-2 text-sm"
                data-testid="function-permission-function-type-select"
                value={form.functionType}
                onChange={(event) =>
                  setForm({
                    ...form,
                    functionType: event.target.value as FunctionType,
                  })
                }
              >
                {functionTypes.map((value) => (
                  <option key={value} value={value}>
                    {value}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="허용 여부" error={fieldErrors.permissionAllowed}>
              <select
                className="w-full rounded-md border border-ld px-3 py-2 text-sm"
                data-testid="function-permission-allowed-select"
                value={form.permissionAllowed}
                onChange={(event) =>
                  setForm({
                    ...form,
                    permissionAllowed: event.target.value as PermissionAllowed,
                  })
                }
              >
                {permissionValues.map((value) => (
                  <option key={value} value={value}>
                    {value === "ALLOW" ? "허용" : "차단"}
                  </option>
                ))}
              </select>
            </Field>
            <Field
              label="대상 데이터 상태(판정용)"
              error={fieldErrors.targetDataStatus}
            >
              <select
                className="w-full rounded-md border border-ld px-3 py-2 text-sm"
                data-testid="function-permission-target-status-select"
                value={form.targetDataStatus}
                onChange={(event) =>
                  setForm({ ...form, targetDataStatus: event.target.value })
                }
              >
                <option value="">일반</option>
                <option value="EVALUATION_CONFIRMED">평가확정</option>
              </select>
            </Field>
            <Field label="변경 사유" error={fieldErrors.changeReason}>
              <textarea
                className="w-full rounded-md border border-ld px-3 py-2 text-sm min-h-[84px]"
                data-testid="function-permission-change-reason-textarea"
                value={form.changeReason}
                onChange={(event) =>
                  setForm({ ...form, changeReason: event.target.value })
                }
              />
            </Field>
          </div>
          <div className="mt-5 flex flex-wrap gap-2">
            <button
              className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
              data-testid="function-permission-save-button"
              disabled={saving}
              onClick={() => void save()}
              type="button"
            >
              {saving ? "저장 중" : "저장"}
            </button>
            <button
              className="rounded-md border border-ld px-4 py-2 text-sm"
              data-testid="function-permission-evaluate-button"
              onClick={() => void evaluate()}
              type="button"
            >
              기능 판정
            </button>
            <button
              className="rounded-md border border-ld px-4 py-2 text-sm"
              data-testid="function-permission-cancel-button"
              onClick={resetForm}
              type="button"
            >
              취소
            </button>
          </div>
        </aside>
      </div>
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
    <label className="block text-sm font-medium text-dark">
      {label}
      <div className="mt-1">{children}</div>
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
