import type React from "react";
import { LockKeyhole, RefreshCw, Search } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import {
  ApiClientError,
  menuPermissionApi,
  type ApiErrorField,
  type MenuPermission,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type TargetType = "ROLE" | "ORGANIZATION" | "USER";
type AccessAllowed = "ALLOW" | "DENY";

type FormState = {
  targetType: TargetType;
  targetId: string;
  menuId: number | null;
  accessAllowed: AccessAllowed;
  changeReason: string;
};

const initialForm: FormState = {
  targetType: "ROLE",
  targetId: "R09",
  menuId: null,
  accessAllowed: "ALLOW",
  changeReason: "",
};

export function MenuPermissionManagementPage() {
  const [targetType, setTargetType] = useState<TargetType>("ROLE");
  const [targetId, setTargetId] = useState("R09");
  const [accessFilter, setAccessFilter] = useState("");
  const [filter, setFilter] = useState("");
  const [permissions, setPermissions] = useState<MenuPermission[]>([]);
  const [selected, setSelected] = useState<MenuPermission | null>(null);
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

  const displayedPermissions = useMemo(() => {
    if (!accessFilter) return permissions;
    return permissions.filter(
      (permission) => permission.accessAllowed === accessFilter,
    );
  }, [accessFilter, permissions]);

  const load = async () => {
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await menuPermissionApi.listMenuPermissions({
        targetType,
        targetId,
        filter,
        accessAllowed: accessFilter
          ? (accessFilter as AccessAllowed)
          : undefined,
        page,
        size,
      });
      setPermissions(response.data?.permissions ?? []);
      setTotalElements(response.data?.totalElements ?? 0);
      setSelected(null);
      setForm({
        targetType,
        targetId,
        menuId: null,
        accessAllowed: "ALLOW",
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
  }, [page]);

  const selectRow = (permission: MenuPermission) => {
    setSelected(permission);
    setFieldErrors({});
    setSuccessMessage(null);
    setForm({
      targetType: permission.targetType,
      targetId: permission.targetId,
      menuId: permission.menuId,
      accessAllowed: permission.accessAllowed,
      changeReason: "",
    });
  };

  const resetFilters = () => {
    setTargetType("ROLE");
    setTargetId("");
    setAccessFilter("");
    setFilter("");
    setPage(0);
  };

  const resetForm = () => {
    if (selected) {
      selectRow(selected);
      return;
    }
    setForm({
      targetType,
      targetId,
      menuId: null,
      accessAllowed: "ALLOW",
      changeReason: "",
    });
  };

  const save = async () => {
    if (!selected || form.menuId === null) return;
    const confirmed = window.confirm(
      `${form.targetType} ${form.targetId}의 ${selected.screenMenuName} 접근권한을 ${form.accessAllowed}로 저장합니까?`,
    );
    if (!confirmed) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const response = await menuPermissionApi.saveMenuPermissions({
        targetType: form.targetType,
        targetId: form.targetId,
        menuId: form.menuId,
        accessAllowed: form.accessAllowed,
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
        return;
      }
      setError(caught.message);
      setFieldErrors(toFieldErrorMap(caught.apiError?.fields ?? []));
      return;
    }
    setError(
      caught instanceof Error
        ? caught.message
        : "메뉴 권한 정보를 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section data-screen-id="SCR-MENU-PERMISSION-MGMT">
        <PermissionState
          title="메뉴 권한 관리 권한이 없습니다"
          message="R09 시스템관리자 또는 해당 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section className="space-y-6" data-screen-id="SCR-MENU-PERMISSION-MGMT">
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">시스템 관리 / 역할·권한 관리</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              메뉴 권한 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              역할·조직·사용자 단위로 대메뉴·중메뉴·화면 접근 여부를 조회하고
              저장합니다.
            </p>
          </div>
          <button
            className="inline-flex h-10 items-center gap-2 rounded-md bg-lightprimary px-4 py-2 text-sm font-semibold text-primary transition-colors hover:bg-primary hover:text-white"
            onClick={() => void load()}
            type="button"
          >
            <RefreshCw size={16} /> 다시 조회
          </button>
        </div>
      </div>

      <div className="sr-only">
        메뉴 권한 관리 권한이 없습니다 조회된 메뉴 권한이 없습니다
        저장되었습니다
      </div>
      {error ? (
        <ErrorState title="메뉴 권한 처리 오류" message={error} />
      ) : null}
      {successMessage ? (
        <SuccessState title="처리 완료" message={successMessage} />
      ) : null}

      <div className="rounded-md border border-ld bg-white p-6 shadow-md">
        <h2 className="card-title mb-4 text-lg font-semibold text-dark">
          검색조건
        </h2>
        <div className="grid grid-cols-12 gap-5 md:gap-6">
          <Field
            label="대상 유형"
            error={fieldErrors.targetType}
            required
            className="col-span-12 md:col-span-3"
          >
            <select
              className="h-10 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={targetType}
              onChange={(event) =>
                setTargetType(event.target.value as TargetType)
              }
            >
              <option value="ROLE">ROLE</option>
              <option value="ORGANIZATION">ORGANIZATION</option>
              <option value="USER">USER</option>
            </select>
          </Field>
          <Field
            label="대상 식별자"
            error={fieldErrors.targetId}
            required
            className="col-span-12 md:col-span-3"
          >
            <input
              className="h-10 w-full rounded-lg border border-ld px-3 py-2 text-sm"
              value={targetId}
              onChange={(event) => setTargetId(event.target.value)}
              placeholder="R09 / 조직코드 / user_id"
            />
          </Field>
          <Field label="accessAllowed" className="col-span-12 md:col-span-3">
            <select
              className="h-10 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={accessFilter}
              onChange={(event) => setAccessFilter(event.target.value)}
            >
              <option value="">전체</option>
              <option value="ALLOW">ALLOW</option>
              <option value="DENY">DENY</option>
            </select>
          </Field>
          <Field label="화면 검색" className="col-span-12 md:col-span-3">
            <div className="relative">
              <Search
                className="absolute left-3 top-1/2 -translate-y-1/2 text-muted"
                size={16}
              />
              <input
                className="h-10 w-full rounded-lg border border-ld px-3 py-2 pl-10 text-sm"
                value={filter}
                onChange={(event) => setFilter(event.target.value)}
                placeholder="메뉴명/화면ID/URL"
              />
            </div>
          </Field>
          <div className="col-span-12 flex gap-3">
            <button
              className="h-10 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
              disabled={loading}
              onClick={() => void load()}
              type="button"
            >
              조회
            </button>
            <button
              className="h-10 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary hover:bg-primary hover:text-white"
              onClick={resetFilters}
              type="button"
            >
              초기화
            </button>
          </div>
        </div>
        <p className="mt-4 text-xs text-muted">
          대상 유형 enum은 ROLE / ORGANIZATION / USER입니다. 화면은 서버의 권한
          결과를 반영하며 우선순위를 임의 계산하지 않습니다.
        </p>
      </div>

      {loading ? (
        <LoadingState
          title="메뉴 권한을 불러오는 중입니다"
          message="대상 유형과 대상 식별자에 맞는 접근권한 matrix를 조회하고 있습니다."
        />
      ) : null}

      <div className="grid grid-cols-12 gap-6">
        <div className="col-span-12 rounded-md border border-ld bg-white p-6 shadow-md lg:col-span-8">
          <div className="mb-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <h2 className="card-title flex items-center gap-2 text-lg font-semibold text-dark">
              <LockKeyhole size={18} /> 접근권한 matrix
            </h2>
            <p className="text-sm text-muted">
              page {page + 1} / size {size} / total {totalElements}
            </p>
          </div>
          <div className="overflow-x-auto rounded-md border border-border">
            <table className="w-full caption-bottom text-sm">
              <thead className="border-b border-ld bg-lightgray">
                <tr>
                  <th className="px-4 py-3 text-left">대메뉴</th>
                  <th className="px-4 py-3 text-left">중메뉴</th>
                  <th className="px-4 py-3 text-left">화면</th>
                  <th className="px-4 py-3 text-left">accessAllowed</th>
                  <th className="px-4 py-3 text-left">변경 상태</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {displayedPermissions.map((permission) => (
                  <tr
                    key={`${permission.targetType}-${permission.targetId}-${permission.menuId}`}
                    className={`cursor-pointer transition-colors hover:bg-lightprimary ${selected?.menuId === permission.menuId ? "bg-lightprimary" : ""}`}
                    onClick={() => selectRow(permission)}
                  >
                    <td className="whitespace-nowrap px-4 py-3 font-semibold text-dark">
                      {permission.topMenuName ?? "-"}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3">
                      {permission.middleMenuName ?? "-"}
                    </td>
                    <td className="min-w-[200px] px-4 py-3">
                      <p className="font-semibold text-dark">
                        {permission.screenMenuName}
                      </p>
                      <p className="text-xs text-muted">
                        {permission.screenId ?? permission.url}
                      </p>
                    </td>
                    <td className="whitespace-nowrap px-4 py-3">
                      <AccessBadge value={permission.accessAllowed} />
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-sm text-muted">
                      저장됨
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {!loading && displayedPermissions.length === 0 ? (
            <EmptyState
              title="조회된 메뉴 권한이 없습니다"
              message="대상 유형 또는 대상 식별자를 변경해 다시 조회하세요."
            />
          ) : null}
          <div className="mt-4 flex gap-3">
            <button
              className="rounded-md border border-primary px-3 py-2 text-sm text-primary disabled:opacity-50"
              disabled={page === 0}
              onClick={() => setPage(Math.max(0, page - 1))}
              type="button"
            >
              이전
            </button>
            <button
              className="rounded-md border border-primary px-3 py-2 text-sm text-primary disabled:opacity-50"
              disabled={(page + 1) * size >= totalElements}
              onClick={() => setPage(page + 1)}
              type="button"
            >
              다음
            </button>
          </div>
        </div>

        <div className="col-span-12 rounded-md border border-ld bg-white p-6 shadow-md lg:col-span-4">
          <h2 className="card-title mb-4 text-lg font-semibold text-dark">
            선택 권한 상세/저장
          </h2>
          {!selected ? (
            <EmptyState
              title="메뉴 권한을 선택하세요"
              message="matrix 행을 선택하면 accessAllowed와 변경 사유를 저장할 수 있습니다."
            />
          ) : (
            <div className="space-y-4">
              <Readonly label="targetType" value={form.targetType} />
              <Readonly label="targetId" value={form.targetId} />
              <Readonly label="menuId" value={String(form.menuId ?? "")} />
              <Readonly label="화면" value={selected.screenMenuName} />
              <Field
                label="accessAllowed"
                error={fieldErrors.accessAllowed}
                required
              >
                <select
                  className="h-10 w-full rounded-md border border-ld px-3 py-2 text-sm"
                  value={form.accessAllowed}
                  onChange={(event) =>
                    setForm({
                      ...form,
                      accessAllowed: event.target.value as AccessAllowed,
                    })
                  }
                >
                  <option value="ALLOW">ALLOW</option>
                  <option value="DENY">DENY</option>
                </select>
              </Field>
              <Field
                label="변경 사유"
                error={fieldErrors.changeReason}
                required
              >
                <textarea
                  className="min-h-24 w-full rounded-lg border border-ld px-3 py-2 text-sm"
                  value={form.changeReason}
                  onChange={(event) =>
                    setForm({ ...form, changeReason: event.target.value })
                  }
                  placeholder="접근권한 변경 사유"
                />
              </Field>
              {fieldErrors.menuId ? (
                <p className="text-xs text-error">{fieldErrors.menuId}</p>
              ) : null}
              <div className="flex gap-3">
                <button
                  className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
                  disabled={saving}
                  onClick={() => void save()}
                  type="button"
                >
                  저장
                </button>
                <button
                  className="rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary"
                  onClick={resetForm}
                  type="button"
                >
                  취소
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </section>
  );
}

function AccessBadge({ value }: { value: AccessAllowed }) {
  const className =
    value === "ALLOW"
      ? "bg-lightsuccess text-success"
      : "bg-lighterror text-error";
  return (
    <span
      className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-semibold ${className}`}
    >
      {value}
    </span>
  );
}

function Field({
  label,
  error,
  required,
  className,
  children,
}: {
  label: string;
  error?: string;
  required?: boolean;
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <label className={`block text-sm font-semibold text-ld ${className ?? ""}`}>
      {label}
      {required ? <span className="ml-1 text-error">*</span> : null}
      <div className="mt-2">{children}</div>
      {error ? <p className="mt-1 text-xs text-error">{error}</p> : null}
    </label>
  );
}

function Readonly({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-xs font-semibold text-muted">{label}</p>
      <p className="mt-1 rounded-lg bg-lightgray px-3 py-2 text-sm text-dark">
        {value}
      </p>
    </div>
  );
}

function toFieldErrorMap(fields: ApiErrorField[]) {
  return fields.reduce<Record<string, string>>((accumulator, field) => {
    accumulator[field.field] = field.message;
    return accumulator;
  }, {});
}
