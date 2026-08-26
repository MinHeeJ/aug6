import type React from "react";
import { useEffect, useState } from "react";
import {
  Download,
  FileSpreadsheet,
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

type ScreenStatus =
  | "idle"
  | "loading"
  | "empty"
  | "loaded"
  | "error"
  | "permission"
  | "success";
type Paged<T> = { page: number; size: number; totalElements: number } & T;
export type ExcelTemplateRule = {
  ruleId?: string;
  requiredColumn: string;
  columnOrder: number;
  codeRuleRef: string;
};
export type ExcelTemplateRow = {
  templateId: string;
  businessType: string;
  templateVersion: string;
  effectiveDate: string;
  fileToken?: string;
  originalFileName?: string;
  rules: ExcelTemplateRule[];
};
export type ExcelUploadErrorRow = {
  errorId: string;
  uploadId: string;
  rowNumber: number;
  columnName: string;
  inputValue?: string;
  errorCode: string;
  errorReason: string;
  correctionGuide?: string;
};
export type ExcelUploadHistoryRow = {
  uploadId: string;
  originalFileName: string;
  uploaderUserId?: number;
  totalCount: number;
  successCount: number;
  errorCount: number;
  excludedCount: number;
  savedCount: number;
  processingTimeMillis?: number;
  processedAt?: string;
};
export type ExcelUploadResult = {
  uploadId: string;
  businessType: string;
  originalFileName: string;
  validationStatus: string;
  totalCount: number;
  successCount: number;
  errorCount: number;
  excludedCount: number;
  savedCount: number;
  errors: ExcelUploadErrorRow[];
};
export type ExcelDownloadJobRow = {
  downloadId: string;
  outputType: "TARGET" | "STATUS" | "ERROR";
  queryCondition: string;
  dataScopeRef: string;
  fileToken: string;
  originalFileName?: string;
  status: string;
};

export const EXCEL_PAGE_SIZE_OPTIONS = [20, 50, 100] as const;

function describeError(caught: unknown, fallback: string) {
  if (caught instanceof ApiClientError && caught.status === 403)
    return "권한이 없습니다.";
  return caught instanceof Error ? caught.message : fallback;
}

export const excelApi = {
  pageSizeOptions: EXCEL_PAGE_SIZE_OPTIONS,
  async listTemplates(params: {
    businessType?: string;
    effectiveDate?: string;
    size?: number;
  }) {
    const q = new URLSearchParams({
      page: "0",
      size: String(params.size ?? 20),
    });
    if (params.businessType?.trim())
      q.set("businessType", params.businessType.trim());
    if (params.effectiveDate?.trim())
      q.set("effectiveDate", params.effectiveDate.trim());
    return apiRequest<Paged<{ templates: ExcelTemplateRow[] }>>(
      `/api/admin/excel-upload-templates?${q.toString()}` as `/api/${string}`,
    );
  },
  saveTemplate(payload: {
    templateId?: string;
    businessType: string;
    templateVersion: string;
    effectiveDate: string;
    originalFileName?: string;
    rules: ExcelTemplateRule[];
  }) {
    return apiRequest<ExcelTemplateRow>("/api/admin/excel-upload-templates", {
      method: "POST",
      body: JSON.stringify(payload),
    });
  },
  downloadTemplate(templateId: string) {
    return fetch(
      `/api/admin/excel-upload-templates/${encodeURIComponent(templateId)}/file`,
      { credentials: "include" },
    );
  },
  async createUpload(businessType: string, templateId: string, file: File) {
    const form = new FormData();
    form.append("file", file);
    const q = new URLSearchParams({ businessType });
    if (templateId.trim()) q.set("templateId", templateId.trim());
    const response = await fetch(`/api/admin/excel-uploads?${q.toString()}`, {
      method: "POST",
      body: form,
      credentials: "include",
    });
    const body = await response.json();
    if (!response.ok || body.success === false)
      throw new ApiClientError(
        response.status,
        body.error?.message ?? "업로드 실패",
        body.error,
      );
    return body as { data?: ExcelUploadResult };
  },
  commitUpload(uploadId: string) {
    return apiRequest<{ uploadId: string; savedCount: number }>(
      `/api/admin/excel-uploads/${encodeURIComponent(uploadId)}/commit` as `/api/${string}`,
      { method: "POST" },
    );
  },
  listHistories(params: {
    uploadId?: string;
    originalFileName?: string;
    size?: number;
  }) {
    const q = new URLSearchParams({
      page: "0",
      size: String(params.size ?? 20),
    });
    if (params.uploadId?.trim()) q.set("uploadId", params.uploadId.trim());
    if (params.originalFileName?.trim())
      q.set("originalFileName", params.originalFileName.trim());
    return apiRequest<Paged<{ histories: ExcelUploadHistoryRow[] }>>(
      `/api/admin/excel-upload-histories?${q.toString()}` as `/api/${string}`,
    );
  },
  listErrors(params: { uploadId: string; size?: number }) {
    const q = new URLSearchParams({
      page: "0",
      size: String(params.size ?? 20),
      uploadId: params.uploadId,
    });
    return apiRequest<Paged<{ errors: ExcelUploadErrorRow[] }>>(
      `/api/admin/excel-upload-errors?${q.toString()}` as `/api/${string}`,
    );
  },
  downloadErrors(uploadId: string) {
    return fetch(
      `/api/admin/excel-upload-errors/download?${new URLSearchParams({ uploadId }).toString()}`,
      { credentials: "include" },
    );
  },
  createDownload(
    outputType: ExcelDownloadJobRow["outputType"],
    queryCondition: Record<string, unknown>,
  ) {
    return apiRequest<ExcelDownloadJobRow>("/api/admin/excel-downloads", {
      method: "POST",
      body: JSON.stringify({ outputType, queryCondition }),
    });
  },
};

function ScreenCard({
  title,
  children,
}: {
  title: string;
  children: React.ReactNode;
}) {
  return (
    <section
      className="rounded-md bg-white p-6 shadow-md"
      data-testid={`${title.replace(/\s+/g, "-")}-panel`}
    >
      {children}
    </section>
  );
}
function StatusView({
  status,
  message,
}: {
  status: ScreenStatus;
  message?: string;
}) {
  if (status === "loading")
    return (
      <LoadingState
        title="조회 중"
        message="Excel 운영 데이터를 불러오고 있습니다."
      />
    );
  if (status === "permission")
    return (
      <PermissionState
        title="권한이 없습니다"
        message="Excel 관리 화면 접근 권한이 없습니다."
      />
    );
  if (status === "error")
    return (
      <ErrorState
        title="처리 오류"
        message={message ?? "Excel 운영 정보를 처리하지 못했습니다."}
      />
    );
  if (status === "success")
    return (
      <SuccessState
        title="처리 완료"
        message={message ?? "요청이 완료되었습니다."}
      />
    );
  return null;
}

export function UploadTemplateManagementPage() {
  const [businessType, setBusinessType] = useState("");
  const [effectiveDate, setEffectiveDate] = useState("");
  const [pageSize, setPageSize] = useState(20);
  const [rows, setRows] = useState<ExcelTemplateRow[]>([]);
  const [status, setStatus] = useState<ScreenStatus>("idle");
  const [message, setMessage] = useState<string>();
  const [form, setForm] = useState({
    templateId: "",
    templateVersion: "",
    originalFileName: "",
    requiredColumn: "",
    columnOrder: "",
    codeRuleRef: "",
  });
  const load = async () => {
    setStatus("loading");
    try {
      const res = await excelApi.listTemplates({
        businessType,
        effectiveDate,
        size: pageSize,
      });
      const next = res.data?.templates ?? [];
      setRows(next);
      setStatus(next.length ? "loaded" : "empty");
    } catch (e) {
      const m = describeError(e, "양식 조회 실패");
      setMessage(m);
      setStatus(m.includes("권한") ? "permission" : "error");
    }
  };
  useEffect(() => {
    void load();
  }, [pageSize]);
  const save = async () => {
    if (!window.confirm("업로드 양식을 저장하시겠습니까?")) return;
    if (
      !businessType ||
      !form.templateVersion ||
      !effectiveDate ||
      !form.requiredColumn ||
      !form.columnOrder ||
      !form.codeRuleRef
    ) {
      setMessage("필수 항목을 입력하세요.");
      setStatus("error");
      return;
    }
    setStatus("loading");
    try {
      await excelApi.saveTemplate({
        templateId: form.templateId || undefined,
        businessType,
        templateVersion: form.templateVersion,
        effectiveDate,
        originalFileName: form.originalFileName,
        rules: [
          {
            requiredColumn: form.requiredColumn,
            columnOrder: Number(form.columnOrder),
            codeRuleRef: form.codeRuleRef,
          },
        ],
      });
      setMessage("업로드 양식이 저장되었습니다.");
      setStatus("success");
      await load();
    } catch (e) {
      setMessage(describeError(e, "저장 실패"));
      setStatus("error");
    }
  };
  return (
    <main className="space-y-6" data-testid="excel-upload-templates-screen">
      <ScreenCard title="업로드 양식 관리">
        <div className="mb-4 flex flex-wrap gap-3">
          <input
            data-testid="excel-template-business-type-input"
            className="form-input"
            value={businessType}
            onChange={(e) => setBusinessType(e.target.value)}
            aria-label="업무구분 필수"
          />
          <input
            data-testid="excel-template-effective-date-input"
            className="form-input"
            type="date"
            value={effectiveDate}
            onChange={(e) => setEffectiveDate(e.target.value)}
            aria-label="시행일 필수"
          />
          <select
            data-testid="excel-template-page-size-select"
            className="form-input"
            value={pageSize}
            onChange={(e) => setPageSize(Number(e.target.value))}
          >
            {EXCEL_PAGE_SIZE_OPTIONS.map((v) => (
              <option key={v}>{v}</option>
            ))}
          </select>
          <button
            data-testid="excel-template-search-button"
            className="btn-secondary"
            onClick={load}
          >
            <Search size={16} />
            조회
          </button>
        </div>
        <div className="grid gap-3 md:grid-cols-3">
          <input
            data-testid="excel-template-version-input"
            className="form-input"
            placeholder="양식 버전 *"
            value={form.templateVersion}
            onChange={(e) =>
              setForm({ ...form, templateVersion: e.target.value })
            }
          />
          <input
            data-testid="excel-template-column-input"
            className="form-input"
            placeholder="필수 열 *"
            value={form.requiredColumn}
            onChange={(e) =>
              setForm({ ...form, requiredColumn: e.target.value })
            }
          />
          <input
            data-testid="excel-template-code-rule-input"
            className="form-input"
            placeholder="코드값 규칙 *"
            value={form.codeRuleRef}
            onChange={(e) => setForm({ ...form, codeRuleRef: e.target.value })}
          />
          <input
            data-testid="excel-template-order-input"
            className="form-input"
            placeholder="열 순서 *"
            value={form.columnOrder}
            onChange={(e) => setForm({ ...form, columnOrder: e.target.value })}
          />
          <input
            data-testid="excel-template-file-name-input"
            className="form-input"
            placeholder="다운로드 파일명"
            value={form.originalFileName}
            onChange={(e) =>
              setForm({ ...form, originalFileName: e.target.value })
            }
          />
          <button
            data-testid="excel-template-save-button"
            className="btn-primary"
            onClick={save}
          >
            <Save size={16} />
            저장
          </button>
        </div>
      </ScreenCard>
      <StatusView status={status} message={message} />
      {status === "empty" && (
        <EmptyState
          title="양식 없음"
          message="조회 조건에 맞는 업로드 양식이 없습니다."
        />
      )}
      <ScreenCard title="업로드 양식 목록">
        <table className="table">
          <thead>
            <tr>
              <th>업무구분</th>
              <th>버전</th>
              <th>시행일</th>
              <th>검증규칙</th>
              <th>파일</th>
              <th>작업</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr data-testid="excel-template-row" key={row.templateId}>
                <td>{row.businessType}</td>
                <td>{row.templateVersion}</td>
                <td>{row.effectiveDate}</td>
                <td>
                  {row.rules
                    .map((r) => `${r.columnOrder}.${r.requiredColumn}`)
                    .join(", ")}
                </td>
                <td>{row.originalFileName}</td>
                <td>
                  <button
                    data-testid="excel-template-download-button"
                    className="btn-secondary"
                    onClick={() =>
                      void excelApi.downloadTemplate(row.templateId)
                    }
                  >
                    <Download size={16} />
                    다운로드
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </ScreenCard>
    </main>
  );
}

export function ExcelUploadManagementPage() {
  const [businessType, setBusinessType] = useState("");
  const [templateId, setTemplateId] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [result, setResult] = useState<ExcelUploadResult | null>(null);
  const [status, setStatus] = useState<ScreenStatus>("idle");
  const [message, setMessage] = useState<string>();
  const upload = async () => {
    if (!window.confirm("엑셀 파일 업로드와 사전검증을 실행하시겠습니까?"))
      return;
    if (!file) {
      setMessage("필수 파일을 선택하세요.");
      setStatus("error");
      return;
    }
    setStatus("loading");
    try {
      const res = await excelApi.createUpload(businessType, templateId, file);
      setResult(res.data ?? null);
      setMessage("사전검증이 완료되었습니다.");
      setStatus("success");
    } catch (e) {
      setMessage(describeError(e, "업로드 실패"));
      setStatus("error");
    }
  };
  const commit = async () => {
    if (
      !result?.uploadId ||
      !window.confirm("오류가 없는 업로드를 전체 반영하시겠습니까?")
    )
      return;
    setStatus("loading");
    try {
      const res = await excelApi.commitUpload(result.uploadId);
      setMessage(`반영 완료: 저장 ${res.data?.savedCount ?? 0}건`);
      setStatus("success");
    } catch (e) {
      setMessage(describeError(e, "반영 실패"));
      setStatus("error");
    }
  };
  return (
    <main className="space-y-6" data-testid="excel-uploads-screen">
      <ScreenCard title="엑셀 업로드">
        <div className="grid gap-3 md:grid-cols-4">
          <input
            data-testid="excel-upload-business-type-input"
            className="form-input"
            value={businessType}
            onChange={(e) => setBusinessType(e.target.value)}
            aria-label="업무구분 필수"
          />
          <input
            data-testid="excel-upload-template-id-input"
            className="form-input"
            value={templateId}
            onChange={(e) => setTemplateId(e.target.value)}
            aria-label="양식 버전"
          />
          <input
            data-testid="excel-upload-file-input"
            className="form-input"
            type="file"
            accept=".csv,.xls,.xlsx"
            onChange={(e) => setFile(e.target.files?.[0] ?? null)}
            aria-label="엑셀 파일 필수"
          />
          <button
            data-testid="excel-upload-submit-button"
            className="btn-primary"
            onClick={upload}
          >
            <Upload size={16} />
            업로드·검증
          </button>
        </div>
        <p className="mt-3 text-sm text-muted">
          절차: 템플릿 다운로드 → 파일 업로드 → 검증 → 검증결과 확인 → 반영
        </p>
      </ScreenCard>
      <StatusView status={status} message={message} />
      {result && (
        <ScreenCard title="검증 결과">
          <div className="grid gap-3 md:grid-cols-5">
            <b>원본 {result.totalCount}</b>
            <b>정상 {result.successCount}</b>
            <b>오류 {result.errorCount}</b>
            <b>제외 {result.excludedCount}</b>
            <b>저장 {result.savedCount}</b>
          </div>
          {result.errors.length ? (
            <table className="table mt-4">
              <tbody>
                {result.errors.map((e) => (
                  <tr data-testid="excel-upload-error-row" key={e.errorId}>
                    <td>{e.rowNumber}</td>
                    <td>{e.columnName}</td>
                    <td>{e.errorReason}</td>
                    <td>{e.correctionGuide}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <button
              data-testid="excel-upload-commit-button"
              className="btn-primary mt-4"
              onClick={commit}
            >
              <Save size={16} />
              전체 반영
            </button>
          )}
        </ScreenCard>
      )}
    </main>
  );
}

export function ExcelUploadHistoryManagementPage() {
  const [uploadId, setUploadId] = useState("");
  const [originalFileName, setOriginalFileName] = useState("");
  const [pageSize, setPageSize] = useState(20);
  const [rows, setRows] = useState<ExcelUploadHistoryRow[]>([]);
  const [status, setStatus] = useState<ScreenStatus>("idle");
  const [message, setMessage] = useState<string>();
  const load = async () => {
    setStatus("loading");
    try {
      const res = await excelApi.listHistories({
        uploadId,
        originalFileName,
        size: pageSize,
      });
      const next = res.data?.histories ?? [];
      setRows(next);
      setStatus(next.length ? "loaded" : "empty");
    } catch (e) {
      const m = describeError(e, "이력 조회 실패");
      setMessage(m);
      setStatus(m.includes("권한") ? "permission" : "error");
    }
  };
  useEffect(() => {
    void load();
  }, [pageSize]);
  return (
    <main className="space-y-6" data-testid="excel-upload-histories-screen">
      <ScreenCard title="업로드 이력 조회">
        <div className="flex flex-wrap gap-3">
          <input
            data-testid="excel-history-upload-id-input"
            className="form-input"
            placeholder="업로드ID"
            value={uploadId}
            onChange={(e) => setUploadId(e.target.value)}
          />
          <input
            data-testid="excel-history-file-name-input"
            className="form-input"
            placeholder="파일명"
            value={originalFileName}
            onChange={(e) => setOriginalFileName(e.target.value)}
          />
          <select
            data-testid="excel-history-page-size-select"
            className="form-input"
            value={pageSize}
            onChange={(e) => setPageSize(Number(e.target.value))}
          >
            {EXCEL_PAGE_SIZE_OPTIONS.map((v) => (
              <option key={v}>{v}</option>
            ))}
          </select>
          <button
            data-testid="excel-history-search-button"
            className="btn-secondary"
            onClick={load}
          >
            <RefreshCw size={16} />
            조회
          </button>
        </div>
      </ScreenCard>
      <StatusView status={status} message={message} />
      {status === "empty" && (
        <EmptyState
          title="이력 없음"
          message="조회 조건의 업로드 이력이 없습니다."
        />
      )}
      <ExcelHistoryTable rows={rows} />
    </main>
  );
}
function ExcelHistoryTable({ rows }: { rows: ExcelUploadHistoryRow[] }) {
  return (
    <ScreenCard title="처리건수">
      <table className="table">
        <thead>
          <tr>
            <th>업로드ID</th>
            <th>파일명</th>
            <th>업로더</th>
            <th>원본</th>
            <th>정상</th>
            <th>오류</th>
            <th>제외</th>
            <th>저장</th>
            <th>처리시간(ms)</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((r) => (
            <tr data-testid="excel-history-row" key={r.uploadId}>
              <td>{r.uploadId}</td>
              <td>{r.originalFileName}</td>
              <td>{r.uploaderUserId ?? "-"}</td>
              <td>{r.totalCount}</td>
              <td>{r.successCount}</td>
              <td>{r.errorCount}</td>
              <td>{r.excludedCount}</td>
              <td>{r.savedCount}</td>
              <td>{r.processingTimeMillis}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </ScreenCard>
  );
}

export function ExcelUploadErrorManagementPage() {
  const [uploadId, setUploadId] = useState("");
  const [rows, setRows] = useState<ExcelUploadErrorRow[]>([]);
  const [status, setStatus] = useState<ScreenStatus>("idle");
  const [message, setMessage] = useState<string>();
  const load = async () => {
    if (!uploadId.trim()) {
      setRows([]);
      setMessage("업로드ID를 입력하세요.");
      setStatus("empty");
      return;
    }
    setStatus("loading");
    try {
      const res = await excelApi.listErrors({ uploadId });
      const next = res.data?.errors ?? [];
      setRows(next);
      setStatus(next.length ? "loaded" : "empty");
    } catch (e) {
      const m = describeError(e, "오류 조회 실패");
      setMessage(m);
      setStatus(m.includes("권한") ? "permission" : "error");
    }
  };
  useEffect(() => {
    void load();
  }, []);
  return (
    <main className="space-y-6" data-testid="excel-upload-errors-screen">
      <ScreenCard title="업로드 오류 관리">
        <div className="flex flex-wrap gap-3">
          <input
            data-testid="excel-error-upload-id-input"
            className="form-input"
            value={uploadId}
            onChange={(e) => setUploadId(e.target.value)}
            aria-label="업로드ID 필수"
          />
          <button
            data-testid="excel-error-search-button"
            className="btn-secondary"
            onClick={load}
          >
            <Search size={16} />
            조회
          </button>
          <button
            data-testid="excel-error-download-button"
            className="btn-secondary"
            disabled={!uploadId.trim()}
            onClick={() => void excelApi.downloadErrors(uploadId)}
          >
            <Download size={16} />
            오류목록 다운로드
          </button>
        </div>
      </ScreenCard>
      <StatusView status={status} message={message} />
      {status === "empty" && (
        <EmptyState
          title="오류 없음"
          message={message ?? "조회 조건의 오류행이 없습니다."}
        />
      )}
      <ScreenCard title="오류 상세">
        <table className="table">
          <thead>
            <tr>
              <th>행</th>
              <th>컬럼</th>
              <th>입력값</th>
              <th>오류코드</th>
              <th>오류사유</th>
              <th>수정안내</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr data-testid="excel-error-row" key={r.errorId}>
                <td>{r.rowNumber}</td>
                <td>{r.columnName}</td>
                <td>{r.inputValue}</td>
                <td>{r.errorCode}</td>
                <td>{r.errorReason}</td>
                <td>{r.correctionGuide}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </ScreenCard>
    </main>
  );
}

export function ExcelDownloadManagementPage() {
  const [outputType, setOutputType] =
    useState<ExcelDownloadJobRow["outputType"]>("ERROR");
  const [queryText, setQueryText] = useState("{}");
  const [job, setJob] = useState<ExcelDownloadJobRow | null>(null);
  const [status, setStatus] = useState<ScreenStatus>("idle");
  const [message, setMessage] = useState<string>();
  const create = async () => {
    if (!window.confirm("현재 조회조건으로 Excel 다운로드를 생성하시겠습니까?"))
      return;
    setStatus("loading");
    try {
      const condition = JSON.parse(queryText || "{}");
      const res = await excelApi.createDownload(outputType, condition);
      setJob(res.data ?? null);
      setMessage("Excel 결과 파일 생성이 완료되었습니다.");
      setStatus("success");
    } catch (e) {
      setMessage(describeError(e, "다운로드 생성 실패"));
      setStatus("error");
    }
  };
  return (
    <main className="space-y-6" data-testid="excel-downloads-screen">
      <ScreenCard title="엑셀 다운로드">
        <div className="grid gap-3 md:grid-cols-3">
          <select
            data-testid="excel-download-output-type-select"
            className="form-input"
            value={outputType}
            onChange={(e) =>
              setOutputType(e.target.value as ExcelDownloadJobRow["outputType"])
            }
          >
            <option value="TARGET">평가대상자</option>
            <option value="STATUS">현황</option>
            <option value="ERROR">오류자료</option>
          </select>
          <textarea
            data-testid="excel-download-query-textarea"
            className="form-input"
            value={queryText}
            onChange={(e) => setQueryText(e.target.value)}
            aria-label="현재 조회조건"
          />
          <button
            data-testid="excel-download-create-button"
            className="btn-primary"
            onClick={create}
          >
            <FileSpreadsheet size={16} />
            생성
          </button>
        </div>
        <p className="mt-3 text-sm text-muted">
          서버에서 현재 조회조건과 사용자 데이터 범위 권한을 재검증합니다.
        </p>
      </ScreenCard>
      <StatusView status={status} message={message} />
      {job && (
        <ScreenCard title="생성 결과">
          <p data-testid="excel-download-job-row">
            {job.downloadId} / {job.outputType} / {job.fileToken} / {job.status}
          </p>
        </ScreenCard>
      )}
    </main>
  );
}
