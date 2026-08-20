import type React from "react";
import { RefreshCw, Search, ToggleLeft } from "lucide-react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  menuUsageApi,
  type ApiErrorField,
  type MenuUsageSetting,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

type SystemUseYn = "Y" | "N";

type FormState = {
  menuId: number | null;
  systemUseYn: SystemUseYn;
  exposureStartAt: string;
  exposureEndAt: string;
  changeReason: string;
};

const emptyForm: FormState = {
  menuId: null,
  systemUseYn: "Y",
  exposureStartAt: "",
  exposureEndAt: "",
  changeReason: "",
};

export function MenuUsageManagementPage() {
  const [filter, setFilter] = useState("");
  const [systemUseYnFilter, setSystemUseYnFilter] = useState("");
  const [settings, setSettings] = useState<MenuUsageSetting[]>([]);
  const [selected, setSelected] = useState<MenuUsageSetting | null>(null);
  const [form, setForm] = useState<FormState>(emptyForm);
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
      const response = await menuUsageApi.listMenuUsageSettings({
        filter,
        systemUseYn: systemUseYnFilter
          ? (systemUseYnFilter as SystemUseYn)
          : undefined,
        page,
        size,
      });
      const rows = response.data?.settings ?? [];
      setSettings(rows);
      setTotalElements(response.data?.totalElements ?? 0);
      setSelected(null);
      setForm(emptyForm);
    } catch (caught) {
      handleApiError(caught);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, [page]);

  const selectRow = (setting: MenuUsageSetting) => {
    setSelected(setting);
    setFieldErrors({});
    setSuccessMessage(null);
    setForm({
      menuId: setting.menuId,
      systemUseYn: setting.systemUseYn,
      exposureStartAt: toInputDateTime(setting.exposureStartAt),
      exposureEndAt: toInputDateTime(setting.exposureEndAt),
      changeReason: "",
    });
  };

  const resetFilters = () => {
    setFilter("");
    setSystemUseYnFilter("");
    setPage(0);
  };

  const resetForm = () => {
    if (selected) {
      selectRow(selected);
      return;
    }
    setForm(emptyForm);
  };

  const save = async () => {
    if (!selected || form.menuId === null) return;
    const localErrors = validateForm(form);
    setFieldErrors(localErrors);
    if (Object.keys(localErrors).length > 0) return;
    const confirmed = window.confirm(
      `${selected.menuName} 메뉴 사용 설정을 저장합니까?`,
    );
    if (!confirmed) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      await menuUsageApi.saveMenuUsageSettings({
        items: [
          {
            menuId: form.menuId,
            systemUseYn: form.systemUseYn,
            exposureStartAt: fromInputDateTime(form.exposureStartAt),
            exposureEndAt: fromInputDateTime(form.exposureEndAt),
            changeReason: form.changeReason,
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
        : "메뉴 사용 설정 정보를 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section data-screen-id="SCR-MENU-USAGE-MGMT">
        <PermissionState
          title="메뉴 사용 관리 권한이 없습니다"
          message="R09 시스템관리자 또는 해당 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-MENU-USAGE-MGMT"
      data-testid="menu-usage-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">시스템 관리 / 메뉴 관리</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              메뉴 사용 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              메뉴별 사용여부와 노출기간을 설정합니다. 중지 또는 비노출 메뉴는
              사용자 메뉴와 직접 URL 접근에서 차단됩니다.
            </p>
          </div>
          <button
            className="inline-flex h-10 items-center gap-2 rounded-md bg-lightprimary px-4 py-2 text-sm font-semibold text-primary transition-colors hover:bg-primary hover:text-white"
            onClick={() => void load()}
            type="button"
            data-testid="menu-usage-refresh-button"
          >
            <RefreshCw size={16} /> 다시 조회
          </button>
        </div>
      </div>

      <div className="sr-only">
        메뉴 사용 설정을 불러오는 중입니다 조회된 메뉴 사용 설정이 없습니다 메뉴
        사용 관리 권한이 없습니다 저장되었습니다 상위 메뉴 메뉴명 URL 사용여부
        노출 시작 노출 종료
      </div>
      {error ? (
        <ErrorState title="메뉴 사용 설정 처리 오류" message={error} />
      ) : null}
      {successMessage ? <SuccessState title={successMessage} /> : null}

      <section
        className="rounded-md bg-white p-6 shadow-md dark:bg-darkgray"
        data-testid="menu-usage-search-panel"
      >
        <div className="mb-4 flex items-center gap-2 text-dark dark:text-white">
          <Search size={18} />
          <h2 className="text-lg font-semibold">검색조건</h2>
        </div>
        <div className="grid gap-4 md:grid-cols-3">
          <label className="text-sm font-medium text-link">
            메뉴명/URL
            <input
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={filter}
              onChange={(event) => setFilter(event.target.value)}
              placeholder="메뉴명, 화면ID, URL"
              data-testid="menu-usage-filter-input"
            />
          </label>
          <label className="text-sm font-medium text-link">
            사용여부
            <select
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
              value={systemUseYnFilter}
              onChange={(event) => setSystemUseYnFilter(event.target.value)}
              data-testid="menu-usage-system-use-filter-select"
            >
              <option value="">전체</option>
              <option value="Y">사용</option>
              <option value="N">미사용</option>
            </select>
          </label>
          <div className="flex items-end gap-2">
            <button
              className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
              onClick={() => void load()}
              type="button"
              data-testid="menu-usage-search-button"
            >
              조회
            </button>
            <button
              className="rounded-md border border-ld px-4 py-2 text-sm font-semibold text-link"
              onClick={resetFilters}
              type="button"
              data-testid="menu-usage-reset-button"
            >
              조건 초기화
            </button>
          </div>
        </div>
      </section>

      <div className="grid grid-cols-12 gap-6">
        <section
          className="col-span-12 rounded-md bg-white p-6 shadow-md xl:col-span-8 dark:bg-darkgray"
          data-testid="menu-usage-list-panel"
        >
          <div className="mb-4 flex items-center justify-between gap-2">
            <h2 className="text-lg font-semibold text-dark dark:text-white">
              메뉴 사용 설정 목록
            </h2>
            <span className="text-sm text-muted">총 {totalElements}건</span>
          </div>
          {loading ? (
            <LoadingState title="메뉴 사용 설정을 불러오는 중입니다" />
          ) : settings.length === 0 ? (
            <EmptyState title="조회된 메뉴 사용 설정이 없습니다" />
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-ld text-sm">
                <thead className="bg-lightgray text-left text-xs uppercase text-muted">
                  <tr>
                    <th className="px-4 py-3">상위 메뉴</th>
                    <th className="px-4 py-3">메뉴명</th>
                    <th className="px-4 py-3">URL</th>
                    <th className="px-4 py-3">사용여부</th>
                    <th className="px-4 py-3">노출 시작</th>
                    <th className="px-4 py-3">노출 종료</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-ld">
                  {settings.map((setting) => (
                    <tr
                      key={setting.menuId}
                      className={`cursor-pointer hover:bg-lightprimary/40 ${selected?.menuId === setting.menuId ? "bg-lightprimary/70" : ""}`}
                      onClick={() => selectRow(setting)}
                      data-testid={`menu-usage-row-${setting.menuId}`}
                    >
                      <td className="px-4 py-3 text-muted">
                        {setting.middleMenuName ?? setting.topMenuName ?? "-"}
                      </td>
                      <td className="px-4 py-3 font-medium text-dark">
                        {setting.menuName}
                      </td>
                      <td className="px-4 py-3 text-muted">
                        {setting.url ?? "-"}
                      </td>
                      <td className="px-4 py-3">
                        {setting.systemUseYn === "Y" ? "사용" : "미사용"}
                      </td>
                      <td className="px-4 py-3 text-muted">
                        {formatDateTime(setting.exposureStartAt)}
                      </td>
                      <td className="px-4 py-3 text-muted">
                        {formatDateTime(setting.exposureEndAt)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          <div className="mt-4 flex items-center justify-end gap-2 text-sm">
            <button
              className="rounded-md border border-ld px-3 py-1"
              disabled={page === 0}
              onClick={() => setPage((value) => Math.max(value - 1, 0))}
              type="button"
              data-testid="menu-usage-prev-page-button"
            >
              이전
            </button>
            <span>
              {page + 1} / {Math.max(1, Math.ceil(totalElements / size))}
            </span>
            <button
              className="rounded-md border border-ld px-3 py-1"
              disabled={(page + 1) * size >= totalElements}
              onClick={() => setPage((value) => value + 1)}
              type="button"
              data-testid="menu-usage-next-page-button"
            >
              다음
            </button>
          </div>
        </section>

        <section
          className="col-span-12 rounded-md bg-white p-6 shadow-md xl:col-span-4 dark:bg-darkgray"
          data-testid="menu-usage-detail-panel"
        >
          <div className="mb-4 flex items-center gap-2 text-dark dark:text-white">
            <ToggleLeft size={18} />
            <h2 className="text-lg font-semibold">사용여부·노출기간 편집</h2>
          </div>
          {selected ? (
            <div className="space-y-4">
              <ReadonlyLine label="메뉴" value={selected.menuName} />
              <ReadonlyLine label="URL" value={selected.url ?? "-"} />
              <label className="block text-sm font-medium text-link">
                사용여부
                <select
                  className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
                  value={form.systemUseYn}
                  onChange={(event) =>
                    setForm((value) => ({
                      ...value,
                      systemUseYn: event.target.value as SystemUseYn,
                    }))
                  }
                  data-testid="menu-usage-system-use-select"
                >
                  <option value="Y">사용</option>
                  <option value="N">미사용</option>
                </select>
              </label>
              <DateTimeInput
                label="노출 시작"
                value={form.exposureStartAt}
                onChange={(value) =>
                  setForm((state) => ({ ...state, exposureStartAt: value }))
                }
                error={fieldErrors.exposureStartAt}
                testId="menu-usage-start-input"
              />
              <DateTimeInput
                label="노출 종료"
                value={form.exposureEndAt}
                onChange={(value) =>
                  setForm((state) => ({ ...state, exposureEndAt: value }))
                }
                error={fieldErrors.exposureEndAt}
                testId="menu-usage-end-input"
              />
              <label className="block text-sm font-medium text-link">
                변경 사유
                <textarea
                  className="mt-2 min-h-24 w-full rounded-md border border-ld px-3 py-2 text-sm"
                  value={form.changeReason}
                  onChange={(event) =>
                    setForm((value) => ({
                      ...value,
                      changeReason: event.target.value,
                    }))
                  }
                  data-testid="menu-usage-change-reason-textarea"
                />
                {fieldErrors.changeReason ? (
                  <p className="mt-1 text-xs text-error">
                    {fieldErrors.changeReason}
                  </p>
                ) : null}
              </label>
              <div className="flex gap-2">
                <button
                  className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
                  disabled={saving}
                  onClick={() => void save()}
                  type="button"
                  data-testid="menu-usage-save-button"
                >
                  {saving ? "저장 중" : "저장"}
                </button>
                <button
                  className="rounded-md border border-ld px-4 py-2 text-sm font-semibold text-link"
                  onClick={resetForm}
                  type="button"
                  data-testid="menu-usage-cancel-button"
                >
                  취소
                </button>
              </div>
            </div>
          ) : (
            <EmptyState title="메뉴 사용 설정 행을 선택하세요" />
          )}
        </section>
      </div>
    </section>
  );
}

function ReadonlyLine({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-xs font-semibold text-muted">{label}</p>
      <p className="mt-1 rounded-md bg-lightgray px-3 py-2 text-sm text-dark">
        {value}
      </p>
    </div>
  );
}

function DateTimeInput({
  label,
  value,
  onChange,
  error,
  testId,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  error?: string;
  testId: string;
}) {
  return (
    <label className="block text-sm font-medium text-link">
      {label}
      <input
        className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm"
        type="datetime-local"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        data-testid={testId}
      />
      {error ? <p className="mt-1 text-xs text-error">{error}</p> : null}
    </label>
  );
}

function validateForm(form: FormState) {
  const errors: Record<string, string> = {};
  if (!form.exposureStartAt)
    errors.exposureStartAt = "노출 시작일시를 입력하세요.";
  if (!form.exposureEndAt) errors.exposureEndAt = "노출 종료일시를 입력하세요.";
  if (
    form.exposureStartAt &&
    form.exposureEndAt &&
    form.exposureEndAt < form.exposureStartAt
  ) {
    errors.exposureEndAt = "노출 종료일시는 시작일시보다 빠를 수 없습니다.";
  }
  if (!form.changeReason.trim())
    errors.changeReason = "변경 사유를 입력하세요.";
  return errors;
}

function toFieldErrorMap(fields: ApiErrorField[]) {
  return fields.reduce<Record<string, string>>((accumulator, field) => {
    const normalized = field.field.replace(/^items\[\d+\]\./, "");
    accumulator[normalized] = field.message;
    return accumulator;
  }, {});
}

function toInputDateTime(value: string | undefined | null) {
  if (!value) return "";
  return value.slice(0, 16);
}

function fromInputDateTime(value: string) {
  return value.length === 16 ? `${value}:00` : value;
}

function formatDateTime(value: string | undefined | null) {
  if (!value) return "-";
  return value.replace("T", " ").slice(0, 16);
}
