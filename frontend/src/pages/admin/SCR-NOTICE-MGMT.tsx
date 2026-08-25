import { useEffect, useState } from "react";
import {
  Download,
  Megaphone,
  RefreshCw,
  Save,
  Search,
  Upload,
} from "lucide-react";
import { ApiClientError, apiRequest } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

export type NoticeTargetType = "ROLE" | "ORGANIZATION";
export type YesNo = "Y" | "N";

export type NoticeTarget = {
  targetId?: number;
  noticeId?: number;
  targetType: NoticeTargetType;
  targetIdValue: string;
  targetName?: string;
};

export type NoticeAttachment = {
  attachmentId?: number;
  noticeId?: number;
  originalFileName: string;
  fileSize?: number;
  createdAt?: string;
};

export type NoticeRow = {
  noticeId?: number;
  title: string;
  content: string;
  publishStartDate: string;
  publishEndDate: string;
  importantYn: YesNo;
  status?: string;
  updatedAt?: string;
  updatedBy?: number;
  targets: NoticeTarget[];
  attachments: NoticeAttachment[];
};

type NoticeSearchResponse = {
  notices: NoticeRow[];
  page: number;
  pageSize: number;
  totalElements: number;
};

type NoticeAttachmentPayload = {
  originalFileName: string;
  contentBase64?: string;
  contentText?: string;
};

type NoticeSavePayload = {
  title: string;
  content: string;
  publishStartDate: string;
  publishEndDate: string;
  importantYn: YesNo;
  targets: Array<{ targetType: NoticeTargetType; targetId: string }>;
  attachments: NoticeAttachmentPayload[];
  changeReason: string;
};

type NoticeForm = NoticeRow & {
  roleTarget: string;
  organizationTarget: string;
  changeReason: string;
  pendingAttachments: NoticeAttachmentPayload[];
};

type ScreenStatus =
  | "idle"
  | "loading"
  | "empty"
  | "loaded"
  | "error"
  | "permission"
  | "success";

type NoticeState = {
  status: ScreenStatus;
  notices: NoticeRow[];
  message?: string;
};

type NoticeAction =
  | { type: "loading" }
  | { type: "loaded"; notices: NoticeRow[] }
  | { type: "error"; message: string }
  | { type: "permission" }
  | { type: "success"; message: string };

type NoticeListParams = {
  publishStartDate?: string;
  publishEndDate?: string;
  targetRoleCode?: string;
  targetOrganizationCode?: string;
  activeOnly?: boolean;
  page?: number;
  pageSize?: number;
};

const ROLE_OPTIONS = [
  "R01",
  "R02",
  "R03",
  "R04",
  "R05",
  "R06",
  "R07",
  "R08",
  "R09",
];

const emptyNotice: NoticeForm = {
  title: "",
  content: "",
  publishStartDate: "",
  publishEndDate: "",
  importantYn: "N",
  targets: [],
  attachments: [],
  roleTarget: "R09",
  organizationTarget: "",
  changeReason: "",
  pendingAttachments: [],
};

export function getNoticeManagementRouteContract() {
  return {
    route: "/admin/notices",
    screenId: "SCR-NOTICE-MGMT",
    operations: [
      "listNotices",
      "createNotice",
      "saveNotice",
      "downloadNoticeAttachment",
    ],
  } as const;
}

export function createEmptyNoticeManagementState(): NoticeState {
  return { status: "idle", notices: [] };
}

export function reduceNoticeManagementState(
  state: NoticeState,
  action: NoticeAction,
): NoticeState {
  switch (action.type) {
    case "loading":
      return { ...state, status: "loading", message: undefined };
    case "loaded":
      return {
        ...state,
        status: action.notices.length === 0 ? "empty" : "loaded",
        notices: action.notices,
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

export const noticeManagementApi = {
  uiMessages: {
    saveConfirm: "공지사항을 저장하시겠습니까?",
    saveSuccess: "공지사항이 저장되었습니다.",
    error: "공지사항을 처리하지 못했습니다.",
  },
  paths: {
    list(params: NoticeListParams = {}) {
      const query = new URLSearchParams();
      query.set("page", String(params.page ?? 0));
      query.set("pageSize", String(params.pageSize ?? 20));
      if (params.publishStartDate)
        query.set("publishStartDate", params.publishStartDate);
      if (params.publishEndDate)
        query.set("publishEndDate", params.publishEndDate);
      if (params.targetRoleCode)
        query.set("targetRoleCode", params.targetRoleCode);
      if (params.targetOrganizationCode)
        query.set("targetOrganizationCode", params.targetOrganizationCode);
      if (params.activeOnly !== undefined)
        query.set("activeOnly", String(params.activeOnly));
      return `/api/admin/notices?${query.toString()}` as `/api/${string}`;
    },
    create() {
      return "/api/admin/notices" as `/api/${string}`;
    },
    save(noticeId: number) {
      return `/api/admin/notices/${encodeURIComponent(String(noticeId))}` as `/api/${string}`;
    },
    download(
      noticeId: number,
      attachmentId: number,
      organizationCode?: string,
    ) {
      const query = new URLSearchParams();
      if (organizationCode?.trim())
        query.set("organizationCode", organizationCode.trim());
      const suffix = query.toString() ? `?${query.toString()}` : "";
      return `/api/admin/notices/${encodeURIComponent(String(noticeId))}/attachments/${encodeURIComponent(String(attachmentId))}/download${suffix}` as `/api/${string}`;
    },
  },
  toSavePayload(form: NoticeForm): NoticeSavePayload {
    const targets: NoticeSavePayload["targets"] = [];
    if (form.roleTarget.trim())
      targets.push({ targetType: "ROLE", targetId: form.roleTarget.trim() });
    if (form.organizationTarget.trim())
      targets.push({
        targetType: "ORGANIZATION",
        targetId: form.organizationTarget.trim(),
      });
    return {
      title: form.title.trim(),
      content: form.content.trim(),
      publishStartDate: form.publishStartDate,
      publishEndDate: form.publishEndDate,
      importantYn: form.importantYn,
      targets,
      attachments: form.pendingAttachments,
      changeReason: form.changeReason.trim(),
    };
  },
  list(params: NoticeListParams = {}) {
    return apiRequest<NoticeSearchResponse>(
      noticeManagementApi.paths.list(params),
    );
  },
  create(payload: NoticeSavePayload) {
    return apiRequest<NoticeRow>(noticeManagementApi.paths.create(), {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },
  save(noticeId: number, payload: NoticeSavePayload) {
    return apiRequest<NoticeRow>(noticeManagementApi.paths.save(noticeId), {
      method: "PUT",
      body: JSON.stringify(payload),
    });
  },
};

export async function fileToNoticeAttachmentPayload(
  file: File,
): Promise<NoticeAttachmentPayload> {
  const contentBase64 = await new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result));
    reader.onerror = () =>
      reject(reader.error ?? new Error("파일을 읽을 수 없습니다."));
    reader.readAsDataURL(file);
  });
  return { originalFileName: file.name, contentBase64 };
}

export function NoticeManagementPage() {
  const [filters, setFilters] = useState({
    publishStartDate: "",
    publishEndDate: "",
    targetRoleCode: "R09",
    targetOrganizationCode: "",
    pageSize: 20,
  });
  const [state, setState] = useState<NoticeState>(
    createEmptyNoticeManagementState(),
  );
  const [selected, setSelected] = useState<NoticeRow | null>(null);
  const [form, setForm] = useState<NoticeForm>(emptyNotice);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const loadNotices = async () => {
    setState((current) =>
      reduceNoticeManagementState(current, { type: "loading" }),
    );
    try {
      const response = await noticeManagementApi.list({
        ...filters,
        pageSize: filters.pageSize,
        page: 0,
      });
      setState((current) =>
        reduceNoticeManagementState(current, {
          type: "loaded",
          notices: response.data?.notices ?? [],
        }),
      );
    } catch (caught) {
      handleApiError(caught);
    }
  };

  useEffect(() => {
    void loadNotices();
  }, []);

  const selectNotice = (notice: NoticeRow) => {
    const roleTarget =
      notice.targets.find((target) => target.targetType === "ROLE")
        ?.targetIdValue ?? "R09";
    const organizationTarget =
      notice.targets.find((target) => target.targetType === "ORGANIZATION")
        ?.targetIdValue ?? "";
    setSelected(notice);
    setForm({
      ...notice,
      roleTarget,
      organizationTarget,
      changeReason: "",
      pendingAttachments: [],
    });
    setFieldErrors({});
  };

  const validateLocal = () => {
    const next: Record<string, string> = {};
    if (!form.title.trim()) next.title = "제목은 필수입니다.";
    if (!form.content.trim()) next.content = "공지 내용은 필수입니다.";
    if (!form.publishStartDate)
      next.publishStartDate = "게시 시작일은 필수입니다.";
    if (!form.publishEndDate) next.publishEndDate = "게시 종료일은 필수입니다.";
    if (
      form.publishStartDate &&
      form.publishEndDate &&
      form.publishEndDate < form.publishStartDate
    )
      next.publishEndDate = "종료일은 시작일 이후여야 합니다.";
    if (!form.roleTarget.trim()) next.targets = "대상 역할은 필수입니다.";
    if (!form.organizationTarget.trim())
      next.targets = "대상 조직은 필수입니다.";
    if (!form.changeReason.trim())
      next.changeReason = "변경 사유는 필수입니다.";
    setFieldErrors(next);
    return Object.keys(next).length === 0;
  };

  const saveNotice = async () => {
    if (!validateLocal()) return;
    if (!window.confirm(noticeManagementApi.uiMessages.saveConfirm)) return;
    try {
      const payload = noticeManagementApi.toSavePayload(form);
      const response = selected?.noticeId
        ? await noticeManagementApi.save(selected.noticeId, payload)
        : await noticeManagementApi.create(payload);
      const saved = response.data;
      if (saved) selectNotice(saved);
      setState((current) =>
        reduceNoticeManagementState(current, {
          type: "success",
          message: noticeManagementApi.uiMessages.saveSuccess,
        }),
      );
      await loadNotices();
    } catch (caught) {
      handleApiError(caught);
    }
  };

  const handleApiError = (caught: unknown) => {
    if (caught instanceof ApiClientError && caught.status === 403) {
      setState((current) =>
        reduceNoticeManagementState(current, { type: "permission" }),
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
    setState((current) =>
      reduceNoticeManagementState(current, {
        type: "error",
        message:
          caught instanceof ApiClientError
            ? caught.message
            : noticeManagementApi.uiMessages.error,
      }),
    );
  };

  const onFileChange = async (files: FileList | null) => {
    if (!files?.length) return;
    const next = await Promise.all(
      Array.from(files).map(fileToNoticeAttachmentPayload),
    );
    setForm({ ...form, pendingAttachments: next });
  };

  if (state.status === "permission") {
    return (
      <PermissionState
        title="공지사항 관리 권한이 없습니다"
        message="R09 시스템관리자 권한 또는 메뉴 접근권한을 확인하세요."
      />
    );
  }

  return (
    <section
      data-testid="notice-management-screen"
      data-screen-id="SCR-NOTICE-MGMT"
      className="space-y-6"
    >
      <div className="rounded-md bg-lightsecondary p-6 shadow-none">
        <p className="text-sm text-muted">
          시스템 관리 &gt; 공지·도움말 관리 &gt; 공지사항 관리
        </p>
        <div className="mt-2 flex items-center gap-3">
          <Megaphone className="h-6 w-6 text-primary" aria-hidden />
          <div>
            <h1 className="text-xl font-semibold text-dark">공지사항 관리</h1>
            <p className="mt-1 text-sm text-muted">
              게시기간, 대상 역할, 대상 조직 조건에 맞는 공지와 첨부파일을
              관리합니다.
            </p>
          </div>
        </div>
      </div>

      {state.status === "loading" && (
        <LoadingState
          title="공지사항 조회 중"
          message="조건에 맞는 공지를 불러오고 있습니다."
        />
      )}
      {state.status === "error" && (
        <ErrorState
          title="공지사항 처리 오류"
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
        <div className="grid gap-4 lg:grid-cols-5">
          <label className="text-sm font-medium text-dark">
            게시 시작일
            <input
              data-testid="notice-start-filter"
              type="date"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2"
              value={filters.publishStartDate}
              onChange={(event) =>
                setFilters({ ...filters, publishStartDate: event.target.value })
              }
            />
          </label>
          <label className="text-sm font-medium text-dark">
            게시 종료일
            <input
              data-testid="notice-end-filter"
              type="date"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2"
              value={filters.publishEndDate}
              onChange={(event) =>
                setFilters({ ...filters, publishEndDate: event.target.value })
              }
            />
          </label>
          <label className="text-sm font-medium text-dark">
            대상 역할
            <select
              data-testid="notice-role-filter"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2"
              value={filters.targetRoleCode}
              onChange={(event) =>
                setFilters({ ...filters, targetRoleCode: event.target.value })
              }
            >
              <option value="">전체</option>
              {ROLE_OPTIONS.map((role) => (
                <option key={role} value={role}>
                  {role}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm font-medium text-dark">
            대상 조직
            <input
              data-testid="notice-organization-filter"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2"
              value={filters.targetOrganizationCode}
              onChange={(event) =>
                setFilters({
                  ...filters,
                  targetOrganizationCode: event.target.value,
                })
              }
              placeholder="조직코드"
            />
          </label>
          <label className="text-sm font-medium text-dark">
            표시 건수
            <select
              data-testid="notice-page-size-select"
              className="mt-2 w-full rounded-md border border-ld px-3 py-2"
              value={filters.pageSize}
              onChange={(event) =>
                setFilters({ ...filters, pageSize: Number(event.target.value) })
              }
            >
              <option value={20}>20건</option>
              <option value={50}>50건</option>
              <option value={100}>100건</option>
            </select>
          </label>
        </div>
        <div className="mt-4 flex flex-wrap gap-2">
          <button
            data-testid="notice-search-button"
            type="button"
            className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
            onClick={() => void loadNotices()}
          >
            <Search className="h-4 w-4" />
            조회
          </button>
          <button
            data-testid="notice-new-button"
            type="button"
            className="inline-flex items-center gap-2 rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary"
            onClick={() => {
              setSelected(null);
              setForm(emptyNotice);
              setFieldErrors({});
            }}
          >
            <RefreshCw className="h-4 w-4" />
            신규 등록
          </button>
        </div>
      </section>

      <div className="grid gap-6 xl:grid-cols-12">
        <section className="rounded-md border border-ld bg-white p-5 shadow-md xl:col-span-7">
          <h2 className="text-lg font-semibold text-dark">공지 목록</h2>
          <div className="mt-4 overflow-x-auto">
            <table className="min-w-full text-left text-sm">
              <thead className="bg-lightgray text-muted">
                <tr>
                  <th className="px-3 py-2">제목</th>
                  <th className="px-3 py-2">게시기간</th>
                  <th className="px-3 py-2">대상</th>
                  <th className="px-3 py-2">중요</th>
                  <th className="px-3 py-2">첨부</th>
                </tr>
              </thead>
              <tbody>
                {state.notices.map((row) => (
                  <tr
                    key={row.noticeId}
                    data-testid={`notice-list-row-${row.noticeId}`}
                    className={`cursor-pointer border-t border-ld ${selected?.noticeId === row.noticeId ? "bg-lightprimary" : ""}`}
                    onClick={() => selectNotice(row)}
                  >
                    <td className="px-3 py-3 font-semibold text-primary">
                      {row.title}
                    </td>
                    <td className="px-3 py-3">
                      {row.publishStartDate} ~ {row.publishEndDate}
                    </td>
                    <td className="px-3 py-3">
                      {row.targets
                        .map(
                          (target) =>
                            `${target.targetType}:${target.targetIdValue}`,
                        )
                        .join(", ")}
                    </td>
                    <td className="px-3 py-3">{row.importantYn}</td>
                    <td className="px-3 py-3">{row.attachments.length}개</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {state.status === "empty" && (
            <EmptyState
              title="조회된 공지사항이 없습니다"
              message="게시기간 또는 대상 조건을 변경하세요."
            />
          )}
        </section>

        <section className="rounded-md border border-ld bg-white p-5 shadow-md xl:col-span-5">
          <h2 className="text-lg font-semibold text-dark">공지 상세/편집</h2>
          <div className="mt-4 space-y-4">
            <label className="block text-sm font-medium text-dark">
              제목 <span className="text-error">*</span>
              <input
                data-testid="notice-title-input"
                className="mt-2 w-full rounded-md border border-ld px-3 py-2"
                value={form.title}
                onChange={(event) =>
                  setForm({ ...form, title: event.target.value })
                }
              />
              {fieldErrors.title && (
                <p className="mt-1 text-xs text-error">{fieldErrors.title}</p>
              )}
            </label>
            <label className="block text-sm font-medium text-dark">
              공지 내용 <span className="text-error">*</span>
              <textarea
                data-testid="notice-content-textarea"
                className="mt-2 min-h-28 w-full rounded-md border border-ld px-3 py-2"
                value={form.content}
                onChange={(event) =>
                  setForm({ ...form, content: event.target.value })
                }
              />
              {fieldErrors.content && (
                <p className="mt-1 text-xs text-error">{fieldErrors.content}</p>
              )}
            </label>
            <div className="grid gap-3 md:grid-cols-3">
              <label className="text-sm font-medium text-dark">
                게시 시작일*
                <input
                  data-testid="notice-start-input"
                  type="date"
                  className="mt-2 w-full rounded-md border border-ld px-3 py-2"
                  value={form.publishStartDate}
                  onChange={(event) =>
                    setForm({ ...form, publishStartDate: event.target.value })
                  }
                />
              </label>
              <label className="text-sm font-medium text-dark">
                게시 종료일*
                <input
                  data-testid="notice-end-input"
                  type="date"
                  className="mt-2 w-full rounded-md border border-ld px-3 py-2"
                  value={form.publishEndDate}
                  onChange={(event) =>
                    setForm({ ...form, publishEndDate: event.target.value })
                  }
                />
              </label>
              <label className="text-sm font-medium text-dark">
                중요여부
                <select
                  data-testid="notice-important-select"
                  className="mt-2 w-full rounded-md border border-ld px-3 py-2"
                  value={form.importantYn}
                  onChange={(event) =>
                    setForm({
                      ...form,
                      importantYn: event.target.value as YesNo,
                    })
                  }
                >
                  <option value="Y">Y</option>
                  <option value="N">N</option>
                </select>
              </label>
            </div>
            {fieldErrors.publishEndDate && (
              <p className="text-xs text-error">{fieldErrors.publishEndDate}</p>
            )}
            <div className="grid gap-3 md:grid-cols-2">
              <label className="text-sm font-medium text-dark">
                대상 역할*
                <select
                  data-testid="notice-role-target-select"
                  className="mt-2 w-full rounded-md border border-ld px-3 py-2"
                  value={form.roleTarget}
                  onChange={(event) =>
                    setForm({ ...form, roleTarget: event.target.value })
                  }
                >
                  {ROLE_OPTIONS.map((role) => (
                    <option key={role} value={role}>
                      {role}
                    </option>
                  ))}
                </select>
              </label>
              <label className="text-sm font-medium text-dark">
                대상 조직*
                <input
                  data-testid="notice-organization-target-input"
                  className="mt-2 w-full rounded-md border border-ld px-3 py-2"
                  value={form.organizationTarget}
                  onChange={(event) =>
                    setForm({ ...form, organizationTarget: event.target.value })
                  }
                  placeholder="조직코드"
                />
              </label>
            </div>
            {fieldErrors.targets && (
              <p className="text-xs text-error">{fieldErrors.targets}</p>
            )}
            <label className="block text-sm font-medium text-dark">
              첨부파일
              <input
                data-testid="notice-file-input"
                type="file"
                multiple
                className="mt-2 w-full rounded-md border border-ld px-3 py-2"
                onChange={(event) => void onFileChange(event.target.files)}
              />
            </label>
            <div className="rounded-md bg-lightgray p-3 text-xs text-muted">
              <Upload className="mr-1 inline h-3 w-3" />
              원본 파일명만 화면에 표시하며 내부 저장명과 경로는 노출하지
              않습니다. 기존 첨부:{" "}
              {form.attachments
                .map((attachment) => attachment.originalFileName)
                .join(", ") || "없음"}
            </div>
            <label className="block text-sm font-medium text-dark">
              변경 사유*
              <input
                data-testid="notice-change-reason-input"
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
            <div className="flex flex-wrap gap-2">
              <button
                data-testid="notice-save-button"
                type="button"
                className="inline-flex items-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white"
                onClick={() => void saveNotice()}
              >
                <Save className="h-4 w-4" />
                저장
              </button>
              {form.attachments.map((attachment) =>
                selected?.noticeId && attachment.attachmentId ? (
                  <a
                    key={attachment.attachmentId}
                    data-testid={`notice-attachment-download-${attachment.attachmentId}`}
                    className="inline-flex items-center gap-2 rounded-md border border-ld px-4 py-2 text-sm font-semibold text-dark"
                    href={noticeManagementApi.paths.download(
                      selected.noticeId,
                      attachment.attachmentId,
                      form.organizationTarget,
                    )}
                  >
                    <Download className="h-4 w-4" />
                    {attachment.originalFileName}
                  </a>
                ) : null,
              )}
            </div>
          </div>
        </section>
      </div>
    </section>
  );
}
