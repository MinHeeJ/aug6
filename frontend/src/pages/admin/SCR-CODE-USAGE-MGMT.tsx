import type React from "react";
import { ListFilter, RefreshCw, ToggleLeft } from "lucide-react";
import { useEffect, useState } from "react";
import {
  ApiClientError,
  codeUsageApi,
  type ApiErrorField,
  type DetailCodeUsageSetting,
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
  codeValue: string;
  systemUseYn: SystemUseYn;
  validStartDate: string;
  validEndDate: string;
  changeReason: string;
};

const emptyForm: FormState = {
  codeValue: "",
  systemUseYn: "Y",
  validStartDate: "",
  validEndDate: "",
  changeReason: "",
};

export function CodeUsageManagementPage() {
  const [groupId, setGroupId] = useState("COMMON_STATUS");
  const [searchedGroupId, setSearchedGroupId] = useState("COMMON_STATUS");
  const [settings, setSettings] = useState<DetailCodeUsageSetting[]>([]);
  const [selectableOptions, setSelectableOptions] = useState<
    DetailCodeUsageSetting[]
  >([]);
  const [selected, setSelected] = useState<DetailCodeUsageSetting | null>(null);
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

  const load = async (nextGroupId = searchedGroupId) => {
    if (!nextGroupId.trim()) {
      setFieldErrors({ groupId: "코드그룹을 입력하세요." });
      setLoading(false);
      return;
    }
    try {
      setLoading(true);
      setError(null);
      setFieldErrors({});
      setPermissionDenied(false);
      const normalizedGroupId = nextGroupId.trim().toUpperCase();
      const response = await codeUsageApi.listDetailCodeUsageSettings(
        normalizedGroupId,
        {
          page,
          size,
        },
      );
      setSearchedGroupId(normalizedGroupId);
      setGroupId(normalizedGroupId);
      setSettings(response.data?.settings ?? []);
      setSelectableOptions(response.data?.selectableOptions ?? []);
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

  const search = () => {
    setPage(0);
    void load(groupId);
  };

  const selectRow = (setting: DetailCodeUsageSetting) => {
    setSelected(setting);
    setFieldErrors({});
    setSuccessMessage(null);
    setForm({
      codeValue: setting.codeValue,
      systemUseYn: setting.systemUseYn,
      validStartDate: setting.validStartDate ?? "",
      validEndDate: setting.validEndDate ?? "",
      changeReason: "",
    });
  };

  const resetForm = () => {
    if (selected) {
      selectRow(selected);
      return;
    }
    setForm(emptyForm);
  };

  const save = async () => {
    if (!selected || !form.codeValue) return;
    const localErrors = validateForm(form);
    setFieldErrors(localErrors);
    if (Object.keys(localErrors).length > 0) return;
    const confirmed = window.confirm(
      `${selected.codeValue} 코드 사용 설정을 저장합니까?`,
    );
    if (!confirmed) return;
    try {
      setSaving(true);
      setError(null);
      setFieldErrors({});
      await codeUsageApi.saveDetailCodeUsageSettings(searchedGroupId, {
        items: [
          {
            codeValue: form.codeValue,
            systemUseYn: form.systemUseYn,
            validStartDate: form.validStartDate || null,
            validEndDate: form.validEndDate || null,
            changeReason: form.changeReason,
          },
        ],
      });
      setSuccessMessage("저장되었습니다");
      await load(searchedGroupId);
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
        : "코드 사용 설정 정보를 처리하지 못했습니다.",
    );
  };

  if (permissionDenied) {
    return (
      <section data-screen-id="SCR-CODE-USAGE-MGMT">
        <PermissionState
          title="코드 사용 관리 권한이 없습니다"
          message="R09 시스템관리자 또는 해당 메뉴 접근 권한이 필요합니다."
        />
      </section>
    );
  }

  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-CODE-USAGE-MGMT"
      data-testid="code-usage-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6 shadow-none">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-sm text-link">시스템 관리 / 공통코드 관리</p>
            <h1 className="mt-2 text-xl font-semibold text-dark">
              코드 사용 관리
            </h1>
            <p className="mt-2 text-sm text-muted">
              상세코드별 사용여부와 적용기간을 설정합니다. 종료 또는 미사용
              코드는 신규 입력 선택값에서 제외됩니다. 과거 자료 조회에서는
              저장된 코드값의 코드명을 유지합니다.
            </p>
          </div>
          <button
            className="inline-flex h-10 items-center gap-2 rounded-md bg-lightprimary px-4 py-2 text-sm font-semibold text-primary transition-colors hover:bg-primary hover:text-white"
            onClick={() => void load()}
            type="button"
            data-testid="code-usage-refresh-button"
          >
            <RefreshCw size={16} /> 다시 조회
          </button>
        </div>
      </div>

      <div className="sr-only">
        코드 사용 설정을 불러오는 중입니다 조회된 코드 사용 설정이 없습니다 코드
        사용 관리 권한이 없습니다 저장되었습니다 코드값 코드명 사용여부 적용
        시작 적용 종료
      </div>
      {error ? (
        <ErrorState title="코드 사용 설정 처리 오류" message={error} />
      ) : null}
      {successMessage ? <SuccessState title={successMessage} /> : null}

      <section
        className="rounded-md bg-white p-6 shadow-md dark:bg-darkgray"
        data-testid="code-usage-search-panel"
      >
        <div className="mb-4 flex items-center gap-2 text-dark dark:text-white">
          <ListFilter size={18} />
          <h2 className="text-lg font-semibold">검색조건</h2>
        </div>
        <div className="grid gap-4 md:grid-cols-3">
          <label className="text-sm font-medium text-link">
            코드그룹
            <input
              className="mt-2 w-full rounded-md border border-ld px-3 py-2 text-sm uppercase"
              value={groupId}
              onChange={(event) => setGroupId(event.target.value)}
              placeholder="예: COMMON_STATUS"
              data-testid="code-usage-group-id-input"
            />
            {fieldErrors.groupId ? (
              <p className="mt-1 text-xs text-error">{fieldErrors.groupId}</p>
            ) : null}
          </label>
          <div className="flex items-end gap-2">
            <button
              className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
              onClick={search}
              type="button"
              data-testid="code-usage-search-button"
            >
              조회
            </button>
            <button
              className="rounded-md border border-ld px-4 py-2 text-sm font-semibold text-link"
              onClick={() => setGroupId("COMMON_STATUS")}
              type="button"
              data-testid="code-usage-reset-button"
            >
              조건 초기화
            </button>
          </div>
        </div>
      </section>

      <div className="grid grid-cols-12 gap-6">
        <section
          className="col-span-12 rounded-md bg-white p-6 shadow-md xl:col-span-8 dark:bg-darkgray"
          data-testid="code-usage-list-panel"
        >
          <div className="mb-4 flex items-center justify-between gap-2">
            <h2 className="text-lg font-semibold text-dark dark:text-white">
              코드 사용 설정 목록
            </h2>
            <span className="text-sm text-muted">총 {totalElements}건</span>
          </div>
          {loading ? (
            <LoadingState title="코드 사용 설정을 불러오는 중입니다" />
          ) : settings.length === 0 ? (
            <EmptyState title="조회된 코드 사용 설정이 없습니다" />
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-ld text-sm">
                <thead className="bg-lightgray text-left text-xs uppercase text-muted">
                  <tr>
                    <th className="px-4 py-3">코드값</th>
                    <th className="px-4 py-3">코드명</th>
                    <th className="px-4 py-3">사용여부</th>
                    <th className="px-4 py-3">적용 시작</th>
                    <th className="px-4 py-3">적용 종료</th>
                    <th className="px-4 py-3">신규 입력 선택값</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-ld">
                  {settings.map((setting) => (
                    <tr
                      key={`${setting.groupId}-${setting.codeValue}`}
                      className={`cursor-pointer hover:bg-lightprimary/40 ${selected?.codeValue === setting.codeValue ? "bg-lightprimary/70" : ""}`}
                      onClick={() => selectRow(setting)}
                      data-testid={`code-usage-row-${setting.codeValue.toLowerCase()}`}
                    >
                      <td className="px-4 py-3 font-medium text-dark">
                        {setting.codeValue}
                      </td>
                      <td className="px-4 py-3 text-dark">
                        {setting.codeName}
                      </td>
                      <td className="px-4 py-3">
                        {setting.systemUseYn === "Y" ? "사용" : "미사용"}
                      </td>
                      <td className="px-4 py-3 text-muted">
                        {formatDate(setting.validStartDate)}
                      </td>
                      <td className="px-4 py-3 text-muted">
                        {formatDate(setting.validEndDate)}
                      </td>
                      <td className="px-4 py-3 text-muted">
                        {setting.selectableForNewInput ? "포함" : "제외"}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          <div
            className="mt-4 rounded-md bg-lightgray p-4 text-sm text-muted"
            data-testid="code-usage-option-summary"
          >
            신규 입력 선택 가능 코드:{" "}
            {selectableOptions.length
              ? selectableOptions
                  .map((option) => `${option.codeValue}(${option.codeName})`)
                  .join(", ")
              : "없음"}
          </div>
          <div className="mt-4 flex items-center justify-end gap-2 text-sm">
            <button
              className="rounded-md border border-ld px-3 py-1"
              disabled={page === 0}
              onClick={() => setPage((value) => Math.max(value - 1, 0))}
              type="button"
              data-testid="code-usage-prev-page-button"
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
              data-testid="code-usage-next-page-button"
            >
              다음
            </button>
          </div>
        </section>

        <section
          className="col-span-12 rounded-md bg-white p-6 shadow-md xl:col-span-4 dark:bg-darkgray"
          data-testid="code-usage-detail-panel"
        >
          <div className="mb-4 flex items-center gap-2 text-dark dark:text-white">
            <ToggleLeft size={18} />
            <h2 className="text-lg font-semibold">사용여부·적용기간 편집</h2>
          </div>
          {selected ? (
            <div className="space-y-4">
              <ReadonlyLine label="코드값" value={selected.codeValue} />
              <ReadonlyLine label="코드명" value={selected.codeName} />
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
                  data-testid="code-usage-system-use-select"
                >
                  <option value="Y">사용</option>
                  <option value="N">미사용</option>
                </select>
              </label>
              <DateInput
                label="적용 시작"
                value={form.validStartDate}
                onChange={(value) =>
                  setForm((state) => ({ ...state, validStartDate: value }))
                }
                error={fieldErrors.validStartDate}
                testId="code-usage-start-input"
              />
              <DateInput
                label="적용 종료"
                value={form.validEndDate}
                onChange={(value) =>
                  setForm((state) => ({ ...state, validEndDate: value }))
                }
                error={fieldErrors.validEndDate}
                testId="code-usage-end-input"
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
                  data-testid="code-usage-change-reason-textarea"
                />
                {fieldErrors.changeReason ? (
                  <p className="mt-1 text-xs text-error">
                    {fieldErrors.changeReason}
                  </p>
                ) : null}
              </label>
              {fieldErrors.codeName ? (
                <p className="text-xs text-error">{fieldErrors.codeName}</p>
              ) : null}
              <div className="flex gap-2">
                <button
                  className="rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
                  disabled={saving}
                  onClick={() => void save()}
                  type="button"
                  data-testid="code-usage-save-button"
                >
                  {saving ? "저장 중" : "저장"}
                </button>
                <button
                  className="rounded-md border border-ld px-4 py-2 text-sm font-semibold text-link"
                  onClick={resetForm}
                  type="button"
                  data-testid="code-usage-cancel-button"
                >
                  취소
                </button>
              </div>
            </div>
          ) : (
            <EmptyState title="코드 사용 설정 행을 선택하세요" />
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

function DateInput({
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
        type="date"
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
  if (
    form.validStartDate &&
    form.validEndDate &&
    form.validEndDate < form.validStartDate
  ) {
    errors.validEndDate = "적용 종료일은 시작일보다 빠를 수 없습니다.";
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

function formatDate(value: string | undefined | null) {
  if (!value) return "-";
  return value;
}
