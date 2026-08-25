import { useEffect, useState } from "react";
import { MessageSquare, RefreshCw, Save, Search } from "lucide-react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

export type MessageType =
  | "SAVE"
  | "DELETE"
  | "APPROVAL"
  | "REJECT"
  | "ERROR"
  | "SESSION_EXPIRED";

export type MessageCodeRow = {
  messageCode: string;
  messageType: MessageType;
  userMessage: string;
  createdAt?: string;
  createdBy?: number;
  updatedAt?: string;
  updatedBy?: number;
};

type MessageSearchResponse = {
  messages: MessageCodeRow[];
  page: number;
  size: number;
  totalElements: number;
};

type MessageForm = MessageCodeRow & {
  changeReason: string;
};

type MessageSavePayload = {
  messageType: MessageType;
  userMessage: string;
  changeReason: string;
};

type MessageListParams = {
  messageType?: string;
  messageCode?: string;
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

type MessageManagementState = {
  status: ScreenStatus;
  messages: MessageCodeRow[];
  message?: string;
};

type MessageManagementAction =
  | { type: "loading" }
  | { type: "loaded"; messages: MessageCodeRow[] }
  | { type: "error"; message: string }
  | { type: "permission" }
  | { type: "success"; message: string };

export function getMessageManagementRouteContract() {
  return {
    route: "/admin/messages",
    screenId: "SCR-MESSAGE-MGMT",
    operations: ["listMessages", "saveMessage", "getMessageText"],
  } as const;
}

export function createEmptyMessageManagementState(): MessageManagementState {
  return { status: "idle", messages: [] };
}

export function reduceMessageManagementState(
  state: MessageManagementState,
  action: MessageManagementAction,
): MessageManagementState {
  switch (action.type) {
    case "loading":
      return { ...state, status: "loading", message: undefined };
    case "loaded":
      return {
        ...state,
        status: action.messages.length === 0 ? "empty" : "loaded",
        messages: action.messages,
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

const emptyMessage: MessageForm = {
  messageCode: "",
  messageType: "SAVE",
  userMessage: "",
  changeReason: "",
};

const MESSAGE_TYPES: Array<{ value: MessageType; label: string }> = [
  { value: "SAVE", label: "저장" },
  { value: "DELETE", label: "삭제" },
  { value: "APPROVAL", label: "승인" },
  { value: "REJECT", label: "반려" },
  { value: "ERROR", label: "오류" },
  { value: "SESSION_EXPIRED", label: "세션만료" },
];

export const messageManagementApi = {
  uiMessages: {
    saveConfirm(messageCode: string) {
      return `${messageCode.trim()} 메시지를 저장하시겠습니까?`;
    },
    saveSuccess: "메시지가 저장되었습니다.",
    error: "메시지 정보를 처리하지 못했습니다.",
  },
  paths: {
    list(params: MessageListParams = {}) {
      const query = new URLSearchParams();
      if (params.messageType?.trim())
        query.set("messageType", params.messageType.trim());
      if (params.messageCode?.trim())
        query.set("messageCode", params.messageCode.trim());
      query.set("page", String(params.page ?? 0));
      query.set("size", String(params.size ?? 20));
      return `/api/admin/system-settings/messages?${query.toString()}` as `/api/${string}`;
    },
    save(messageCode: string) {
      return `/api/admin/system-settings/messages/${encodeURIComponent(messageCode)}` as `/api/${string}`;
    },
    text(messageCode: string) {
      return `/api/system/messages/${encodeURIComponent(messageCode)}` as `/api/${string}`;
    },
  },
  toSavePayload(
    form: Pick<MessageForm, "messageType" | "userMessage" | "changeReason"> &
      Partial<Pick<MessageForm, "messageCode">>,
  ) {
    return {
      messageType: form.messageType,
      userMessage: form.userMessage.trim(),
      changeReason: form.changeReason.trim(),
    } satisfies MessageSavePayload;
  },
  list(params: MessageListParams = {}) {
    return apiRequest<MessageSearchResponse>(
      messageManagementApi.paths.list(params),
    );
  },
  save(messageCode: string, payload: MessageSavePayload) {
    return apiRequest<MessageCodeRow>(
      messageManagementApi.paths.save(messageCode),
      {
        method: "PUT",
        body: JSON.stringify(payload),
      },
    );
  },
  getText(messageCode: string) {
    return apiRequest<{ messageCode: string; userMessage: string }>(
      messageManagementApi.paths.text(messageCode),
    );
  },
};

export function MessageManagementPage() {
  const [messageType, setMessageType] = useState("");
  const [messageCode, setMessageCode] = useState("");
  const [pageSize, setPageSize] = useState(20);
  const [state, setState] = useState<MessageManagementState>(
    createEmptyMessageManagementState(),
  );
  const [selectedMessage, setSelectedMessage] = useState<MessageCodeRow | null>(
    null,
  );
  const [form, setForm] = useState<MessageForm>(emptyMessage);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const loadMessages = async () => {
    setState((current) =>
      reduceMessageManagementState(current, { type: "loading" }),
    );
    setFieldErrors({});
    try {
      const response = await messageManagementApi.list({
        messageType,
        messageCode,
        page: 0,
        size: pageSize,
      });
      const messages = response.data?.messages ?? [];
      setState((current) =>
        reduceMessageManagementState(current, { type: "loaded", messages }),
      );
      if (selectedMessage) {
        const refreshed =
          messages.find(
            (row) => row.messageCode === selectedMessage.messageCode,
          ) ?? null;
        if (refreshed) applySelectedMessage(refreshed);
      }
    } catch (caught) {
      handleApiError(caught);
    }
  };

  useEffect(() => {
    void loadMessages();
  }, []);

  const applySelectedMessage = (row: MessageCodeRow) => {
    setSelectedMessage(row);
    setForm({ ...row, changeReason: "" });
    setFieldErrors({});
  };

  const startNew = () => {
    setSelectedMessage(null);
    setForm(emptyMessage);
    setFieldErrors({});
  };

  const resetFilters = () => {
    setMessageType("");
    setMessageCode("");
    setPageSize(20);
    setSelectedMessage(null);
    setForm(emptyMessage);
    setState(createEmptyMessageManagementState());
  };

  const cancelEdit = () => {
    if (selectedMessage) applySelectedMessage(selectedMessage);
    else setForm(emptyMessage);
    setFieldErrors({});
  };

  const validateLocal = () => {
    const next: Record<string, string> = {};
    if (!form.messageCode.trim()) next.messageCode = "메시지코드는 필수입니다.";
    if (!form.userMessage.trim())
      next.userMessage = "사용자 문구는 필수입니다.";
    if (!form.changeReason.trim())
      next.changeReason = "변경 사유는 필수입니다.";
    setFieldErrors(next);
    return Object.keys(next).length === 0;
  };

  const saveMessage = async () => {
    if (!validateLocal()) return;
    if (
      !window.confirm(
        messageManagementApi.uiMessages.saveConfirm(form.messageCode),
      )
    )
      return;
    try {
      setFieldErrors({});
      const response = await messageManagementApi.save(
        form.messageCode.trim(),
        messageManagementApi.toSavePayload(form),
      );
      const saved = response.data ?? {
        ...form,
        messageCode: form.messageCode.trim(),
      };
      applySelectedMessage(saved);
      setState((current) =>
        reduceMessageManagementState(current, {
          type: "success",
          message: messageManagementApi.uiMessages.saveSuccess,
        }),
      );
      await loadMessages();
    } catch (caught) {
      handleApiError(caught);
    }
  };

  const handleApiError = (caught: unknown) => {
    if (caught instanceof ApiClientError && caught.status === 403) {
      setState((current) =>
        reduceMessageManagementState(current, { type: "permission" }),
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
        : messageManagementApi.uiMessages.error;
    setState((current) =>
      reduceMessageManagementState(current, { type: "error", message }),
    );
  };

  if (state.status === "permission") {
    return (
      <PermissionState
        title="메시지 관리 권한이 없습니다"
        message="R09 시스템관리자 권한 또는 메뉴 접근권한을 확인하세요."
      />
    );
  }

  return (
    <section
      data-testid="message-management-screen"
      data-screen-id="SCR-MESSAGE-MGMT"
      className="space-y-6"
    >
      <div className="rounded-md bg-lightsecondary p-6 shadow-none">
        <p className="text-sm text-muted">
          시스템 관리 &gt; 시스템 환경설정 &gt; 메시지 관리
        </p>
        <div className="mt-2 flex items-center gap-3">
          <MessageSquare className="h-6 w-6 text-primary" aria-hidden />
          <div>
            <h1 className="text-xl font-semibold text-dark">메시지 관리</h1>
            <p className="mt-1 text-sm text-muted">
              저장·삭제·승인·반려·오류·세션만료 상황의 공통 사용자 안내 문구를
              관리합니다.
            </p>
          </div>
        </div>
      </div>

      {state.status === "loading" && (
        <LoadingState
          title="메시지 조회 중"
          message="메시지코드 목록을 불러오고 있습니다."
        />
      )}
      {state.status === "error" && (
        <ErrorState
          title="메시지 처리 오류"
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
          <label className="text-sm font-medium text-dark">
            메시지 유형
            <select
              data-testid="message-type-filter"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2"
              value={messageType}
              onChange={(event) => setMessageType(event.target.value)}
            >
              <option value="">전체</option>
              {MESSAGE_TYPES.map((type) => (
                <option key={type.value} value={type.value}>
                  {type.label}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm font-medium text-dark lg:col-span-2">
            메시지코드/문구
            <input
              data-testid="message-code-filter"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2"
              value={messageCode}
              onChange={(event) => setMessageCode(event.target.value)}
              placeholder="예: SAVE.SUCCESS"
            />
          </label>
          <label className="text-sm font-medium text-dark">
            표시 건수
            <select
              data-testid="message-page-size-select"
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
            data-testid="message-search-button"
            type="button"
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void loadMessages()}
          >
            <Search className="h-4 w-4" />
            조회
          </button>
          <button
            data-testid="message-reset-button"
            type="button"
            className="inline-flex items-center gap-2 rounded-md border border-ld px-4 py-2 text-sm font-semibold text-dark"
            onClick={resetFilters}
          >
            <RefreshCw className="h-4 w-4" />
            조건 초기화
          </button>
          <button
            data-testid="message-new-button"
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
          <h2 className="text-lg font-semibold text-dark">메시지 목록</h2>
          <div className="mt-4 overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead className="bg-lightgray text-muted">
                <tr>
                  <th className="px-3 py-2">메시지코드</th>
                  <th className="px-3 py-2">유형</th>
                  <th className="px-3 py-2">사용자 문구</th>
                  <th className="px-3 py-2">최종 수정</th>
                </tr>
              </thead>
              <tbody>
                {state.messages.map((row) => (
                  <tr
                    key={row.messageCode}
                    data-testid={`message-list-row-${row.messageCode.toLowerCase().replace(/[^a-z0-9]+/g, "-")}`}
                    className={`cursor-pointer border-t border-ld ${selectedMessage?.messageCode === row.messageCode ? "bg-lightprimary" : ""}`}
                    onClick={() => applySelectedMessage(row)}
                  >
                    <td className="px-3 py-3 font-semibold text-primary">
                      {row.messageCode}
                    </td>
                    <td className="px-3 py-3">{row.messageType}</td>
                    <td className="px-3 py-3">{row.userMessage}</td>
                    <td className="px-3 py-3 text-muted">
                      {row.updatedAt ?? "-"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {state.status === "empty" && (
            <EmptyState
              title="조회된 메시지가 없습니다"
              message="검색조건을 변경하거나 신규 메시지를 등록하세요."
            />
          )}
        </section>

        <section className="rounded-md border border-ld bg-white p-5 shadow-md xl:col-span-5">
          <h2 className="text-lg font-semibold text-dark">메시지 상세/편집</h2>
          <div className="mt-4 space-y-4">
            <label className="block text-sm font-medium text-dark">
              메시지코드 <span className="text-error">*</span>
              <input
                data-testid="message-code-input"
                className="mt-2 w-full rounded-md border border-ld px-3 py-2"
                value={form.messageCode}
                onChange={(event) =>
                  setForm({
                    ...form,
                    messageCode: event.target.value.toUpperCase(),
                  })
                }
                placeholder="SAVE.SUCCESS"
              />
              {fieldErrors.messageCode && (
                <p className="mt-1 text-xs text-error">
                  {fieldErrors.messageCode}
                </p>
              )}
            </label>
            <label className="block text-sm font-medium text-dark">
              메시지 유형 <span className="text-error">*</span>
              <select
                data-testid="message-type-select"
                className="mt-2 w-full rounded-md border border-ld px-3 py-2"
                value={form.messageType}
                onChange={(event) =>
                  setForm({
                    ...form,
                    messageType: event.target.value as MessageType,
                  })
                }
              >
                {MESSAGE_TYPES.map((type) => (
                  <option key={type.value} value={type.value}>
                    {type.label}
                  </option>
                ))}
              </select>
              {fieldErrors.messageType && (
                <p className="mt-1 text-xs text-error">
                  {fieldErrors.messageType}
                </p>
              )}
            </label>
            <label className="block text-sm font-medium text-dark">
              사용자 문구 <span className="text-error">*</span>
              <textarea
                data-testid="message-user-message-textarea"
                className="mt-2 min-h-28 w-full rounded-md border border-ld px-3 py-2"
                value={form.userMessage}
                onChange={(event) =>
                  setForm({ ...form, userMessage: event.target.value })
                }
              />
              {fieldErrors.userMessage && (
                <p className="mt-1 text-xs text-error">
                  {fieldErrors.userMessage}
                </p>
              )}
            </label>
            <label className="block text-sm font-medium text-dark">
              변경 사유 <span className="text-error">*</span>
              <input
                data-testid="message-change-reason-input"
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
                data-testid="message-save-button"
                type="button"
                className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
                onClick={() => void saveMessage()}
              >
                <Save className="h-4 w-4" />
                저장
              </button>
              <button
                data-testid="message-cancel-button"
                type="button"
                className="rounded-md border border-ld px-4 py-2 text-sm font-semibold text-dark"
                onClick={cancelEdit}
              >
                취소
              </button>
            </div>
          </div>
        </section>
      </div>
    </section>
  );
}
