import { useEffect, useRef, useState, type ReactNode } from "react";
import { ApiClientError, type ApiResponse } from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

const BASE = "/api/business/employment-rate-achievements";
const panel = "rounded-md border border-ld bg-white p-5 shadow-sm space-y-4";
const button =
  "rounded-md border border-primary px-4 py-2 text-sm font-semibold text-primary disabled:opacity-40 disabled:cursor-not-allowed";
type Achievement = {
  achievementId: number;
  managementNo: string;
  teacherName: string;
  managementItemCode: string;
  achievementDate: string;
  achievementName?: string;
  certificationStatus: string;
  attachmentIds?: string[];
  attachmentRef?: string;
};
type Form = {
  managementItemCode: string;
  achievementDate: string;
  achievementName: string;
  attachmentIds: string[];
};
type UploadError = {
  rowNumber: number;
  columnName: string;
  errorReason: string;
};
type Upload = {
  uploadId: string;
  totalCount: number;
  successCount: number;
  errorCount: number;
  savedCount?: number;
  errors?: UploadError[];
};
type History = Upload & {
  originalFileName: string;
  uploaderName?: string;
  uploadedBy?: string;
  processedAt?: string;
  uploadedAt?: string;
};
type List = {
  achievements: Achievement[];
  totalElements: number;
};
const blank: Form = {
  managementItemCode: "",
  achievementDate: "",
  achievementName: "",
  attachmentIds: [],
};
const statuses: Record<string, string> = {
  DRAFT: "작성중",
  SUBMITTED: "제출",
  DEPARTMENT_CONFIRMED: "학과장확인",
  DEPARTMENT_REJECTED: "학과장미승인",
  CERTIFIED: "인증",
  CERTIFICATION_REJECTED: "인증반려",
  EVALUATION_CONFIRMED: "평가확정",
  DELETED: "삭제",
};

/** Unlike the shared JSON client, multipart requests must not set Content-Type. */
async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(path, {
    credentials: "include",
    ...init,
  });

  const body = (await response.json()) as ApiResponse<T>;

  if (!response.ok || body.success === false) {
    throw new ApiClientError(
      response.status,
      body.error?.message ?? "요청을 처리하지 못했습니다.",
      body.error,
    );
  }

  return body.data as T;
}
function toForm(row: Achievement): Form {
  return {
    managementItemCode: row.managementItemCode,
    achievementDate: row.achievementDate,
    achievementName: row.achievementName ?? "",
    attachmentIds:
      row.attachmentIds ?? (row.attachmentRef ? [row.attachmentRef] : []),
  };
}
function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="block text-sm text-muted">
      {label}
      <span className="mt-2 block [&_input]:w-full [&_input]:rounded-md [&_input]:border [&_input]:border-ld [&_input]:p-2 [&_select]:w-full [&_select]:rounded-md [&_select]:border [&_select]:border-ld [&_select]:p-2">
        {children}
      </span>
    </label>
  );
}
function ApiData({ value }: { value: unknown }) {
  return (
    <pre
      className="overflow-auto rounded-md bg-lightsecondary p-4 text-sm"
      aria-label="서버 처리결과"
    >
      {JSON.stringify(value, null, 2)}
    </pre>
  );
}

export function EmploymentRateAchievementManagementPage({
  roles = [],
}: {
  roles?: string[];
}) {
  const individual = roles.some((role) => ["R01", "R02", "R04"].includes(role));

  const r07 = roles.includes("R07");

  const writer = roles.includes("R01");

  const [tab, setTab] = useState(individual ? "individual" : "excel");

  const [filters, setFilters] = useState({
    managementNo: "",
    teacherName: "",
    managementItemCode: "",
    certificationStatus: "",
  });

  const [applied, setApplied] = useState(filters);

  const [page, setPage] = useState(0);

  const [size, setSize] = useState(20);

  const [rows, setRows] = useState<Achievement[]>([]);

  const [total, setTotal] = useState(0);

  const [selected, setSelected] = useState<Achievement | null>(null);

  const [form, setForm] = useState<Form>(blank);

  const [busy, setBusy] = useState(false);

  const [listLoading, setListLoading] = useState(false);

  const [error, setError] = useState("");

  const [success, setSuccess] = useState("");

  const [permission, setPermission] = useState("");

  const [fields, setFields] = useState<Record<string, string>>({});

  const [file, setFile] = useState<File | null>(null);

  const [upload, setUpload] = useState<Upload | null>(null);

  const [histories, setHistories] = useState<History[]>([]);

  const [year, setYear] = useState("");

  const [action, setAction] = useState("GENERATE");

  const [condition, setCondition] = useState("{}");

  const [preview, setPreview] = useState<unknown>(null);

  const [jobId, setJobId] = useState("");

  const [job, setJob] = useState<unknown>(null);

  const listSequence = useRef(0);

  const detailSequence = useRef(0);

  const locked =
    !!selected &&
    !["DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED"].includes(
      selected.certificationStatus,
    );

  const query = () => {
    const q = new URLSearchParams({
      page: String(page),
      pageSize: String(size),
    });

    Object.entries(applied).forEach(([key, value]) => {
      if (value.trim()) q.set(key, value.trim());
    });

    return q.toString();
  };

  function fail(caught: unknown) {
    setSuccess("");

    if (caught instanceof ApiClientError) {
      setPermission(
        caught.status === 403
          ? "해당 작업의 역할 또는 데이터 범위 권한이 없습니다."
          : caught.status === 401
            ? "세션이 만료되었습니다. 다시 로그인하세요."
            : "",
      );

      setFields(
        Object.fromEntries(
          (caught.apiError?.fields ?? []).map((field) => [
            field.field,
            field.message,
          ]),
        ),
      );
    }

    setError(
      caught instanceof Error ? caught.message : "처리 중 오류가 발생했습니다.",
    );
  }

  async function load() {
    if (!individual) return;

    const sequence = ++listSequence.current;

    setListLoading(true);

    try {
      const data = await request<List>(`${BASE}?${query()}`);

      if (sequence !== listSequence.current) return;

      setRows(data.achievements ?? []);
      setTotal(data.totalElements ?? 0);
    } catch (caught) {
      if (sequence === listSequence.current) {
        setRows([]);
        setTotal(0);
        fail(caught);
      }
    } finally {
      if (sequence === listSequence.current) setListLoading(false);
    }
  }

  useEffect(() => {
    void load();
  }, [page, size, applied, individual]);

  async function run(operation: () => Promise<void>) {
    setBusy(true);
    setError("");
    setSuccess("");
    setFields({});
    setPermission("");

    try {
      await operation();
    } catch (caught) {
      fail(caught);
    } finally {
      setBusy(false);
    }
  }

  async function detail(id: number) {
    const sequence = ++detailSequence.current;

    const data = await request<
      | Achievement
      | {
          achievement: Achievement;
        }
    >(`${BASE}/${id}`);

    if (sequence !== detailSequence.current) return;

    const row = "achievement" in data ? data.achievement : data;

    setSelected(row);
    setForm(toForm(row));
  }

  function save() {
    if (!writer || locked || busy) return;

    const errors: Record<string, string> = {};

    if (!form.managementItemCode.trim())
      errors.managementItemCode = "관리항목코드를 입력하세요.";

    if (!form.achievementDate)
      errors.achievementDate = "업적발생일을 입력하세요.";

    setFields(errors);

    if (
      Object.keys(errors).length ||
      !window.confirm("취업률 실적을 저장하시겠습니까?")
    )
      return;

    void run(async () => {
      const id = selected?.achievementId;

      const result = await request<{
        achievement?: Achievement;
        warningMessage?: string;
        occurredDateWarning?: boolean;
        achievementDateWarning?: boolean;
      }>(id ? `${BASE}/${id}` : BASE, {
        method: id ? "PUT" : "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          managementItemCode: form.managementItemCode.trim(),
          achievementDate: form.achievementDate,
          achievementName: form.achievementName,
          attachmentRef: form.attachmentIds[0] || null,
        }),
      });

      const savedId = id ?? result.achievement?.achievementId;

      if (savedId) await detail(savedId);
      else {
        setSelected(null);
        setForm(blank);
      }

      await load();

      setSuccess(result.warningMessage ?? "저장되었습니다.");
    });
  }

  async function loadHistories() {
    setHistories(await request<History[]>(`${BASE}/excel-uploads/histories`));
  }

  async function validateUpload() {
    if (!file || !r07) {
      setError("Excel 파일을 선택하세요.");
      return;
    }

    setUpload(null);

    const body = new FormData();
    body.append("file", file);

    const response = await fetch(`${BASE}/excel-uploads`, {
      method: "POST",
      credentials: "include",
      body,
    });

    const result = (await response.json()) as ApiResponse<Upload>;
    // Validation failures still carry the diagnostic uploadId. Never discard it.

    if (result.data?.uploadId) setUpload(result.data);

    if (!response.ok || result.success === false) {
      if (result.data?.uploadId) await loadHistories();

      throw new ApiClientError(
        response.status,
        result.error?.message ?? "오류 행이 있어 전체 반영할 수 없습니다.",
        result.error,
      );
    }

    setSuccess("검증 완료: 결과를 확인한 후 전체 반영하세요.");

    await loadHistories();
  }

  function commit() {
    if (
      !r07 ||
      !upload ||
      upload.errorCount !== 0 ||
      upload.successCount <= 0 ||
      upload.savedCount ||
      busy
    )
      return;

    if (
      !window.confirm(
        `${upload.totalCount}건을 전체 반영하시겠습니까? 중복 데이터는 자동 갱신하지 않습니다.`,
      )
    )
      return;

    void run(async () => {
      const data = await request<{
        savedCount: number;
      }>(
        `${BASE}/excel-uploads/${encodeURIComponent(upload.uploadId)}/commit`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ confirmed: true }),
        },
      );

      setUpload({
        ...upload,
        savedCount: data.savedCount,
      });

      setSuccess(`${data.savedCount}건을 전체 반영했습니다.`);

      await loadHistories();
    });
  }

  if (!individual && !r07)
    return (
      <PermissionState
        title="취업률 실적 관리 권한이 없습니다"
        message="R01, R02, R04 또는 R07 역할이 필요합니다."
      />
    );

  return (
    <section
      data-testid="employment-rate-section-1"
      className="space-y-6"
      data-screen-id="SCR-EMPLOYMENT-RATE-ACHIEVEMENTS"
    >
      <header className="rounded-md bg-lightsecondary p-6">
        <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          취업률 실적 관리
        </h1>
        <p className="mt-2 text-sm text-muted">
          개별 실적과 Excel 검증·전체 반영 및 일괄 처리결과를 확인합니다.
        </p>
      </header>

      <div
        role="tablist"
        aria-label="취업률 실적 업무"
        className="flex flex-wrap gap-2"
      >
        {[
          ["individual", "개별 실적", individual],
          ["excel", "Excel 등록", r07],
          ["bulk", "일괄 처리", r07],
        ].map(([key, label, allowed]) => (
          <button
            data-testid={`employment-rate-button-2-${String(key)}`}
            key={String(key)}
            className={button}
            role="tab"
            aria-selected={tab === key}
            disabled={!allowed || busy}
            onClick={() => {
              setTab(String(key));
              setError("");
              setSuccess("");
              setPermission("");
            }}
          >
            {label}
          </button>
        ))}
      </div>

      {busy || listLoading ? (
        <LoadingState title="취업률 실적 처리 중" />
      ) : null}

      {permission ? (
        <PermissionState title="접근 제한" message={permission} />
      ) : null}

      {error ? <ErrorState title="취업률 실적 오류" message={error} /> : null}

      {success ? <SuccessState title="처리 완료" message={success} /> : null}

      {tab === "individual" && individual ? (
        <div
          data-testid="employment-rate-div-3"
          role="tabpanel"
          className="space-y-6"
        >
          <form
            className={panel}
            onSubmit={(event) => {
              event.preventDefault();
              setPage(0);
              setApplied({ ...filters });
              setSelected(null);
              setForm(blank);
            }}
          >
            <h2 className="text-lg font-semibold text-dark">검색조건</h2>
            <div className="grid gap-4 md:grid-cols-4">
              {[
                ["managementNo", "관리번호"],
                ["teacherName", "성명"],
                ["managementItemCode", "관리항목"],
                ["certificationStatus", "인증상태"],
              ].map(([key, label]) => (
                <Field key={key} label={label}>
                  <input
                    data-testid={`employment-rate-filter-${key}`}
                    value={filters[key as keyof typeof filters]}
                    onChange={(event) =>
                      setFilters({
                        ...filters,
                        [key]: event.target.value,
                      })
                    }
                  />
                </Field>
              ))}
            </div>

            <div className="flex gap-2">
              <button
                data-testid="employment-rate-button-5"
                className={button}
                disabled={busy}
              >
                조회
              </button>
              <button
                data-testid="employment-rate-button-6"
                type="button"
                className={button}
                disabled={busy}
                onClick={() => {
                  const reset = {
                    managementNo: "",
                    teacherName: "",
                    managementItemCode: "",
                    certificationStatus: "",
                  };
                  setFilters(reset);
                  setApplied(reset);
                  setPage(0);
                  setSelected(null);
                  setForm(blank);
                }}
              >
                조건 초기화
              </button>
            </div>
          </form>

          <section className={panel}>
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h2 className="text-lg font-semibold text-dark">
                실적 목록 ·{total}건
              </h2>
              <div className="flex gap-3">
                <Field label="표시 건수">
                  <select
                    data-testid="employment-rate-select-7"
                    value={size}
                    onChange={(event) => {
                      setSize(Number(event.target.value));
                      setPage(0);
                    }}
                  >
                    {[20, 50, 100].map((n) => (
                      <option key={n} value={n}>
                        {n}건
                      </option>
                    ))}
                  </select>
                </Field>
                <a
                  data-testid="employment-rate-a-8"
                  className={button}
                  href={`${BASE}/download?${query()}`}
                >
                  Excel 다운로드
                </a>
              </div>
            </div>

            {!listLoading && !rows.length ? (
              <EmptyState title="조회된 실적이 없습니다" />
            ) : null}

            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr data-testid="employment-rate-tr-9">
                    {[
                      "관리번호",
                      "성명",
                      "관리항목",
                      "업적발생일",
                      "실적명",
                      "인증상태",
                      "상세",
                    ].map((label) => (
                      <th key={label} className="p-3 text-left">
                        {label}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {rows.map((row) => (
                    <tr
                      data-testid={`employment-rate-tr-10-${row.achievementId}`}
                      key={row.achievementId}
                      className="border-t border-ld"
                    >
                      <td className="p-3">{row.managementNo}</td>
                      <td>{row.teacherName}</td>
                      <td>{row.managementItemCode}</td>
                      <td>{row.achievementDate}</td>
                      <td>{row.achievementName}</td>
                      <td>
                        {statuses[row.certificationStatus] ??
                          row.certificationStatus}
                      </td>
                      <td>
                        <button
                          data-testid={`employment-rate-detail-${row.achievementId}`}
                          className={button}
                          aria-label={`${row.managementNo} 상세`}
                          disabled={busy || listLoading}
                          onClick={() =>
                            void run(() => detail(row.achievementId))
                          }
                        >
                          상세
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="flex items-center gap-3">
              <button
                data-testid="employment-rate-button-12"
                className={button}
                disabled={page === 0 || busy || listLoading}
                onClick={() => setPage(page - 1)}
              >
                이전
              </button>
              <span>{page + 1} 페이지</span>
              <button
                data-testid="employment-rate-button-13"
                className={button}
                disabled={(page + 1) * size >= total || busy || listLoading}
                onClick={() => setPage(page + 1)}
              >
                다음
              </button>
            </div>
          </section>

          <section className={panel}>
            <h2 className="text-lg font-semibold text-dark">
              실적 상세 / 등록
            </h2>
            <p>
              관리번호:
              {selected?.managementNo ?? "저장 후 부여"} · 인증상태:
              {selected
                ? (statuses[selected.certificationStatus] ??
                  selected.certificationStatus)
                : "작성중"}{" "}
              (읽기 전용)
            </p>

            {locked ? (
              <p className="text-error">평가확정 실적은 수정할 수 없습니다.</p>
            ) : null}

            {!writer ? <p>개별 등록·수정은 R01만 가능합니다.</p> : null}

            <div className="grid gap-4 md:grid-cols-2">
              {[
                ["managementItemCode", "관리항목코드 *", "text"],
                ["achievementDate", "업적발생일 *", "date"],
                ["achievementName", "실적명", "text"],
              ].map(([key, label, type]) => (
                <Field key={key} label={label}>
                  <input
                    data-testid={`employment-rate-form-${key}`}
                    type={type}
                    value={
                      form[
                        key as
                          | "managementItemCode"
                          | "achievementDate"
                          | "achievementName"
                      ]
                    }
                    disabled={!writer || locked || busy}
                    aria-required={key !== "achievementName"}
                    onChange={(event) =>
                      setForm({
                        ...form,
                        [key]: event.target.value,
                      })
                    }
                  />
                  {fields[key] ? (
                    <span role="alert" className="text-error">
                      {fields[key]}
                    </span>
                  ) : null}
                </Field>
              ))}
              <Field label="첨부참조">
                <input
                  data-testid="employment-rate-input-15"
                  value={form.attachmentIds[0] ?? ""}
                  disabled={!writer || locked || busy}
                  onChange={(event) =>
                    setForm({
                      ...form,
                      attachmentIds: event.target.value.trim()
                        ? [event.target.value.trim()]
                        : [],
                    })
                  }
                />
              </Field>
            </div>

            <div className="flex gap-2">
              <button
                data-testid="employment-rate-button-16"
                className={button}
                disabled={!writer || locked || busy || listLoading}
                onClick={save}
              >
                저장
              </button>
              <button
                data-testid="employment-rate-button-17"
                className={button}
                disabled={!writer || busy}
                onClick={() => {
                  detailSequence.current++;
                  setSelected(null);
                  setForm(blank);
                  setFields({});
                  setSuccess("");
                }}
              >
                신규 등록
              </button>
              <button
                data-testid="employment-rate-button-18"
                className={button}
                disabled={busy}
                onClick={() => {
                  setForm(selected ? toForm(selected) : blank);
                  setFields({});
                  setSuccess("");
                }}
              >
                취소
              </button>
            </div>
          </section>
        </div>
      ) : null}

      {tab === "individual" && !individual ? (
        <PermissionState
          title="개별 조회 권한 없음"
          message="R07은 Excel 등록 및 일괄 처리 탭을 사용하세요."
        />
      ) : null}

      {tab === "excel" && r07 ? (
        <section
          data-testid="employment-rate-section-19"
          role="tabpanel"
          className={panel}
        >
          <h2 className="text-lg font-semibold text-dark">
            Excel 일괄등록 · R07
          </h2>
          <p>
            템플릿 다운로드 → 파일 업로드 → 검증결과 확인 → 전체 반영. 오류·중복
            행이 하나라도 있으면 0건 반영하며 자동 갱신하지 않습니다.
          </p>

          <a
            data-testid="employment-rate-a-20"
            className={button}
            href={`${BASE}/excel-uploads/template`}
          >
            템플릿 다운로드
          </a>
          <Field label="Excel 파일">
            <input
              data-testid="employment-rate-input-21"
              type="file"
              accept=".xlsx"
              disabled={busy}
              onChange={(event) => {
                setFile(event.target.files?.[0] ?? null);
                setUpload(null);
                setSuccess("");
              }}
            />
          </Field>
          <button
            data-testid="employment-rate-button-22"
            className={button}
            disabled={busy || !file}
            onClick={() => void run(validateUpload)}
          >
            업로드·검증
          </button>

          {upload ? (
            <div className="space-y-3">
              <p>
                총{upload.totalCount}건 · 정상
                {upload.successCount}건 · 오류
                {upload.errorCount}건 · 반영
                {upload.savedCount ?? 0}건
              </p>
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr data-testid="employment-rate-tr-23">
                      <th>행 번호</th>
                      <th>오류 열</th>
                      <th>오류 사유</th>
                    </tr>
                  </thead>
                  <tbody>
                    {upload.errors?.map((item, index) => (
                      <tr
                        data-testid={`employment-rate-tr-24-${index}`}
                        key={index}
                      >
                        <td>{item.rowNumber}</td>
                        <td>{item.columnName}</td>
                        <td>{item.errorReason}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <button
                data-testid="employment-rate-button-25"
                className={button}
                disabled={busy}
                onClick={() =>
                  void run(async () => {
                    const errors = await request<UploadError[]>(
                      `${BASE}/excel-uploads/${encodeURIComponent(upload.uploadId)}/errors`,
                    );
                    setUpload({
                      ...upload,
                      errors,
                    });
                  })
                }
              >
                오류 상세 조회
              </button>
              <a
                data-testid="employment-rate-a-26"
                className={button}
                href={`${BASE}/excel-uploads/${encodeURIComponent(upload.uploadId)}/errors/download`}
              >
                오류 파일 다운로드
              </a>
              <button
                data-testid="employment-rate-button-27"
                className={button}
                disabled={
                  busy ||
                  upload.errorCount !== 0 ||
                  upload.successCount <= 0 ||
                  !!upload.savedCount
                }
                onClick={commit}
              >
                전체 반영
              </button>
            </div>
          ) : (
            <EmptyState title="파일을 업로드하여 검증결과를 확인하세요" />
          )}

          <h3 className="font-semibold">업로드 이력</h3>
          <button
            data-testid="employment-rate-button-28"
            className={button}
            disabled={busy}
            onClick={() => void run(loadHistories)}
          >
            이력 조회
          </button>
          {histories.length ? (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr data-testid="employment-rate-tr-29">
                    {[
                      "파일명",
                      "업로더",
                      "일시",
                      "총",
                      "정상",
                      "오류",
                      "반영",
                      "진단",
                    ].map((label) => (
                      <th key={label}>{label}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {histories.map((item) => (
                    <tr
                      data-testid={`employment-rate-tr-30-${item.uploadId}`}
                      key={item.uploadId}
                    >
                      <td>{item.originalFileName}</td>
                      <td>{item.uploaderName ?? item.uploadedBy ?? "-"}</td>
                      <td>{item.processedAt ?? item.uploadedAt ?? "-"}</td>
                      <td>{item.totalCount}</td>
                      <td>{item.successCount}</td>
                      <td>{item.errorCount}</td>
                      <td>{item.savedCount ?? 0}</td>
                      <td>
                        <a
                          data-testid={`employment-rate-history-errors-${item.uploadId}`}
                          href={`${BASE}/excel-uploads/${encodeURIComponent(item.uploadId)}/errors/download`}
                        >
                          오류 파일
                        </a>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p>조회된 업로드 이력이 없습니다. 이력 조회로 확인하세요.</p>
          )}
        </section>
      ) : null}

      {tab === "bulk" && r07 ? (
        <section
          data-testid="employment-rate-section-32"
          role="tabpanel"
          className={panel}
        >
          <h2 className="text-lg font-semibold text-dark">
            일괄 생성·삭제 / 처리결과 · R07
          </h2>
          <p role="status" className="rounded-md bg-lightsecondary p-4">
            정책 승인 대기 (D4 / OQ-83-01·02·03): 후보 미리보기만 가능하며 일괄
            실행은 차단됩니다. 미승인 정책으로 작업이나 실적을 생성·변경하지
            않습니다.
          </p>

          <div className="grid gap-4 md:grid-cols-3">
            <Field label="평가년도">
              <input
                data-testid="employment-rate-input-33"
                value={year}
                onChange={(event) => {
                  setYear(event.target.value);
                  setPreview(null);
                }}
              />
            </Field>
            <Field label="작업 유형">
              <select
                data-testid="employment-rate-select-34"
                value={action}
                onChange={(event) => {
                  setAction(event.target.value);
                  setPreview(null);
                }}
              >
                <option value="GENERATE">일괄 생성</option>
                <option value="DELETE">일괄 삭제</option>
              </select>
            </Field>
            <Field label="대상 조건 (JSON)">
              <input
                data-testid="employment-rate-input-35"
                value={condition}
                onChange={(event) => {
                  setCondition(event.target.value);
                  setPreview(null);
                }}
              />
            </Field>
          </div>

          <button
            data-testid="employment-rate-button-36"
            className={button}
            disabled={busy || !year.trim()}
            onClick={() =>
              void run(async () => {
                const parsed: unknown = JSON.parse(condition);
                if (
                  !parsed ||
                  typeof parsed !== "object" ||
                  Array.isArray(parsed)
                )
                  throw new Error("대상 조건은 JSON 객체여야 합니다.");
                const q = new URLSearchParams({
                  evaluationYear: year.trim(),
                  actionType: action,
                  targetConditionJson: JSON.stringify(parsed),
                });
                setPreview(await request(`${BASE}/bulk-jobs/preview?${q}`));
              })
            }
          >
            대상 미리보기
          </button>
          <button
            data-testid="employment-rate-button-37"
            className={button}
            disabled
            title="정책 미승인: 서버도 409 및 작업 미생성을 보장합니다."
          >
            일괄 실행
          </button>

          {preview !== null ? (
            <>
              <p>
                후보 결과이며 실행 자격이나 삭제 허용 상태를 의미하지 않습니다.
              </p>
              <ApiData value={preview} />
            </>
          ) : null}

          <Field label="작업 ID">
            <input
              data-testid="employment-rate-input-38"
              value={jobId}
              onChange={(event) => {
                setJobId(event.target.value);
                setJob(null);
              }}
            />
          </Field>
          <button
            data-testid="employment-rate-button-39"
            className={button}
            disabled={busy || !jobId.trim()}
            onClick={() =>
              void run(async () => {
                setJob(null);
                setJob(
                  await request(
                    `${BASE}/bulk-jobs/${encodeURIComponent(jobId.trim())}`,
                  ),
                );
              })
            }
          >
            처리결과 조회
          </button>
          {job !== null ? (
            <>
              <h3 className="font-semibold">
                처리건수 · 미처리 대상 · 건별 처리결과
              </h3>
              <ApiData value={job} />
            </>
          ) : (
            <EmptyState title="작업 ID로 저장된 처리결과를 조회하세요" />
          )}
        </section>
      ) : null}
    </section>
  );
}
