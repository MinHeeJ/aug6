import { useEffect, useState } from "react";
import { RefreshCw, Save, Search, ShieldCheck } from "lucide-react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

export type FilePolicy = {
  filePolicyId?: number;
  businessType: string;
  allowedExtensions: string;
  maxFileSizeMb: number;
  maxFilesPerItem: number;
  maxTotalSizeMb?: number | null;
  maxFilenameLength: number;
  malwareScanEnabled: "Y" | "N";
  updatedAt?: string;
};

export type FilePolicyForm = {
  businessType: string;
  allowedExtensions: string;
  maxFileSizeMb: number;
  maxFilesPerItem: number;
  maxTotalSizeMb?: number;
  maxFilenameLength: number;
  malwareScanEnabled: boolean;
};

type FilePolicySearchResponse = {
  policies: FilePolicy[];
  page: number;
  size: number;
  totalElements: number;
};

type FilePolicyListParams = {
  page?: number;
  size?: 20 | 50 | 100;
  businessType?: string;
};

type ScreenStatus =
  | "idle"
  | "loading"
  | "empty"
  | "loaded"
  | "error"
  | "permission"
  | "success";

type FilePolicyState = {
  status: ScreenStatus;
  policies: FilePolicy[];
  totalElements: number;
  message?: string;
};

type FilePolicyAction =
  | { type: "loading" }
  | { type: "loaded"; policies: FilePolicy[]; totalElements: number }
  | { type: "error"; message: string }
  | { type: "permission" }
  | { type: "success"; message: string };

const emptyForm: FilePolicyForm = {
  businessType: "",
  allowedExtensions: "",
  maxFileSizeMb: 20,
  maxFilesPerItem: 5,
  maxTotalSizeMb: 100,
  maxFilenameLength: 120,
  malwareScanEnabled: true,
};

export function getFilePolicyRouteContract() {
  return {
    route: "/admin/file-policies",
    screenId: "SCR-FILE-POLICY-MGMT",
    operations: ["listFilePolicies", "saveFilePolicy"],
    menuPath: "시스템 관리 > 시스템 환경설정 > 파일정책 관리",
  } as const;
}

export function createEmptyFilePolicyState(): FilePolicyState {
  return { status: "idle", policies: [], totalElements: 0 };
}

export function reduceFilePolicyState(
  state: FilePolicyState,
  action: FilePolicyAction,
): FilePolicyState {
  switch (action.type) {
    case "loading":
      return { ...state, status: "loading", message: undefined };
    case "loaded":
      return {
        ...state,
        status: action.policies.length === 0 ? "empty" : "loaded",
        policies: action.policies,
        totalElements: action.totalElements,
        message: undefined,
      };
    case "error":
      return { ...state, status: "error", message: action.message };
    case "permission":
      return { ...state, status: "permission", message: "권한 없음" };
    case "success":
      return { ...state, status: "success", message: action.message };
    default:
      return state;
  }
}

export function validateFilePolicyForm(form: FilePolicyForm) {
  const errors: Partial<Record<keyof FilePolicyForm, string>> = {};
  if (!form.businessType.trim()) errors.businessType = "업무구분을 입력하세요.";
  if (!form.allowedExtensions.trim())
    errors.allowedExtensions = "허용 확장자를 입력하세요.";
  if (form.maxFileSizeMb < 1)
    errors.maxFileSizeMb = "단일 파일 최대용량은 1MB 이상이어야 합니다.";
  if (form.maxFilesPerItem < 1)
    errors.maxFilesPerItem = "건당 첨부개수는 1개 이상이어야 합니다.";
  if (form.maxTotalSizeMb !== undefined && form.maxTotalSizeMb < 1)
    errors.maxTotalSizeMb = "전체용량은 1MB 이상이어야 합니다.";
  if (form.maxFilenameLength < 1)
    errors.maxFilenameLength = "파일명 길이는 1자 이상이어야 합니다.";
  return errors;
}

export const filePolicyApi = {
  paths: {
    list(params: FilePolicyListParams = {}) {
      const query = new URLSearchParams();
      query.set("page", String(params.page ?? 0));
      query.set("size", String(params.size ?? 20));
      if (params.businessType?.trim())
        query.set("businessType", params.businessType.trim());
      return `/api/admin/file-policies?${query.toString()}` as `/api/${string}`;
    },
    save() {
      return "/api/admin/file-policies-save" as const;
    },
  },
  list(params: FilePolicyListParams = {}) {
    return apiRequest<FilePolicySearchResponse>(
      filePolicyApi.paths.list(params),
    );
  },
  save(form: FilePolicyForm) {
    return apiRequest<FilePolicy>(filePolicyApi.paths.save(), {
      method: "PUT",
      body: JSON.stringify({
        businessType: form.businessType,
        allowedExtensions: form.allowedExtensions,
        maxFileSizeMb: form.maxFileSizeMb,
        maxFilesPerItem: form.maxFilesPerItem,
        maxTotalSizeMb: form.maxTotalSizeMb,
        maxFilenameLength: form.maxFilenameLength,
        malwareScanEnabled: form.malwareScanEnabled,
      }),
    });
  },
};

export function FilePolicyManagementPage() {
  const [filter, setFilter] = useState("");
  const [pageSize, setPageSize] = useState<20 | 50 | 100>(20);
  const [state, setState] = useState<FilePolicyState>(
    createEmptyFilePolicyState(),
  );
  const [form, setForm] = useState<FilePolicyForm>(emptyForm);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const loadPolicies = async (nextFilter = filter, nextSize = pageSize) => {
    setState((current) => reduceFilePolicyState(current, { type: "loading" }));
    try {
      const response = await filePolicyApi.list({
        page: 0,
        size: nextSize,
        businessType: nextFilter,
      });
      const data = response.data ?? {
        policies: [],
        totalElements: 0,
        page: 0,
        size: nextSize,
      };
      setState((current) =>
        reduceFilePolicyState(current, {
          type: "loaded",
          policies: data.policies,
          totalElements: data.totalElements,
        }),
      );
    } catch (caught) {
      handleApiError(caught);
    }
  };

  useEffect(() => {
    void loadPolicies("", 20);
  }, []);

  const selectPolicy = (policy: FilePolicy) => {
    setForm({
      businessType: policy.businessType,
      allowedExtensions: policy.allowedExtensions,
      maxFileSizeMb: policy.maxFileSizeMb,
      maxFilesPerItem: policy.maxFilesPerItem,
      maxTotalSizeMb: policy.maxTotalSizeMb ?? undefined,
      maxFilenameLength: policy.maxFilenameLength,
      malwareScanEnabled: policy.malwareScanEnabled === "Y",
    });
    setFieldErrors({});
  };

  const savePolicy = async () => {
    const errors = validateFilePolicyForm(form);
    setFieldErrors(errors as Record<string, string>);
    if (Object.keys(errors).length > 0) return;
    if (!window.confirm("파일정책을 저장하시겠습니까?")) return;
    try {
      setFieldErrors({});
      const response = await filePolicyApi.save(form);
      if (response.data) selectPolicy(response.data);
      setState((current) =>
        reduceFilePolicyState(current, {
          type: "success",
          message: "파일정책이 저장되었습니다.",
        }),
      );
      await loadPolicies();
    } catch (caught) {
      handleApiError(caught);
    }
  };

  const handleApiError = (caught: unknown) => {
    if (caught instanceof ApiClientError && caught.status === 403) {
      setState((current) =>
        reduceFilePolicyState(current, { type: "permission" }),
      );
      return;
    }
    if (caught instanceof ApiClientError && caught.apiError?.fields) {
      setFieldErrors(
        Object.fromEntries(
          caught.apiError.fields.map((field) => [field.field, field.message]),
        ),
      );
    }
    const message =
      caught instanceof Error
        ? caught.message
        : "파일정책을 처리하지 못했습니다.";
    setState((current) =>
      reduceFilePolicyState(current, { type: "error", message }),
    );
  };

  return (
    <section
      data-testid="file-policy-management-page"
      data-screen-id="SCR-FILE-POLICY-MGMT"
      className="space-y-6"
    >
      <header className="rounded-md bg-lightsecondary p-6 shadow-none">
        <p className="text-xs font-semibold uppercase tracking-wide text-primary">
          SCR-FILE-POLICY-MGMT
        </p>
        <h1 className="mt-2 text-2xl font-semibold text-dark">파일정책 관리</h1>
        <p className="mt-2 text-sm text-muted">
          업무별 허용 확장자, 용량, 첨부 개수, 파일명 길이, 악성파일 검사 적용
          여부를 중앙에서 관리합니다.
        </p>
      </header>

      <section className="rounded-md bg-white p-5 shadow-md">
        <div className="flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
          <label className="text-sm font-medium text-dark">
            검색어
            <input
              data-testid="file-policy-filter-input"
              className="mt-1 w-full rounded border border-border px-3 py-2 text-sm"
              value={filter}
              onChange={(event) => setFilter(event.target.value)}
              placeholder="업무구분"
            />
          </label>
          <label className="text-sm font-medium text-dark">
            목록 표시 건수
            <select
              data-testid="file-policy-size-select"
              className="mt-1 rounded border border-border px-3 py-2 text-sm"
              value={pageSize}
              onChange={(event) => {
                const next = Number(event.target.value) as 20 | 50 | 100;
                setPageSize(next);
                void loadPolicies(filter, next);
              }}
            >
              <option value={20}>20건</option>
              <option value={50}>50건</option>
              <option value={100}>100건</option>
            </select>
          </label>
          <button
            data-testid="file-policy-search-button"
            type="button"
            onClick={() => void loadPolicies()}
            className="inline-flex items-center justify-center gap-2 rounded bg-primary px-4 py-2 text-sm font-semibold text-white"
          >
            <Search className="h-4 w-4" /> 조회
          </button>
        </div>
      </section>

      {state.status === "loading" && (
        <LoadingState
          title="파일정책 조회 중"
          message="업무별 파일정책을 불러오고 있습니다."
        />
      )}
      {state.status === "empty" && (
        <EmptyState
          title="파일정책 없음"
          message="조회 조건에 해당하는 파일정책이 없습니다."
        />
      )}
      {state.status === "error" && (
        <ErrorState
          title="처리 오류"
          message={state.message ?? "파일정책 처리 중 오류가 발생했습니다."}
        />
      )}
      {state.status === "permission" && (
        <PermissionState
          title="권한이 없습니다"
          message="파일정책 관리 권한이 없습니다."
        />
      )}
      {state.status === "success" && (
        <SuccessState
          title="처리 완료"
          message={state.message ?? "파일정책이 저장되었습니다."}
        />
      )}

      <section className="grid gap-6 xl:grid-cols-[1.2fr_1fr]">
        <div className="rounded-md bg-white p-5 shadow-md">
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-lg font-semibold text-dark">정책 목록</h2>
            <span className="text-xs text-muted">
              총 {state.totalElements}건
            </span>
          </div>
          <div className="overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead className="bg-lightsecondary text-xs text-muted">
                <tr>
                  <th className="px-3 py-2">업무구분</th>
                  <th className="px-3 py-2">허용 확장자</th>
                  <th className="px-3 py-2">단일/전체 용량</th>
                  <th className="px-3 py-2">검사</th>
                </tr>
              </thead>
              <tbody>
                {state.policies.map((policy) => (
                  <tr
                    data-testid="file-policy-row"
                    key={policy.businessType}
                    className="cursor-pointer border-b border-border hover:bg-lightsecondary"
                    onClick={() => selectPolicy(policy)}
                  >
                    <td className="px-3 py-3 font-medium text-dark">
                      {policy.businessType}
                    </td>
                    <td className="px-3 py-3 text-muted">
                      {policy.allowedExtensions}
                    </td>
                    <td className="px-3 py-3 text-muted">
                      {policy.maxFileSizeMb}MB /{" "}
                      {policy.maxTotalSizeMb ?? "제한 없음"}MB
                    </td>
                    <td className="px-3 py-3 text-muted">
                      {policy.malwareScanEnabled === "Y" ? "적용" : "미적용"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        <form
          className="rounded-md bg-white p-5 shadow-md"
          onSubmit={(event) => {
            event.preventDefault();
            void savePolicy();
          }}
        >
          <div className="mb-4 flex items-center gap-2">
            <ShieldCheck className="h-5 w-5 text-primary" />
            <h2 className="text-lg font-semibold text-dark">정책 상세/저장</h2>
          </div>
          <RequiredTextInput
            id="file-policy-business-type-input"
            label="업무구분"
            value={form.businessType}
            error={fieldErrors.businessType}
            onChange={(value) => setForm({ ...form, businessType: value })}
          />
          <RequiredTextInput
            id="file-policy-allowed-extensions-input"
            label="허용 확장자"
            value={form.allowedExtensions}
            error={fieldErrors.allowedExtensions}
            onChange={(value) => setForm({ ...form, allowedExtensions: value })}
            placeholder="pdf,docx,png,zip"
          />
          <NumberInput
            id="file-policy-max-file-size-input"
            label="단일 파일 최대용량(MB)"
            value={form.maxFileSizeMb}
            error={fieldErrors.maxFileSizeMb}
            onChange={(value) => setForm({ ...form, maxFileSizeMb: value })}
            required
          />
          <NumberInput
            id="file-policy-max-files-input"
            label="건당 첨부개수"
            value={form.maxFilesPerItem}
            error={fieldErrors.maxFilesPerItem}
            onChange={(value) => setForm({ ...form, maxFilesPerItem: value })}
            required
          />
          <NumberInput
            id="file-policy-max-total-size-input"
            label="전체용량(MB)"
            value={form.maxTotalSizeMb ?? 0}
            error={fieldErrors.maxTotalSizeMb}
            onChange={(value) => setForm({ ...form, maxTotalSizeMb: value })}
          />
          <NumberInput
            id="file-policy-max-filename-length-input"
            label="파일명 길이"
            value={form.maxFilenameLength}
            error={fieldErrors.maxFilenameLength}
            onChange={(value) => setForm({ ...form, maxFilenameLength: value })}
            required
          />
          <label className="mb-4 flex items-center gap-2 text-sm text-dark">
            <input
              data-testid="file-policy-malware-checkbox"
              type="checkbox"
              checked={form.malwareScanEnabled}
              onChange={(event) =>
                setForm({ ...form, malwareScanEnabled: event.target.checked })
              }
            />
            악성파일 검사 적용
          </label>
          <div className="flex justify-end gap-2">
            <button
              data-testid="file-policy-reset-button"
              type="button"
              onClick={() => {
                setForm(emptyForm);
                setFieldErrors({});
              }}
              className="inline-flex items-center gap-2 rounded border border-border px-4 py-2 text-sm font-semibold text-dark"
            >
              <RefreshCw className="h-4 w-4" /> 초기화
            </button>
            <button
              data-testid="file-policy-save-button"
              type="submit"
              className="inline-flex items-center gap-2 rounded bg-primary px-4 py-2 text-sm font-semibold text-white"
            >
              <Save className="h-4 w-4" /> 저장
            </button>
          </div>
        </form>
      </section>
    </section>
  );
}

function RequiredTextInput({
  id,
  label,
  value,
  error,
  onChange,
  placeholder,
}: {
  id: string;
  label: string;
  value: string;
  error?: string;
  onChange: (value: string) => void;
  placeholder?: string;
}) {
  return (
    <label className="mb-4 block text-sm font-medium text-dark">
      {label}{" "}
      <span className="text-error" aria-label="필수">
        *
      </span>
      <input
        data-testid={id}
        className="mt-1 w-full rounded border border-border px-3 py-2 text-sm"
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
      />
      {error && <p className="mt-1 text-xs text-error">{error}</p>}
    </label>
  );
}

function NumberInput({
  id,
  label,
  value,
  error,
  onChange,
  required,
}: {
  id: string;
  label: string;
  value: number;
  error?: string;
  onChange: (value: number) => void;
  required?: boolean;
}) {
  return (
    <label className="mb-4 block text-sm font-medium text-dark">
      {label}{" "}
      {required && (
        <span className="text-error" aria-label="필수">
          *
        </span>
      )}
      <input
        data-testid={id}
        type="number"
        min="0"
        className="mt-1 w-full rounded border border-border px-3 py-2 text-sm"
        value={value}
        onChange={(event) => onChange(Number(event.target.value))}
      />
      {error && <p className="mt-1 text-xs text-error">{error}</p>}
    </label>
  );
}
