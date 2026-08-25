import { useEffect, useState } from "react";
import { HelpCircle, RefreshCw, Save, Search } from "lucide-react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

export type HelpContentRow = {
  screenId: string;
  businessDescription: string;
  inputCriteria: string;
  faq: string;
  contact: string;
  createdAt?: string;
  createdBy?: number;
  updatedAt?: string;
  updatedBy?: number;
};

type HelpContentSearchResponse = {
  helpContents: HelpContentRow[];
  page: number;
  size: number;
  totalElements: number;
};

type HelpContentForm = HelpContentRow & {
  changeReason: string;
};

type HelpContentSavePayload = {
  businessDescription: string;
  inputCriteria: string;
  faq: string;
  contact: string;
  changeReason: string;
};

type HelpContentListParams = {
  screenId?: string;
  page?: number;
  size?: number;
};

type ScreenStatus =
  | "idle"
  | "loading"
  | "empty"
  | "loaded"
  | "error"
  | "permission"
  | "success";

type HelpContentManagementState = {
  status: ScreenStatus;
  helpContents: HelpContentRow[];
  message?: string;
};

type HelpContentManagementAction =
  | { type: "loading" }
  | { type: "loaded"; helpContents: HelpContentRow[] }
  | { type: "error"; message: string }
  | { type: "permission" }
  | { type: "success"; message: string };

export function getHelpContentManagementRouteContract() {
  return {
    route: "/admin/help-contents",
    screenId: "SCR-HELP-MGMT",
    operations: ["listHelpContents", "saveHelpContent", "getHelpContent"],
  } as const;
}

export function createEmptyHelpContentManagementState(): HelpContentManagementState {
  return { status: "idle", helpContents: [] };
}

export function reduceHelpContentManagementState(
  state: HelpContentManagementState,
  action: HelpContentManagementAction,
): HelpContentManagementState {
  switch (action.type) {
    case "loading":
      return { ...state, status: "loading", message: undefined };
    case "loaded":
      return {
        ...state,
        status: action.helpContents.length === 0 ? "empty" : "loaded",
        helpContents: action.helpContents,
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

const emptyHelpContent: HelpContentForm = {
  screenId: "",
  businessDescription: "",
  inputCriteria: "",
  faq: "",
  contact: "",
  changeReason: "",
};

export const helpContentManagementApi = {
  uiMessages: {
    saveConfirm(screenId: string) {
      return `${screenId.trim().toUpperCase()} 도움말을 저장하시겠습니까?`;
    },
    saveSuccess: "도움말이 저장되었습니다.",
    error: "도움말 정보를 처리하지 못했습니다.",
  },
  paths: {
    list(params: HelpContentListParams = {}) {
      const query = new URLSearchParams();
      if (params.screenId?.trim())
        query.set("screenId", params.screenId.trim().toUpperCase());
      query.set("page", String(params.page ?? 0));
      query.set("size", String(params.size ?? 20));
      return `/api/admin/help-contents?${query.toString()}` as `/api/${string}`;
    },
    save(screenId: string) {
      return `/api/admin/help-contents/${encodeURIComponent(screenId)}` as `/api/${string}`;
    },
    get(screenId: string) {
      return `/api/help-contents/${encodeURIComponent(screenId)}` as `/api/${string}`;
    },
  },
  toSavePayload(
    form: Pick<
      HelpContentForm,
      | "businessDescription"
      | "inputCriteria"
      | "faq"
      | "contact"
      | "changeReason"
    > &
      Partial<Pick<HelpContentForm, "screenId">>,
  ) {
    return {
      businessDescription: form.businessDescription.trim(),
      inputCriteria: form.inputCriteria.trim(),
      faq: form.faq.trim(),
      contact: form.contact.trim(),
      changeReason: form.changeReason.trim(),
    } satisfies HelpContentSavePayload;
  },
  list(params: HelpContentListParams = {}) {
    return apiRequest<HelpContentSearchResponse>(
      helpContentManagementApi.paths.list(params),
    );
  },
  save(screenId: string, payload: HelpContentSavePayload) {
    return apiRequest<HelpContentRow>(
      helpContentManagementApi.paths.save(screenId),
      {
        method: "PUT",
        body: JSON.stringify(payload),
      },
    );
  },
  get(screenId: string) {
    return apiRequest<HelpContentRow>(
      helpContentManagementApi.paths.get(screenId),
    );
  },
};

export function HelpContentManagementPage() {
  const [screenIdFilter, setScreenIdFilter] = useState("");
  const [pageSize, setPageSize] = useState(20);
  const [state, setState] = useState<HelpContentManagementState>(
    createEmptyHelpContentManagementState(),
  );
  const [selectedHelpContent, setSelectedHelpContent] =
    useState<HelpContentRow | null>(null);
  const [form, setForm] = useState<HelpContentForm>(emptyHelpContent);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [preview, setPreview] = useState<HelpContentRow | null>(null);

  const loadHelpContents = async () => {
    setState((current) =>
      reduceHelpContentManagementState(current, { type: "loading" }),
    );
    setFieldErrors({});
    try {
      const response = await helpContentManagementApi.list({
        screenId: screenIdFilter,
        page: 0,
        size: pageSize,
      });
      const helpContents = response.data?.helpContents ?? [];
      setState((current) =>
        reduceHelpContentManagementState(current, {
          type: "loaded",
          helpContents,
        }),
      );
      if (selectedHelpContent) {
        const refreshed =
          helpContents.find(
            (row) => row.screenId === selectedHelpContent.screenId,
          ) ?? null;
        if (refreshed) applySelectedHelpContent(refreshed);
      }
    } catch (caught) {
      handleApiError(caught);
    }
  };

  useEffect(() => {
    void loadHelpContents();
  }, []);

  const applySelectedHelpContent = (row: HelpContentRow) => {
    setSelectedHelpContent(row);
    setForm({ ...row, changeReason: "" });
    setFieldErrors({});
    setPreview(null);
  };

  const startNew = () => {
    setSelectedHelpContent(null);
    setForm(emptyHelpContent);
    setPreview(null);
    setFieldErrors({});
  };

  const resetFilters = () => {
    setScreenIdFilter("");
    setPageSize(20);
    setSelectedHelpContent(null);
    setForm(emptyHelpContent);
    setPreview(null);
    setState(createEmptyHelpContentManagementState());
  };

  const cancelEdit = () => {
    if (selectedHelpContent) applySelectedHelpContent(selectedHelpContent);
    else setForm(emptyHelpContent);
    setFieldErrors({});
  };

  const validateLocal = () => {
    const next: Record<string, string> = {};
    if (!form.screenId.trim()) next.screenId = "화면ID는 필수입니다.";
    if (!form.businessDescription.trim())
      next.businessDescription = "업무 설명은 필수입니다.";
    if (!form.inputCriteria.trim())
      next.inputCriteria = "입력 기준은 필수입니다.";
    if (!form.changeReason.trim())
      next.changeReason = "변경 사유는 필수입니다.";
    setFieldErrors(next);
    return Object.keys(next).length === 0;
  };

  const saveHelpContent = async () => {
    if (!validateLocal()) return;
    if (
      !window.confirm(
        helpContentManagementApi.uiMessages.saveConfirm(form.screenId),
      )
    )
      return;
    try {
      setFieldErrors({});
      const response = await helpContentManagementApi.save(
        form.screenId.trim().toUpperCase(),
        helpContentManagementApi.toSavePayload(form),
      );
      const saved = response.data ?? {
        ...form,
        screenId: form.screenId.trim().toUpperCase(),
      };
      applySelectedHelpContent(saved);
      setState((current) =>
        reduceHelpContentManagementState(current, {
          type: "success",
          message: helpContentManagementApi.uiMessages.saveSuccess,
        }),
      );
      await loadHelpContents();
    } catch (caught) {
      handleApiError(caught);
    }
  };

  const loadBusinessScreenHelp = async () => {
    const targetScreenId = form.screenId.trim().toUpperCase();
    if (!targetScreenId) {
      setFieldErrors({ screenId: "도움말을 조회할 화면ID를 입력하세요." });
      return;
    }
    try {
      const response = await helpContentManagementApi.get(targetScreenId);
      setPreview(response.data ?? null);
      setState((current) =>
        reduceHelpContentManagementState(current, {
          type: "success",
          message: "업무화면 도움말을 조회했습니다.",
        }),
      );
    } catch (caught) {
      setPreview(null);
      handleApiError(caught);
    }
  };

  const handleApiError = (caught: unknown) => {
    if (caught instanceof ApiClientError && caught.status === 403) {
      setState((current) =>
        reduceHelpContentManagementState(current, { type: "permission" }),
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
      caught instanceof ApiClientError
        ? caught.message
        : helpContentManagementApi.uiMessages.error;
    setState((current) =>
      reduceHelpContentManagementState(current, { type: "error", message }),
    );
  };

  if (state.status === "permission") {
    return (
      <PermissionState
        title="도움말 관리 권한이 없습니다"
        message="R09 시스템관리자 권한 또는 메뉴 접근권한을 확인하세요."
      />
    );
  }

  return (
    <section
      data-testid="help-content-management-screen"
      data-screen-id="SCR-HELP-MGMT"
      className="space-y-6"
    >
      <div className="rounded-md bg-lightsecondary p-6 shadow-none">
        <p className="text-sm text-muted">
          시스템 관리 &gt; 공지·도움말 관리 &gt; 도움말 관리
        </p>
        <div className="mt-2 flex items-center gap-3">
          <HelpCircle className="h-6 w-6 text-primary" aria-hidden />
          <div>
            <h1 className="text-xl font-semibold text-dark">도움말 관리</h1>
            <p className="mt-1 text-sm text-muted">
              화면ID별 업무 설명, 입력 기준, FAQ, 연락처를 관리합니다.
            </p>
          </div>
        </div>
      </div>

      {state.status === "loading" && (
        <LoadingState
          title="도움말 조회 중"
          message="화면ID별 도움말 목록을 불러오고 있습니다."
        />
      )}
      {state.status === "error" && (
        <ErrorState
          title="도움말 처리 오류"
          message={state.message ?? "오류가 발생했습니다."}
        />
      )}
      {state.status === "success" && (
        <SuccessState
          title="처리 완료"
          message={state.message ?? "처리되었습니다."}
        />
      )}

      <section className="rounded-md border border-ld bg-white p-5 shadow-md">
        <div className="grid gap-4 lg:grid-cols-4">
          <label className="text-sm font-medium text-dark lg:col-span-3">
            화면ID
            <input
              data-testid="help-screen-id-filter"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2"
              value={screenIdFilter}
              onChange={(event) =>
                setScreenIdFilter(event.target.value.toUpperCase())
              }
              placeholder="예: SCR-USER-MGMT"
            />
          </label>
          <label className="text-sm font-medium text-dark">
            표시 건수
            <select
              data-testid="help-page-size-select"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2"
              value={pageSize}
              onChange={(event) => setPageSize(Number(event.target.value))}
            >
              <option value={20}>20건</option>
              <option value={50}>50건</option>
              <option value={100}>100건</option>
            </select>
          </label>
        </div>
        <div className="mt-4 flex flex-wrap gap-2">
          <button
            data-testid="help-search-button"
            type="button"
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void loadHelpContents()}
          >
            <Search className="h-4 w-4" />
            조회
          </button>
          <button
            data-testid="help-reset-button"
            type="button"
            className="inline-flex items-center gap-2 rounded-md border border-ld px-4 py-2 text-sm font-semibold text-dark"
            onClick={resetFilters}
          >
            <RefreshCw className="h-4 w-4" />
            조건 초기화
          </button>
          <button
            data-testid="help-new-button"
            type="button"
            className="rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary"
            onClick={startNew}
          >
            신규 등록
          </button>
        </div>
      </section>

      <div className="grid gap-6 xl:grid-cols-12">
        <section className="rounded-md border border-ld bg-white p-5 shadow-md xl:col-span-7">
          <h2 className="text-lg font-semibold text-dark">도움말 목록</h2>
          <div className="mt-4 overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead className="bg-lightgray text-muted">
                <tr>
                  <th className="px-3 py-2">화면ID</th>
                  <th className="px-3 py-2">업무 설명</th>
                  <th className="px-3 py-2">입력 기준</th>
                  <th className="px-3 py-2">FAQ</th>
                  <th className="px-3 py-2">연락처</th>
                </tr>
              </thead>
              <tbody>
                {state.helpContents.map((row) => (
                  <tr
                    key={row.screenId}
                    data-testid={`help-list-row-${row.screenId.toLowerCase().replace(/[^a-z0-9]+/g, "-")}`}
                    className={`cursor-pointer border-t border-ld ${selectedHelpContent?.screenId === row.screenId ? "bg-lightprimary" : ""}`}
                    onClick={() => applySelectedHelpContent(row)}
                  >
                    <td className="px-3 py-3 font-semibold text-primary">
                      {row.screenId}
                    </td>
                    <td className="px-3 py-3">{row.businessDescription}</td>
                    <td className="px-3 py-3">{row.inputCriteria}</td>
                    <td className="px-3 py-3">{row.faq || "-"}</td>
                    <td className="px-3 py-3">{row.contact || "-"}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {state.status === "empty" && (
            <EmptyState
              title="조회된 도움말이 없습니다"
              message="다른 화면ID를 조회하거나 신규 도움말을 등록하세요."
            />
          )}
        </section>

        <section className="rounded-md border border-ld bg-white p-5 shadow-md xl:col-span-5">
          <h2 className="text-lg font-semibold text-dark">도움말 상세/편집</h2>
          <div className="mt-4 space-y-4">
            <label className="block text-sm font-medium text-dark">
              화면ID <span className="text-error">*</span>
              <input
                data-testid="help-screen-id-input"
                className="mt-2 w-full rounded-md border border-ld px-3 py-2"
                value={form.screenId}
                onChange={(event) =>
                  setForm({
                    ...form,
                    screenId: event.target.value.toUpperCase(),
                  })
                }
                placeholder="SCR-USER-MGMT"
              />
              {fieldErrors.screenId && (
                <p className="mt-1 text-xs text-error">
                  {fieldErrors.screenId}
                </p>
              )}
            </label>
            <label className="block text-sm font-medium text-dark">
              업무 설명 <span className="text-error">*</span>
              <textarea
                data-testid="help-business-description-textarea"
                className="mt-2 min-h-24 w-full rounded-md border border-ld px-3 py-2"
                value={form.businessDescription}
                onChange={(event) =>
                  setForm({ ...form, businessDescription: event.target.value })
                }
              />
              {fieldErrors.businessDescription && (
                <p className="mt-1 text-xs text-error">
                  {fieldErrors.businessDescription}
                </p>
              )}
            </label>
            <label className="block text-sm font-medium text-dark">
              입력 기준 <span className="text-error">*</span>
              <textarea
                data-testid="help-input-criteria-textarea"
                className="mt-2 min-h-24 w-full rounded-md border border-ld px-3 py-2"
                value={form.inputCriteria}
                onChange={(event) =>
                  setForm({ ...form, inputCriteria: event.target.value })
                }
              />
              {fieldErrors.inputCriteria && (
                <p className="mt-1 text-xs text-error">
                  {fieldErrors.inputCriteria}
                </p>
              )}
            </label>
            <label className="block text-sm font-medium text-dark">
              FAQ
              <textarea
                data-testid="help-faq-textarea"
                className="mt-2 min-h-20 w-full rounded-md border border-ld px-3 py-2"
                value={form.faq}
                onChange={(event) =>
                  setForm({ ...form, faq: event.target.value })
                }
              />
              {fieldErrors.faq && (
                <p className="mt-1 text-xs text-error">{fieldErrors.faq}</p>
              )}
            </label>
            <label className="block text-sm font-medium text-dark">
              연락처
              <input
                data-testid="help-contact-input"
                className="mt-2 w-full rounded-md border border-ld px-3 py-2"
                value={form.contact}
                onChange={(event) =>
                  setForm({ ...form, contact: event.target.value })
                }
              />
              {fieldErrors.contact && (
                <p className="mt-1 text-xs text-error">{fieldErrors.contact}</p>
              )}
            </label>
            <label className="block text-sm font-medium text-dark">
              변경 사유 <span className="text-error">*</span>
              <input
                data-testid="help-change-reason-input"
                className="mt-2 w-full rounded-md border border-ld px-3 py-2"
                value={form.changeReason}
                onChange={(event) =>
                  setForm({ ...form, changeReason: event.target.value })
                }
              />
              {fieldErrors.changeReason && (
                <p className="mt-1 text-xs text-error">
                  {fieldErrors.changeReason}
                </p>
              )}
            </label>
            <div className="rounded-md bg-lightgray p-3 text-xs text-muted">
              최초 입력자: {form.createdBy ?? "-"} · 최종 수정자:{" "}
              {form.updatedBy ?? "-"}
            </div>
            <div className="flex flex-wrap gap-2">
              <button
                data-testid="help-save-button"
                type="button"
                className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
                onClick={() => void saveHelpContent()}
              >
                <Save className="h-4 w-4" />
                저장
              </button>
              <button
                data-testid="help-preview-button"
                type="button"
                className="rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary"
                onClick={() => void loadBusinessScreenHelp()}
              >
                업무화면 도움말 보기
              </button>
              <button
                data-testid="help-cancel-button"
                type="button"
                className="rounded-md border border-ld px-4 py-2 text-sm font-semibold text-dark"
                onClick={cancelEdit}
              >
                취소
              </button>
            </div>
          </div>
          {preview && (
            <aside
              data-testid="help-preview-panel"
              className="mt-5 rounded-md border border-ld bg-lightgray p-4 text-sm"
            >
              <h3 className="font-semibold text-dark">
                업무화면 도움말: {preview.screenId}
              </h3>
              <p className="mt-2 text-muted">{preview.businessDescription}</p>
              <p className="mt-2 text-muted">
                입력 기준: {preview.inputCriteria}
              </p>
              {preview.faq && (
                <p className="mt-2 text-muted">FAQ: {preview.faq}</p>
              )}
              {preview.contact && (
                <p className="mt-2 text-muted">문의: {preview.contact}</p>
              )}
            </aside>
          )}
        </section>
      </div>
    </section>
  );
}
