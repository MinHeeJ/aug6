import type React from "react";
import { FileCog, RefreshCw, Search } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { useAuth } from "../../app/AuthProvider";
import {
  ApiClientError,
  menuExecutionApi,
  type ApiErrorField,
  type MenuExecution,
  type MenuItem,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type FormState = {
  menuName: string;
  screenId: string;
  url: string;
  icon: string;
  businessCategory: string;
  description: string;
  changeReason: string;
};

type MenuCandidate = Pick<
  MenuItem,
  "menuId" | "menuName" | "screenId" | "url" | "icon"
> & { parentMenuName?: string };

const emptyForm: FormState = {
  menuName: "",
  screenId: "",
  url: "",
  icon: "",
  businessCategory: "",
  description: "",
  changeReason: "",
};

export function MenuInfoManagementPage() {
  const auth = useAuth();
  const menuCandidates = useMemo(
    () => flattenMenus(auth.user?.menus ?? []),
    [auth.user?.menus],
  );
  const [menuNameFilter, setMenuNameFilter] = useState("");
  const [urlFilter, setUrlFilter] = useState("");
  const [selectedMenuId, setSelectedMenuId] = useState<number | null>(
    menuCandidates[0]?.menuId ?? null,
  );
  const [execution, setExecution] = useState<MenuExecution | null>(null);
  const [form, setForm] = useState<FormState>(emptyForm);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [permissionDenied, setPermissionDenied] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const filteredMenus = useMemo(
    () =>
      menuCandidates.filter((menu) => {
        const menuMatch =
          !menuNameFilter ||
          menu.menuName.includes(menuNameFilter) ||
          (menu.screenId ?? "").includes(menuNameFilter);
        const urlMatch = !urlFilter || (menu.url ?? "").includes(urlFilter);
        return menuMatch && urlMatch;
      }),
    [menuCandidates, menuNameFilter, urlFilter],
  );

  const loadExecution = async (menuId = selectedMenuId) => {
    if (menuId === null) {
      setLoading(false);
      setExecution(null);
      setForm(emptyForm);
      return;
    }
    try {
      setLoading(true);
      setError(null);
      setPermissionDenied(false);
      const response = await menuExecutionApi.getMenuExecution(menuId);
      const data = response.data ?? null;
      setExecution(data);
      setForm(data ? toForm(data) : emptyForm);
      setSelectedMenuId(menuId);
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const firstMenuId = selectedMenuId ?? menuCandidates[0]?.menuId ?? null;
    if (selectedMenuId === null && firstMenuId !== null) {
      setSelectedMenuId(firstMenuId);
    }
    void loadExecution(firstMenuId);
  }, []);

  const selectMenu = (menuId: number) => {
    setFieldErrors({});
    setSuccessMessage(null);
    void loadExecution(menuId);
  };

  const resetFilters = () => {
    setMenuNameFilter("");
    setUrlFilter("");
    setSelectedMenuId(null);
    setExecution(null);
    setForm(emptyForm);
    setError(null);
    setSuccessMessage(null);
  };

  const cancel = () => {
    setForm(execution ? toForm(execution) : emptyForm);
    setFieldErrors({});
    setSuccessMessage(null);
  };

  const save = async () => {
    if (!execution || selectedMenuId === null) return;
    const confirmed =
      typeof window === "undefined" ||
      window.confirm(`${form.menuName} 실행정보를 저장합니까?`);
    if (!confirmed) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      const response = await menuExecutionApi.updateMenuExecution(
        selectedMenuId,
        {
          menuName: form.menuName,
          screenId: form.screenId,
          url: form.url,
          icon: form.icon || undefined,
          businessCategory: form.businessCategory || undefined,
          description: form.description || undefined,
          changeReason: form.changeReason || undefined,
        },
      );
      const data = response.data ?? null;
      setExecution(data);
      setForm(data ? toForm(data) : form);
      setSuccessMessage("저장되었습니다");
      await loadExecution(selectedMenuId);
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
        : "메뉴 실행정보를 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section data-screen-id="SCR-MENU-INFO-MGMT">
        <PermissionState
          title="메뉴 정보 관리 권한이 없습니다"
          message="R09 시스템관리자 또는 해당 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section className="space-y-6" data-screen-id="SCR-MENU-INFO-MGMT">
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">시스템 관리 / 메뉴 관리</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              메뉴 정보 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              메뉴명·화면ID·URL·아이콘·업무구분·설명을 조회하고 실행 화면 연결
              정보를 저장합니다.
            </p>
          </div>
          <button
            className="inline-flex h-10 items-center gap-2 rounded-md bg-lightprimary px-4 py-2 text-sm font-semibold text-primary transition-colors hover:bg-primary hover:text-white"
            onClick={() => void loadExecution()}
            type="button"
          >
            <RefreshCw size={16} /> 다시 조회
          </button>
        </div>
      </div>

      <div className="sr-only">
        메뉴 실행정보를 불러오는 중입니다 조회된 메뉴 실행정보가 없습니다 메뉴
        정보 관리 권한이 없습니다 저장되었습니다 저장된 URL로 메뉴 클릭 시 이동
        대상과 화면ID가 일치해야 합니다
      </div>
      {error ? (
        <ErrorState title="메뉴 실행정보 처리 오류" message={error} />
      ) : null}
      {successMessage ? (
        <SuccessState title="처리 완료" message={successMessage} />
      ) : null}

      <div className="rounded-md border border-ld bg-white p-6 shadow-md">
        <h2 className="card-title mb-4 text-lg font-semibold text-dark">
          검색조건
        </h2>
        <div className="grid grid-cols-12 gap-5 md:gap-6">
          <Field label="메뉴명/화면ID" className="col-span-12 md:col-span-4">
            <div className="relative">
              <Search
                className="absolute left-3 top-1/2 -translate-y-1/2 text-muted"
                size={16}
              />
              <input
                className="h-10 w-full rounded-lg border border-ld px-3 py-2 pl-10 text-sm"
                value={menuNameFilter}
                onChange={(event) => setMenuNameFilter(event.target.value)}
                placeholder="메뉴 정보 관리 / SCR-MENU"
              />
            </div>
          </Field>
          <Field label="URL" className="col-span-12 md:col-span-4">
            <input
              className="h-10 w-full rounded-lg border border-ld px-3 py-2 text-sm"
              value={urlFilter}
              onChange={(event) => setUrlFilter(event.target.value)}
              placeholder="/admin/menu-info"
            />
          </Field>
          <div className="col-span-12 flex items-end gap-3 md:col-span-4">
            <button
              className="h-10 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
              disabled={loading}
              onClick={() =>
                selectedMenuId !== null && void loadExecution(selectedMenuId)
              }
              type="button"
            >
              조회
            </button>
            <button
              className="h-10 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary hover:bg-primary hover:text-white"
              onClick={resetFilters}
              type="button"
            >
              조건 초기화
            </button>
          </div>
        </div>
        <p className="mt-4 text-xs text-muted">
          목록은 로그인 사용자의 권한 메뉴 tree에서 가져오며, 실행정보 상세는
          선택한 실제 menuId로 getMenuExecution을 호출합니다.
        </p>
      </div>

      {loading ? (
        <LoadingState
          title="메뉴 실행정보를 불러오는 중입니다"
          message="선택 메뉴의 실행 화면 연결 정보를 조회하고 있습니다."
        />
      ) : null}

      <div className="grid grid-cols-12 gap-6">
        <div className="col-span-12 rounded-md border border-ld bg-white p-6 shadow-md lg:col-span-7">
          <div className="mb-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <h2 className="card-title flex items-center gap-2 text-lg font-semibold text-dark">
              <FileCog size={18} /> 메뉴 실행정보 목록
            </h2>
            <p className="text-sm text-muted">{filteredMenus.length}개 메뉴</p>
          </div>
          <div className="overflow-x-auto rounded-md border border-border">
            <table className="w-full caption-bottom text-sm">
              <thead className="border-b border-ld bg-lightgray">
                <tr>
                  <th className="px-4 py-3 text-left">상위 메뉴</th>
                  <th className="px-4 py-3 text-left">메뉴명</th>
                  <th className="px-4 py-3 text-left">화면ID</th>
                  <th className="px-4 py-3 text-left">URL</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {filteredMenus.map((menu) => (
                  <tr
                    key={menu.menuId}
                    className={`cursor-pointer transition-colors hover:bg-lightprimary ${selectedMenuId === menu.menuId ? "bg-lightprimary" : ""}`}
                    onClick={() => selectMenu(menu.menuId)}
                  >
                    <td className="whitespace-nowrap px-4 py-3 text-muted">
                      {menu.parentMenuName ?? "-"}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 font-semibold text-dark">
                      {menu.menuName}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3">
                      {menu.screenId ?? "-"}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-primary">
                      {menu.url ?? "-"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {!loading && filteredMenus.length === 0 ? (
            <EmptyState
              title="조회된 메뉴 실행정보가 없습니다"
              message="메뉴명 또는 URL 조건을 변경하거나 권한 메뉴 tree를 확인하세요."
            />
          ) : null}
        </div>

        <div className="col-span-12 rounded-md border border-ld bg-white p-6 shadow-md lg:col-span-5">
          <h2 className="card-title mb-4 text-lg font-semibold text-dark">
            실행정보 상세/저장
          </h2>
          {!execution ? (
            <EmptyState
              title="메뉴를 선택하세요"
              message="목록에서 메뉴를 선택하면 메뉴명·화면ID·URL·아이콘·업무구분·설명을 편집할 수 있습니다."
            />
          ) : (
            <div className="space-y-4">
              <Readonly label="menuId" value={String(execution.menuId)} />
              <Field label="메뉴명" error={fieldErrors.menuName} required>
                <input
                  className="h-10 w-full rounded-lg border border-ld px-3 py-2 text-sm"
                  value={form.menuName}
                  onChange={(event) =>
                    setForm({ ...form, menuName: event.target.value })
                  }
                />
              </Field>
              <Field label="화면ID" error={fieldErrors.screenId} required>
                <input
                  className="h-10 w-full rounded-lg border border-ld px-3 py-2 text-sm"
                  value={form.screenId}
                  onChange={(event) =>
                    setForm({ ...form, screenId: event.target.value })
                  }
                />
              </Field>
              <Field label="URL" error={fieldErrors.url} required>
                <input
                  className="h-10 w-full rounded-lg border border-ld px-3 py-2 text-sm"
                  value={form.url}
                  onChange={(event) =>
                    setForm({ ...form, url: event.target.value })
                  }
                />
              </Field>
              <div className="grid grid-cols-2 gap-4">
                <Field label="아이콘" error={fieldErrors.icon}>
                  <input
                    className="h-10 w-full rounded-lg border border-ld px-3 py-2 text-sm"
                    value={form.icon}
                    onChange={(event) =>
                      setForm({ ...form, icon: event.target.value })
                    }
                  />
                </Field>
                <Field label="업무구분" error={fieldErrors.businessCategory}>
                  <input
                    className="h-10 w-full rounded-lg border border-ld px-3 py-2 text-sm"
                    value={form.businessCategory}
                    onChange={(event) =>
                      setForm({ ...form, businessCategory: event.target.value })
                    }
                  />
                </Field>
              </div>
              <Field label="설명" error={fieldErrors.description}>
                <textarea
                  className="min-h-20 w-full rounded-lg border border-ld px-3 py-2 text-sm"
                  value={form.description}
                  onChange={(event) =>
                    setForm({ ...form, description: event.target.value })
                  }
                />
              </Field>
              <Field label="변경 사유" error={fieldErrors.changeReason}>
                <textarea
                  className="min-h-20 w-full rounded-lg border border-ld px-3 py-2 text-sm"
                  value={form.changeReason}
                  onChange={(event) =>
                    setForm({ ...form, changeReason: event.target.value })
                  }
                  placeholder="실행정보 변경 사유"
                />
              </Field>
              <div className="rounded-md bg-lightsecondary p-4 text-sm text-muted">
                저장된 URL로 메뉴 클릭 시 이동 대상과 화면ID가 일치해야 합니다.
                현재 연결: {form.screenId || "-"} / {form.url || "-"}
              </div>
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
                  onClick={cancel}
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

function flattenMenus(
  items: MenuItem[],
  parentMenuName?: string,
): MenuCandidate[] {
  return items.flatMap((item) => {
    const children = flattenMenus(item.children ?? [], item.menuName);
    const self =
      item.url && item.screenId
        ? [
            {
              menuId: item.menuId,
              menuName: item.menuName,
              screenId: item.screenId,
              url: item.url,
              icon: item.icon,
              parentMenuName,
            },
          ]
        : [];
    return [...self, ...children];
  });
}

function toForm(execution: MenuExecution): FormState {
  return {
    menuName: execution.menuName ?? "",
    screenId: execution.screenId ?? "",
    url: execution.url ?? "",
    icon: execution.icon ?? "",
    businessCategory: execution.businessCategory ?? "",
    description: execution.description ?? "",
    changeReason: "",
  };
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
