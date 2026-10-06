import { useEffect, useRef, useState, type ReactNode } from "react";
import {
  ApiClientError,
  apiRequest,
  type ApiResponse,
} from "../../api/apiClient";
import { useAuth } from "../../app/AuthProvider";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";
type Achievement = {
  achievementId: number;
  managementNo: string;
  teacherName: string;
  managementItemCode: string;
  achievementDate: string;
  achievementName: string;
  attachmentIds: string[];
  certificationStatus:
    | "DRAFT"
    | "SUBMITTED"
    | "DEPARTMENT_CONFIRMED"
    | "DEPARTMENT_REJECTED"
    | "CERTIFIED"
    | "CERTIFICATION_REJECTED"
    | "EVALUATION_CONFIRMED"
    | "DELETED";
};
type ListResult = {
  achievements: Achievement[];
  page: number;
  pageSize: number;
  totalElements: number;
};
type SaveResult = {
  achievement: Achievement;
  occurredDateWarning: boolean;
  warningMessage?: string;
};
type Form = {
  managementItemCode: string;
  achievementDate: string;
  achievementName: string;
  attachments: string;
};
type UploadResult = {
  uploadId: string;
  originalFileName?: string;
  totalCount: number;
  successCount: number;
  errorCount: number;
  savedCount?: number;
  warnings?: string[];
  errors: {
    rowNumber: number;
    columnName: string;
    errorReason: string;
  }[];
};
type UploadHistory = UploadResult & {
  uploadedBy?: string | number;
  uploaderName?: string;
  processedAt?: string;
  uploadedAt?: string;
};
type BulkRequest = {
  evaluationYear: string;
  actionType: "GENERATE" | "DELETE";
  targetCondition: Record<string, unknown>;
};
type BulkPreview = {
  request: BulkRequest;
  existingAchievementCount: number;
  policyApproved: boolean;
};
type BulkJob = {
  jobId: string;
  evaluationYear: string;
  actionType: string;
  jobStatus: string;
  totalCount: number;
  processedCount: number;
  unprocessedCount: number;
  items: Record<string, unknown>[];
};
const BASE = "/api/business/employment-rate-achievements" as const;
const emptyForm: Form = {
  managementItemCode: "",
  achievementDate: "",
  achievementName: "",
  attachments: "",
};
const card = "rounded-md border border-ld bg-white p-5 shadow-sm";
const button =
  "rounded-md border border-ld px-4 py-2 text-sm font-semibold text-primary";
const primary =
  "rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white";
function testId(prefix: string, ...parts: (string | number)[]) {
  return [
    prefix,
    ...parts.map((part) =>
      String(part)
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, "-"),
    ),
  ].join("-");
}
const statusNames: Record<string, string> = {
  DRAFT: "작성중",
  SUBMITTED: "제출",
  DEPARTMENT_CONFIRMED: "학과장확인",
  DEPARTMENT_REJECTED: "학과장미승인",
  CERTIFIED: "인증",
  CERTIFICATION_REJECTED: "인증반려",
  EVALUATION_CONFIRMED: "평가확정",
  DELETED: "삭제",
};
function toForm(row: Achievement): Form {
  return {
    managementItemCode: row.managementItemCode,
    achievementDate: row.achievementDate,
    achievementName: row.achievementName,
    attachments: row.attachmentIds.join(", "),
  };
}
export function EmploymentRateAchievementPage() {
  const { user, status } = useAuth();
  const roles = user?.roles ?? [];
  const canRead = roles.some((role) => ["R01", "R02", "R04"].includes(role));
  const canEdit = roles.includes("R01");
  const canBulk = roles.includes("R07");
  const [tab, setTab] = useState<"individual" | "excel" | "bulk">(
    canRead ? "individual" : "excel",
  );
  const [rows, setRows] = useState<Achievement[]>([]);
  const [selected, setSelected] = useState<Achievement | null>(null);
  const [form, setForm] = useState<Form>(emptyForm);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [fields, setFields] = useState<Record<string, string>>({});
  const [file, setFile] = useState<File | null>(null);
  const [upload, setUpload] = useState<UploadResult | null>(null);
  const [committed, setCommitted] = useState(false);
  const [histories, setHistories] = useState<UploadHistory[]>([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [year, setYear] = useState("");
  const [action, setAction] = useState<"GENERATE" | "DELETE">("GENERATE");
  const [condition, setCondition] = useState("{}");
  const [preview, setPreview] = useState<BulkPreview | null>(null);
  const [jobId, setJobId] = useState("");
  const [job, setJob] = useState<BulkJob | null>(null);
  const listSequence = useRef(0);
  const detailSequence = useRef(0);
  const locked =
    !canEdit || (selected !== null && selected.certificationStatus !== "DRAFT");
  const query = `page=${page}&pageSize=${pageSize}`;
  function fail(caught: unknown) {
    setSuccess(null);
    setError(
      caught instanceof Error ? caught.message : "처리 중 오류가 발생했습니다.",
    );
    setFields(
      caught instanceof ApiClientError
        ? Object.fromEntries(
            (caught.apiError?.fields ?? []).map((field) => [
              field.field,
              field.message,
            ]),
          )
        : {},
    );
  }
  function clearMessages() {
    setError(null);
    setSuccess(null);
    setFields({});
  }
  async function load() {
    if (!canRead) return;
    const sequence = ++listSequence.current;
    setLoading(true);
    try {
      const response = await apiRequest<ListResult>(`${BASE}?${query}`);
      if (sequence === listSequence.current) {
        setRows(response.data?.achievements ?? []);
        setTotal(response.data?.totalElements ?? 0);
      }
    } catch (caught) {
      if (sequence === listSequence.current) {
        setRows([]);
        setTotal(0);
        fail(caught);
      }
    } finally {
      if (sequence === listSequence.current) setLoading(false);
    }
  }
  useEffect(() => {
    void load();
    return () => {
      listSequence.current++;
    };
  }, [canRead, page, pageSize]);
  useEffect(() => {
    if (canRead) setTab("individual");
    else if (canBulk) setTab("excel");
  }, [canRead, canBulk]);
  async function detail(id: number) {
    const sequence = ++detailSequence.current;
    clearMessages();
    setBusy(true);
    try {
      const response = await apiRequest<Achievement>(`${BASE}/${id}`);
      if (!response.data) throw new Error("상세 응답이 없습니다.");
      if (sequence === detailSequence.current) {
        setSelected(response.data);
        setForm(toForm(response.data));
      }
    } catch (caught) {
      if (sequence === detailSequence.current) fail(caught);
    } finally {
      if (sequence === detailSequence.current) setBusy(false);
    }
  }
  async function save() {
    if (locked || busy) return;
    clearMessages();
    const validation: Record<string, string> = {};
    if (!form.managementItemCode.trim())
      validation.managementItemCode = "관리항목을 입력하세요.";
    if (!/^\d{4}-\d{2}-\d{2}$/.test(form.achievementDate))
      validation.achievementDate = "업적발생일을 입력하세요.";
    if (Object.keys(validation).length) {
      setFields(validation);
      return;
    }
    if (!window.confirm("취업률 실적을 저장하시겠습니까?")) return;
    setBusy(true);
    try {
      const response = await apiRequest<SaveResult>(
        selected ? `${BASE}/${selected.achievementId}` : BASE,
        {
          method: selected ? "PUT" : "POST",
          body: JSON.stringify({
            managementItemCode: form.managementItemCode.trim(),
            achievementDate: form.achievementDate,
            achievementName: form.achievementName.trim(),
            attachmentIds: form.attachments
              .split(",")
              .map((value) => value.trim())
              .filter(Boolean),
          }),
        },
      );
      const result = response.data;
      if (!result?.achievement)
        throw new Error("저장 결과가 없습니다. 목록을 다시 확인하세요.");
      // Verify persisted detail rather than presenting the outgoing form as saved data.
      const persisted = await apiRequest<Achievement>(
        `${BASE}/${result.achievement.achievementId}`,
      );
      if (!persisted.data) throw new Error("저장 후 상세 응답이 없습니다.");
      setSelected(persisted.data);
      setForm(toForm(persisted.data));
      setSuccess(
        result.occurredDateWarning
          ? (result.warningMessage ??
              "평가대상 기간 밖 발생일 경고와 함께 저장되었습니다.")
          : "저장되었습니다.",
      );
      await load();
    } catch (caught) {
      fail(caught);
    } finally {
      setBusy(false);
    }
  }
  async function loadHistories() {
    if (!canBulk) return;
    setHistoryLoading(true);
    try {
      const response = await apiRequest<UploadHistory[]>(
        `${BASE}/excel-uploads/histories`,
      );
      setHistories(response.data ?? []);
    } catch (caught) {
      fail(caught);
    } finally {
      setHistoryLoading(false);
    }
  }
  useEffect(() => {
    if (tab === "excel" && canBulk) void loadHistories();
  }, [tab, canBulk]);
  async function validateUpload() {
    if (!canBulk || busy) return;
    clearMessages();
    setUpload(null);
    setCommitted(false);
    if (!file) {
      setError("현행 양식의 파일을 선택하세요.");
      return;
    }
    setBusy(true);
    try {
      const data = new FormData();
      data.append("file", file);
      // apiRequest forces application/json: use a local multipart adapter without altering the shared client.
      const response = await fetch(`${BASE}/excel-uploads`, {
        method: "POST",
        credentials: "include",
        body: data,
      });
      const body = (await response.json()) as ApiResponse<UploadResult>;
      if (body.data?.uploadId) setUpload(body.data);
      if (!response.ok || !body.success)
        throw new ApiClientError(
          response.status,
          body.error?.message ?? "파일 검증에 실패했습니다.",
          body.error,
        );
      if (!body.data?.uploadId) throw new Error("검증 결과가 없습니다.");
      setSuccess(
        "파일 검증이 완료되었습니다. 모든 행이 정상일 때만 전체 반영할 수 있습니다.",
      );
    } catch (caught) {
      fail(caught);
    } finally {
      await loadHistories();
      setBusy(false);
    }
  }
  async function commitUpload() {
    if (!canBulk || busy || !upload || upload.errorCount !== 0 || committed)
      return;
    if (
      !window.confirm(
        `${upload.totalCount}건을 전체 반영하시겠습니까? 오류·중복행은 부분 반영하지 않습니다.`,
      )
    )
      return;
    clearMessages();
    setBusy(true);
    try {
      const response = await apiRequest<{
        savedCount: number;
        warnings?: string[];
      }>(
        `${BASE}/excel-uploads/${encodeURIComponent(upload.uploadId)}/commit`,
        {
          method: "POST",
        },
      );
      if (response.data?.savedCount === undefined)
        throw new Error("반영 결과가 없습니다. 이력을 확인하세요.");
      const warnings = response.data.warnings;
      setUpload((current) =>
        current
          ? { ...current, warnings: warnings ?? current.warnings }
          : current,
      );
      setCommitted(true);
      setSuccess(`${response.data.savedCount}건을 전체 반영했습니다.`);
      await loadHistories();
      if (canRead) await load();
    } catch (caught) {
      fail(caught);
    } finally {
      setBusy(false);
    }
  }
  async function previewBulk() {
    if (!canBulk || busy) return;
    clearMessages();
    setPreview(null);
    if (!year.trim()) {
      setFields({
        evaluationYear: "평가연도를 입력하세요.",
      });
      return;
    }
    setBusy(true);
    try {
      const parsed: unknown = JSON.parse(condition);
      if (!parsed || typeof parsed !== "object" || Array.isArray(parsed))
        throw new Error("대상·실행조건은 JSON 객체로 입력하세요.");
      const targetCondition = parsed as Record<string, unknown>;
      if (
        Object.keys(targetCondition).some(
          (key) =>
            ![
              "managementNo",
              "managementItemCode",
              "certificationStatus",
            ].includes(key),
        )
      ) {
        throw new Error(
          "대상 조건은 managementNo, managementItemCode, certificationStatus만 지원합니다.",
        );
      }
      const request: BulkRequest = {
        evaluationYear: year.trim(),
        actionType: action,
        targetCondition,
      };
      const response = await apiRequest<Omit<BulkPreview, "request">>(
        `${BASE}/bulk-jobs/preview`,
        {
          method: "POST",
          body: JSON.stringify(request),
        },
      );
      if (
        !response.data ||
        !Number.isInteger(response.data.existingAchievementCount) ||
        response.data.existingAchievementCount < 0 ||
        typeof response.data.policyApproved !== "boolean"
      ) {
        throw new Error("기존 실적 건수 조회 결과가 없습니다.");
      }
      setPreview({
        request,
        ...response.data,
      });
    } catch (caught) {
      fail(caught);
    } finally {
      setBusy(false);
    }
  }
  async function getJob(id = jobId) {
    if (!canBulk || !id.trim()) {
      setError("작업 ID를 입력하세요.");
      return;
    }
    setJob(null);
    const response = await apiRequest<BulkJob>(
      `${BASE}/bulk-jobs/${encodeURIComponent(id.trim())}`,
    );
    if (!response.data) throw new Error("작업 결과가 없습니다.");
    setJob(response.data);
  }
  async function executeBulk() {
    if (!canBulk || !preview || busy) return;
    if (
      !window.confirm(
        `${preview.request.evaluationYear}년 ${
          preview.request.actionType === "DELETE" ? "일괄 삭제" : "일괄 생성"
        } 요청을 실행하시겠습니까? 서버가 승인된 정책과 상태를 검증합니다.`,
      )
    )
      return;
    clearMessages();
    setBusy(true);
    try {
      const response = await apiRequest<{
        jobId: string;
      }>(`${BASE}/bulk-jobs`, {
        method: "POST",
        body: JSON.stringify(preview.request),
      });
      if (!response.data?.jobId) throw new Error("접수된 작업 ID가 없습니다.");
      setJobId(response.data.jobId);
      setPreview(null);
      await getJob(response.data.jobId);
      setSuccess("일괄 작업이 접수되었습니다. 처리결과를 확인하세요.");
    } catch (caught) {
      setPreview(null);
      fail(caught);
    } finally {
      setBusy(false);
    }
  }
  if (status === "loading")
    return (
      <LoadingState
        title="권한 확인 중"
        message="로그인 역할을 확인하고 있습니다."
      />
    );
  if (!canRead && !canBulk)
    return (
      <PermissionState
        title="취업률 실적 관리 권한이 없습니다"
        message="R01/R02/R04 조회 또는 R07 Excel·일괄 권한이 필요합니다."
      />
    );
  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-EMPLOYMENT-RATE-ACHIEVEMENT"
      data-testid="employment-rate-achievement-page"
    >
      <div
        className="rounded-md bg-lightsecondary p-6 shadow-none"
        data-testid="employment-rate-div-1"
      >
        <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          취업률 실적 관리
        </h1>
        <p className="mt-2 text-sm text-muted">
          개별 실적 조회·저장과 Excel 검증·일괄 처리결과를 확인합니다.
        </p>
      </div>
      {error && <ErrorState title="취업률 실적 오류" message={error} />}
      {success && <SuccessState title="처리 완료" message={success} />}
      {busy && (
        <LoadingState
          title="처리 중"
          message="요청 결과를 확인하고 있습니다."
        />
      )}
      <nav
        role="tablist"
        aria-label="실적 관리 기능"
        className="flex flex-wrap gap-2"
        data-testid="employment-rate-nav-2"
      >
        {canRead && (
          <button
            role="tab"
            aria-selected={tab === "individual"}
            className={tab === "individual" ? primary : button}
            disabled={busy}
            onClick={() => {
              clearMessages();
              setTab("individual");
            }}
            data-testid="employment-rate-button-3"
          >
            개별 실적
          </button>
        )}
        {canBulk && (
          <>
            <button
              role="tab"
              aria-selected={tab === "excel"}
              className={tab === "excel" ? primary : button}
              disabled={busy}
              onClick={() => {
                clearMessages();
                setTab("excel");
              }}
              data-testid="employment-rate-button-4"
            >
              Excel 일괄등록
            </button>
            <button
              role="tab"
              aria-selected={tab === "bulk"}
              className={tab === "bulk" ? primary : button}
              disabled={busy}
              onClick={() => {
                clearMessages();
                setTab("bulk");
              }}
              data-testid="employment-rate-button-5"
            >
              일괄 처리
            </button>
          </>
        )}
      </nav>
      {tab === "individual" && canRead && (
        <div
          role="tabpanel"
          aria-label="개별 실적"
          className="space-y-6"
          data-testid="employment-rate-div-6"
        >
          <section className={card} data-testid="employment-rate-section-7">
            <div
              className="flex flex-wrap items-center gap-3"
              data-testid="employment-rate-div-8"
            >
              <h2 className="mr-auto text-lg font-semibold">실적 조회</h2>
              <Field label="페이지 크기">
                <select
                  value={pageSize}
                  disabled={loading || busy}
                  onChange={(event) => {
                    clearMessages();
                    setPage(0);
                    setPageSize(Number(event.target.value));
                  }}
                  data-testid="employment-rate-select-9"
                >
                  <option value={20}>20건</option>
                  <option value={50}>50건</option>
                  <option value={100}>100건</option>
                </select>
              </Field>
              <button
                className={button}
                disabled={loading || busy}
                onClick={() => {
                  clearMessages();
                  void load();
                }}
                data-testid="employment-rate-button-10"
              >
                조회
              </button>
              <a
                className={button}
                href={`${BASE}/download?${query}`}
                data-testid="employment-rate-a-11"
              >
                Excel 다운로드
              </a>
            </div>
            {loading ? (
              <LoadingState
                title="목록 조회 중"
                message="취업률 실적을 불러오고 있습니다."
              />
            ) : rows.length === 0 ? (
              <EmptyState
                title="조회 결과 없음"
                message="조회 가능한 실적이 없습니다."
              />
            ) : (
              <div
                className="mt-4 overflow-x-auto"
                data-testid="employment-rate-div-12"
              >
                <table>
                  <thead>
                    <tr data-testid="employment-rate-tr-13">
                      <th>관리번호</th>
                      <th>성명</th>
                      <th>관리항목</th>
                      <th>업적발생일</th>
                      <th>인증상태</th>
                      <th>상세</th>
                    </tr>
                  </thead>
                  <tbody>
                    {rows.map((row) => (
                      <tr
                        key={row.achievementId}
                        className="border-b border-ld"
                        data-testid={testId(
                          "employment-rate-tr-14",
                          row.achievementId,
                        )}
                      >
                        <td>{row.managementNo}</td>
                        <td>{row.teacherName}</td>
                        <td>{row.managementItemCode}</td>
                        <td>{row.achievementDate}</td>
                        <td>
                          {statusNames[row.certificationStatus] ??
                            row.certificationStatus}
                        </td>
                        <td>
                          <button
                            className={button}
                            aria-label={`${row.managementNo} 상세`}
                            disabled={busy}
                            onClick={() => void detail(row.achievementId)}
                            data-testid={testId(
                              "employment-rate-button-15",
                              row.achievementId,
                            )}
                          >
                            상세
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            <div
              className="mt-4 flex items-center justify-end gap-3 text-sm"
              data-testid="employment-rate-div-16"
            >
              <span>
                총 {total}건 / {page + 1}페이지
              </span>
              <button
                className={button}
                disabled={page === 0 || loading || busy}
                onClick={() => {
                  clearMessages();
                  setPage(page - 1);
                }}
                data-testid="employment-rate-button-17"
              >
                이전
              </button>
              <button
                className={button}
                disabled={(page + 1) * pageSize >= total || loading || busy}
                onClick={() => {
                  clearMessages();
                  setPage(page + 1);
                }}
                data-testid="employment-rate-button-18"
              >
                다음
              </button>
            </div>
          </section>
          <section className={card} data-testid="employment-rate-section-19">
            <div
              className="mb-4 flex justify-between gap-3"
              data-testid="employment-rate-div-20"
            >
              <h2 className="text-lg font-semibold">
                {selected ? "실적 상세·수정" : "실적 등록"}
              </h2>
              {canEdit && (
                <button
                  className={button}
                  disabled={busy}
                  onClick={() => {
                    detailSequence.current++;
                    setSelected(null);
                    setForm(emptyForm);
                    clearMessages();
                  }}
                  data-testid="employment-rate-button-21"
                >
                  신규 등록
                </button>
              )}
            </div>
            {selected && (
              <p className="mb-4 text-sm">
                관리번호: {selected.managementNo} / 성명: {selected.teacherName}{" "}
                / 인증상태:
                {statusNames[selected.certificationStatus] ??
                  selected.certificationStatus}
              </p>
            )}
            {selected?.certificationStatus === "EVALUATION_CONFIRMED" && (
              <p className="mb-4 text-sm text-error">
                평가확정 실적은 수정할 수 없습니다.
              </p>
            )}
            {!canEdit && (
              <p className="mb-4 text-sm text-muted">
                조회 전용입니다. 등록·수정은 R01만 가능합니다.
              </p>
            )}
            <div
              className="grid gap-4 md:grid-cols-2"
              data-testid="employment-rate-div-22"
            >
              <Field label="관리항목 *" error={fields.managementItemCode}>
                <input
                  value={form.managementItemCode}
                  disabled={locked || busy}
                  onChange={(event) =>
                    setForm({
                      ...form,
                      managementItemCode: event.target.value,
                    })
                  }
                  data-testid="employment-rate-input-23"
                />
              </Field>
              <Field label="업적발생일 *" error={fields.achievementDate}>
                <input
                  type="date"
                  value={form.achievementDate}
                  disabled={locked || busy}
                  onChange={(event) =>
                    setForm({
                      ...form,
                      achievementDate: event.target.value,
                    })
                  }
                  data-testid="employment-rate-input-24"
                />
              </Field>
              <Field label="실적명" error={fields.achievementName}>
                <input
                  value={form.achievementName}
                  disabled={locked || busy}
                  onChange={(event) =>
                    setForm({
                      ...form,
                      achievementName: event.target.value,
                    })
                  }
                  data-testid="employment-rate-input-25"
                />
              </Field>
              <Field
                label="첨부 참조 ID (쉼표 구분)"
                error={fields.attachmentIds}
              >
                <input
                  value={form.attachments}
                  disabled={locked || busy}
                  onChange={(event) =>
                    setForm({
                      ...form,
                      attachments: event.target.value,
                    })
                  }
                  data-testid="employment-rate-input-26"
                />
              </Field>
            </div>
            <p className="mt-3 text-sm text-muted">
              * 필수 항목. 인증상태는 조회 전용입니다. 입력기간·권한은 서버에서
              검증하며 평가대상 기간 밖 발생일은 경고 후 저장할 수 있습니다.
            </p>
            {canEdit && (
              <div
                className="mt-4 flex gap-3"
                data-testid="employment-rate-div-27"
              >
                <button
                  className={primary}
                  disabled={locked || busy}
                  onClick={() => void save()}
                  data-testid="employment-rate-button-28"
                >
                  저장
                </button>
                <button
                  className={button}
                  disabled={busy}
                  onClick={() => {
                    setForm(selected ? toForm(selected) : emptyForm);
                    clearMessages();
                  }}
                  data-testid="employment-rate-button-29"
                >
                  취소
                </button>
              </div>
            )}
          </section>
        </div>
      )}
      {tab === "excel" && canBulk && (
        <div
          role="tabpanel"
          aria-label="Excel 일괄등록"
          className="space-y-6"
          data-testid="employment-rate-div-30"
        >
          <section className={card} data-testid="employment-rate-section-31">
            <h2 className="mb-3 text-lg font-semibold">
              템플릿 → 업로드·검증 → 검증결과 → 전체 반영
            </h2>
            <p className="mb-4 text-sm text-muted">
              현행 템플릿 형식을 사용하세요. 오류·중복행이 하나라도 있으면 0건
              반영되며 기존 실적을 자동 갱신하지 않습니다.
            </p>
            <div
              className="flex flex-wrap items-center gap-3"
              data-testid="employment-rate-div-32"
            >
              <a
                className={button}
                href={`${BASE}/excel-uploads/template`}
                data-testid="employment-rate-a-33"
              >
                템플릿 다운로드
              </a>
              <Field label="업로드 파일">
                <input
                  type="file"
                  disabled={busy}
                  onChange={(event) => {
                    setFile(event.target.files?.[0] ?? null);
                    setUpload(null);
                    setCommitted(false);
                    clearMessages();
                  }}
                  data-testid="employment-rate-input-34"
                />
              </Field>
              <button
                className={primary}
                disabled={busy}
                onClick={() => void validateUpload()}
                data-testid="employment-rate-button-35"
              >
                업로드·검증
              </button>
            </div>
            {upload && (
              <div
                className="mt-5 space-y-3"
                data-testid="employment-rate-div-36"
              >
                <h3 className="font-semibold">검증결과</h3>
                <p>
                  정상행 {upload.successCount}건 / 오류행 {upload.errorCount}건
                  / 전체 {upload.totalCount}건
                </p>
                {upload.errorCount > 0 && (
                  <>
                    <p className="text-error">
                      오류 행이 있어 전체 반영하지 않습니다.
                    </p>
                    <a
                      className={button}
                      href={`${BASE}/excel-uploads/${encodeURIComponent(upload.uploadId)}/errors/download`}
                      data-testid="employment-rate-a-37"
                    >
                      오류파일 다운로드
                    </a>
                  </>
                )}
                <ul className="list-disc pl-5 text-sm">
                  {(upload.errors ?? []).map((item, index) => (
                    <li
                      key={index}
                      data-testid={testId("employment-rate-li-38", index)}
                    >
                      {item.rowNumber}행 {item.columnName}: {item.errorReason}
                    </li>
                  ))}
                </ul>
                {(upload.warnings ?? []).length > 0 && (
                  <div role="status" className="text-sm">
                    <p className="font-semibold">평가대상기간 경고</p>
                    <ul className="list-disc pl-5">
                      {(upload.warnings ?? []).map((warning, index) => (
                        <li key={index}>{warning}</li>
                      ))}
                    </ul>
                  </div>
                )}
                <button
                  className={primary}
                  disabled={busy || upload.errorCount !== 0 || committed}
                  onClick={() => void commitUpload()}
                  data-testid="employment-rate-button-39"
                >
                  전체 반영
                </button>
                {committed && <p>이미 전체 반영된 파일입니다.</p>}
              </div>
            )}
          </section>
          <section className={card} data-testid="employment-rate-section-40">
            <div
              className="flex items-center justify-between"
              data-testid="employment-rate-div-41"
            >
              <h2 className="text-lg font-semibold">업로드 이력</h2>
              <button
                className={button}
                disabled={historyLoading || busy}
                onClick={() => {
                  clearMessages();
                  void loadHistories();
                }}
                data-testid="employment-rate-button-42"
              >
                이력 새로고침
              </button>
            </div>
            {historyLoading ? (
              <LoadingState
                title="이력 조회 중"
                message="업로드 이력을 불러오고 있습니다."
              />
            ) : histories.length === 0 ? (
              <EmptyState
                title="업로드 이력 없음"
                message="등록된 업로드 이력이 없습니다."
              />
            ) : (
              <div
                className="mt-4 overflow-x-auto"
                data-testid="employment-rate-div-43"
              >
                <table>
                  <thead>
                    <tr data-testid="employment-rate-tr-44">
                      <th>파일명</th>
                      <th>업로더</th>
                      <th>일시</th>
                      <th>총/정상/오류/반영</th>
                      <th>오류파일</th>
                    </tr>
                  </thead>
                  <tbody>
                    {histories.map((history) => (
                      <tr
                        key={history.uploadId}
                        data-testid={testId(
                          "employment-rate-tr-45",
                          history.uploadId,
                        )}
                      >
                        <td>{history.originalFileName}</td>
                        <td>
                          {history.uploaderName ?? history.uploadedBy ?? "-"}
                        </td>
                        <td>
                          {history.uploadedAt ?? history.processedAt ?? "-"}
                        </td>
                        <td>
                          {history.totalCount}/{history.successCount}/
                          {history.errorCount}/{history.savedCount ?? 0}
                        </td>
                        <td>
                          {history.errorCount > 0 && (
                            <a
                              className="text-primary"
                              href={`${BASE}/excel-uploads/${encodeURIComponent(history.uploadId)}/errors/download`}
                              data-testid={testId(
                                "employment-rate-a-46",
                                history.uploadId,
                              )}
                            >
                              오류파일 다운로드
                            </a>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>
        </div>
      )}
      {tab === "bulk" && canBulk && (
        <div
          role="tabpanel"
          aria-label="일괄 처리"
          className="space-y-6"
          data-testid="employment-rate-div-47"
        >
          <section className={card} data-testid="employment-rate-section-48">
            <h2 className="mb-4 text-lg font-semibold">일괄 생성·삭제</h2>
            <p className="mb-4 text-sm text-muted">
              OQ-83-01 실행조건·삭제 허용 상태는 미확정입니다. 서버 승인 없이
              실행하지 않으며 정책 충돌(409)은 데이터 변경 없이 안내합니다.
            </p>
            <div
              className="grid gap-4 md:grid-cols-2"
              data-testid="employment-rate-div-49"
            >
              <Field label="평가연도 *" error={fields.evaluationYear}>
                <input
                  value={year}
                  disabled={busy}
                  onChange={(event) => {
                    setYear(event.target.value);
                    setPreview(null);
                  }}
                  data-testid="employment-rate-input-50"
                />
              </Field>
              <Field label="작업 유형">
                <select
                  value={action}
                  disabled={busy}
                  onChange={(event) => {
                    setAction(event.target.value as "GENERATE" | "DELETE");
                    setPreview(null);
                  }}
                  data-testid="employment-rate-select-51"
                >
                  <option value="GENERATE">일괄 생성</option>
                  <option value="DELETE">일괄 삭제</option>
                </select>
              </Field>
            </div>
            <div className="mt-4" data-testid="employment-rate-div-52">
              <Field
                label="대상·실행조건 (JSON)"
                error={fields.targetCondition}
              >
                <textarea
                  rows={4}
                  value={condition}
                  disabled={busy}
                  onChange={(event) => {
                    setCondition(event.target.value);
                    setPreview(null);
                  }}
                  data-testid="employment-rate-textarea-53"
                />
              </Field>
            </div>
            <div
              className="mt-4 flex flex-wrap gap-3"
              data-testid="employment-rate-div-54"
            >
              <button
                className={button}
                disabled={busy}
                onClick={previewBulk}
                data-testid="employment-rate-button-55"
              >
                대상·실행조건 미리보기
              </button>
              <button
                className={primary}
                disabled={busy || !preview}
                onClick={() => void executeBulk()}
                data-testid="employment-rate-button-56"
              >
                일괄 실행
              </button>
            </div>
            {preview && (
              <div
                className="mt-4 rounded-md bg-lightsecondary p-4"
                data-testid="bulk-preview-panel"
              >
                <h3 className="font-semibold">실행요청 미리보기</h3>
                <p className="text-sm">
                  기존 실적 건수: {preview.existingAchievementCount}건
                </p>
                <p className="text-sm">
                  기존 DB 실적 조회 결과이며 생성 대상 건수나 실행 승인이
                  아닙니다.
                </p>
                <p className="text-sm">
                  실행 정책:{preview.policyApproved ? "승인" : "미승인"}
                </p>
                <pre className="mt-3 overflow-x-auto whitespace-pre-wrap break-all text-sm">
                  {JSON.stringify(preview.request, null, 2)}
                </pre>
              </div>
            )}
          </section>
          <section className={card} data-testid="employment-rate-section-57">
            <h2 className="mb-4 text-lg font-semibold">일괄 처리결과</h2>
            <div
              className="flex flex-wrap items-end gap-3"
              data-testid="employment-rate-div-58"
            >
              <Field label="작업 ID">
                <input
                  value={jobId}
                  disabled={busy}
                  onChange={(event) => {
                    setJobId(event.target.value);
                    setJob(null);
                  }}
                  data-testid="employment-rate-input-59"
                />
              </Field>
              <button
                className={button}
                disabled={busy}
                onClick={() => {
                  clearMessages();
                  setBusy(true);
                  void getJob()
                    .catch(fail)
                    .finally(() => setBusy(false));
                }}
                data-testid="employment-rate-button-60"
              >
                처리결과 조회
              </button>
            </div>
            <p className="mt-3 text-sm text-muted">
              접수된 작업 또는 기존 작업 ID를 입력하여 결과를 조회합니다.
            </p>
            {job && (
              <div
                className="mt-4 space-y-3"
                data-testid="employment-rate-div-61"
              >
                <p>
                  작업 {job.jobId}/{job.evaluationYear}년 / {job.actionType}/
                  {job.jobStatus}
                </p>
                <p>
                  전체 {job.totalCount}건 / 처리 {job.processedCount}건 / 미처리{" "}
                  {job.unprocessedCount}건
                </p>
                {job.items.length === 0 ? (
                  <EmptyState
                    title="건별 결과 없음"
                    message="아직 처리된 건별 결과가 없습니다. 다시 조회하세요."
                  />
                ) : (
                  <div
                    className="overflow-x-auto"
                    data-testid="employment-rate-div-62"
                  >
                    <table>
                      <thead>
                        <tr data-testid="employment-rate-tr-63">
                          <th>건별 결과 (대상·처리여부·미처리 사유)</th>
                        </tr>
                      </thead>
                      <tbody>
                        {job.items.map((item, index) => (
                          <tr
                            key={index}
                            data-testid={testId("employment-rate-tr-64", index)}
                          >
                            <td>
                              <dl className="flex flex-wrap gap-x-5 gap-y-2">
                                {Object.entries(item).map(([key, value]) => (
                                  <div
                                    key={key}
                                    data-testid={testId(
                                      "employment-rate-div-65",
                                      index,
                                      key,
                                    )}
                                  >
                                    <dt className="inline font-semibold">
                                      {key}:
                                    </dt>
                                    <dd className="inline">
                                      {typeof value === "object"
                                        ? JSON.stringify(value)
                                        : typeof value === "boolean"
                                          ? value
                                            ? "처리"
                                            : "미처리"
                                          : String(value ?? "-")}
                                    </dd>
                                  </div>
                                ))}
                              </dl>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            )}
          </section>
        </div>
      )}
      {!canRead && canBulk && tab === "excel" && (
        <a
          className={button}
          href={`${BASE}/download?${query}`}
          data-testid="employment-rate-a-66"
        >
          Excel 다운로드
        </a>
      )}
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
  children: ReactNode;
}) {
  const fieldIds: Record<string, string> = {
    "페이지 크기": "page-size",
    "관리항목 *": "management-item-code",
    "업적발생일 *": "achievement-date",
    실적명: "achievement-name",
    "첨부 참조 ID (쉼표 구분)": "attachment-ids",
    "업로드 파일": "upload-file",
    "평가연도 *": "evaluation-year",
    "작업 유형": "action-type",
    "대상·실행조건 (JSON)": "target-condition",
    "작업 ID": "job-id",
  };
  return (
    <div
      className="text-sm"
      data-testid={`employment-rate-field-${fieldIds[label]}`}
    >
      <label className="flex flex-col gap-2">
        <span className="font-semibold text-dark">{label}</span>
        {children}
      </label>
      {error && (
        <span role="alert" className="mt-2 block text-error">
          {error}
        </span>
      )}
    </div>
  );
}
