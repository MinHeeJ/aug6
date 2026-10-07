import { useEffect, useState } from "react";
import {
  ApiClientError,
  apiRequest,
  type CurrentUser,
} from "../../api/apiClient";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";

const base = "/api/business/employment-rate-achievements" as const;
type Row = {
  achievementId: number;
  managementNo: string;
  teacherName?: string;
  evaluationYear: string;
  managementItemCode: string;
  achievementDate: string;
  achievementName?: string;
  achievementStatus: string;
  attachmentIds: string[];
};
type Search = { achievements: Row[]; totalElements: number };
type Save = {
  achievement: Row;
  occurredDateWarning: boolean;
  warningMessage?: string;
};
type History = {
  uploadId: string;
  originalFileName: string;
  validationStatus: string;
  totalCount: number;
  successCount: number;
  errorCount: number;
  savedCount: number;
};
type Job = {
  jobId: string;
  jobStatus: string;
  totalCount: number;
  processedCount: number;
  unprocessedCount: number;
  items: {
    itemId: number;
    targetUserId: number;
    processedYn: string;
    unprocessedReason?: string;
  }[];
};
type Choice = { managementItemCode: string; managementItemName: string };
const blank = {
  managementItemCode: "",
  achievementDate: "",
  achievementName: "",
};
const control = "rounded-md border border-ld px-3 py-2 text-sm";
const button =
  "rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-50";

export function EmploymentRateAchievementPage({
  user,
}: {
  user: CurrentUser | null;
}) {
  const allowed = (...roles: string[]) =>
    user?.roles.some((role) => role === "R09" || roles.includes(role)) ?? false;
  const canRead = allowed("R01", "R02", "R04");
  const canWrite = allowed("R01");
  const canExcel = allowed("R07");
  const [tab, setTab] = useState<"individual" | "excel" | "bulk">(
    canRead ? "individual" : "excel",
  );
  const [rows, setRows] = useState<Row[]>([]);
  const [selected, setSelected] = useState<Row | null>(null);
  const [form, setForm] = useState(blank);
  const [choices, setChoices] = useState<Choice[]>([]);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [total, setTotal] = useState(0);
  const [managementNo, setManagementNo] = useState("");
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [permission, setPermission] = useState(false);
  const [success, setSuccess] = useState("");
  const [fields, setFields] = useState<Record<string, string>>({});
  const [file, setFile] = useState<File | null>(null);
  const [histories, setHistories] = useState<History[]>([]);
  const [upload, setUpload] = useState<History | null>(null);
  const [excelErrors, setExcelErrors] = useState<
    { rowNumber: number; errorReason: string }[]
  >([]);
  const [year, setYear] = useState("");
  const [action, setAction] = useState("GENERATE");
  const [condition, setCondition] = useState("{}");
  const [jobId, setJobId] = useState("");
  const [job, setJob] = useState<Job | null>(null);
  const locked =
    !!selected &&
    !["DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED"].includes(
      selected.achievementStatus,
    );
  const listPath =
    `${base}?page=${page}&pageSize=${size}&managementNo=${encodeURIComponent(managementNo)}` as const;

  const handle = (caught: unknown) => {
    if (caught instanceof ApiClientError) {
      setPermission(caught.status === 403);
      setFields(
        Object.fromEntries(
          (caught.apiError?.fields ?? []).map((field) => [
            field.field,
            field.message,
          ]),
        ),
      );
    }
    setError(caught instanceof Error ? caught.message : "처리하지 못했습니다.");
  };
  const load = async () => {
    if (!canRead) return;
    setLoading(true);
    setPermission(false);
    setError("");
    try {
      const response = await apiRequest<Search>(listPath);
      setRows(response.data?.achievements ?? []);
      setTotal(response.data?.totalElements ?? 0);
    } catch (caught) {
      handle(caught);
    } finally {
      setLoading(false);
    }
  };
  const loadHistories = async () => {
    try {
      const response = await apiRequest<History[]>(
        `${base}/excel-uploads/histories`,
      );
      setHistories(response.data ?? []);
    } catch (caught) {
      handle(caught);
    }
  };
  useEffect(() => {
    void load();
  }, [page, size]);
  useEffect(() => {
    if (canExcel && tab === "excel") void loadHistories();
  }, [tab]);
  useEffect(() => {
    const evaluationYear =
      selected?.evaluationYear ?? form.achievementDate.slice(0, 4);
    setChoices([]);
    if (!canRead || !evaluationYear) return;
    let active = true;
    void apiRequest<Choice[]>(
      `${base}/management-items?evaluationYear=${encodeURIComponent(evaluationYear)}`,
    )
      .then((response) => {
        if (active) setChoices(response.data ?? []);
      })
      .catch((caught) => {
        if (active) handle(caught);
      });
    return () => {
      active = false;
    };
  }, [form.achievementDate, selected?.evaluationYear]);

  const select = async (id: number) => {
    try {
      const response = await apiRequest<Row>(`${base}/${id}`);
      const row = response.data;
      if (!row) return;
      setSelected(row);
      setForm({
        managementItemCode: row.managementItemCode,
        achievementDate: row.achievementDate,
        achievementName: row.achievementName ?? "",
      });
      setFields({});
      setSuccess("");
    } catch (caught) {
      handle(caught);
    }
  };
  const save = async () => {
    if (
      !canWrite ||
      locked ||
      !window.confirm("취업률 실적을 저장하시겠습니까?")
    )
      return;
    setBusy(true);
    setError("");
    setFields({});
    try {
      const response = await apiRequest<Save>(
        selected ? `${base}/${selected.achievementId}` : base,
        {
          method: selected ? "PUT" : "POST",
          body: JSON.stringify({
            ...form,
            attachmentIds: selected?.attachmentIds ?? [],
          }),
        },
      );
      const saved = response.data?.achievement;
      if (saved) await select(saved.achievementId);
      await load();
      setSuccess(
        response.data?.occurredDateWarning
          ? (response.data.warningMessage ??
              "발생일 경고와 함께 저장되었습니다.")
          : "저장되었습니다.",
      );
    } catch (caught) {
      handle(caught);
    } finally {
      setBusy(false);
    }
  };
  const download = async (path: `/api/${string}`, name: string) => {
    try {
      const response = await fetch(path, { credentials: "include" });
      if (!response.ok) {
        const body = await response.json();
        throw new ApiClientError(
          response.status,
          body.error?.message ?? "파일을 내려받지 못했습니다.",
          body.error,
        );
      }
      const url = URL.createObjectURL(await response.blob());
      const link = document.createElement("a");
      link.href = url;
      link.download = name;
      link.click();
      URL.revokeObjectURL(url);
    } catch (caught) {
      handle(caught);
    }
  };
  const validateUpload = async () => {
    if (!file) return;
    setBusy(true);
    setError("");
    setUpload(null);
    try {
      const body = new FormData();
      body.append("file", file);
      const response = await fetch(`${base}/excel-uploads`, {
        method: "POST",
        body,
        credentials: "include",
      });
      const result = await response.json();
      if (!response.ok) {
        const id = result.error?.fields?.find(
          (field: { field: string }) => field.field === "uploadId",
        )?.message;
        if (id) {
          const errors = await apiRequest<
            { rowNumber: number; errorReason: string }[]
          >(`${base}/excel-uploads/${id}/errors`);
          setExcelErrors(errors.data ?? []);
        }
        throw new ApiClientError(
          response.status,
          result.error?.message ?? "Excel 검증 실패",
          result.error,
        );
      }
      setUpload(result.data);
      setExcelErrors([]);
      setSuccess(
        "검증 완료: 결과를 확인한 후 반영하세요. 아직 업무 데이터는 반영되지 않았습니다.",
      );
    } catch (caught) {
      handle(caught);
    } finally {
      await loadHistories();
      setBusy(false);
    }
  };
  const commit = async () => {
    if (!upload || !window.confirm("검증된 모든 행을 반영하시겠습니까?"))
      return;
    setBusy(true);
    try {
      await apiRequest(`${base}/excel-uploads/${upload.uploadId}/commit`, {
        method: "POST",
      });
      setUpload(null);
      setSuccess("전체 행이 반영되었습니다.");
      await loadHistories();
    } catch (caught) {
      handle(caught);
    } finally {
      setBusy(false);
    }
  };
  const requestJob = async () => {
    if (!window.confirm("현재 조건으로 정책 확인을 요청하시겠습니까?")) return;
    try {
      const targetCondition = JSON.parse(condition);
      if (
        !targetCondition ||
        Array.isArray(targetCondition) ||
        typeof targetCondition !== "object"
      ) {
        throw new Error("대상 조건은 JSON 객체로 입력하세요.");
      }
      await apiRequest(`${base}/bulk-jobs`, {
        method: "POST",
        body: JSON.stringify({
          evaluationYear: year,
          actionType: action,
          targetCondition,
        }),
      });
    } catch (caught) {
      handle(caught);
    }
  };

  if (!canRead && !canExcel)
    return (
      <PermissionState
        title="권한이 없습니다"
        message="화면 접근 권한이 필요합니다."
      />
    );
  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-EMPLOYMENT-RATE-ACHIEVEMENT"
      data-testid="employment-rate-page"
    >
      <div className="rounded-md bg-lightsecondary p-6">
        <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          취업률 실적 관리
        </h1>
      </div>
      <nav className="flex flex-wrap gap-2" aria-label="취업률 실적 탭">
        {canRead && (
          <button
            className={button}
            data-testid="employment-individual-tab"
            onClick={() => setTab("individual")}
          >
            개별 실적
          </button>
        )}
        {canExcel && (
          <button
            className={button}
            data-testid="employment-excel-tab"
            onClick={() => setTab("excel")}
          >
            Excel 일괄등록
          </button>
        )}
        {canExcel && (
          <button
            className={button}
            data-testid="employment-bulk-tab"
            onClick={() => setTab("bulk")}
          >
            일괄 처리결과
          </button>
        )}
      </nav>
      {permission && (
        <PermissionState
          title="권한이 없습니다"
          message="역할 및 데이터 범위 권한을 확인하세요."
        />
      )}
      {error && <ErrorState title="처리 오류" message={error} />}
      {success && <SuccessState title="처리 결과" message={success} />}
      {Object.entries(fields).map(([field, message]) => (
        <p role="alert" key={field}>
          {field}: {message}
        </p>
      ))}
      {tab === "individual" && canRead && (
        <div className="space-y-4" data-testid="employment-individual-panel">
          <form
            className="flex flex-wrap gap-3 rounded-md bg-white p-6"
            onSubmit={(event) => {
              event.preventDefault();
              if (page) setPage(0);
              else void load();
            }}
          >
            <label>
              관리번호{" "}
              <input
                className={control}
                value={managementNo}
                data-testid="employment-search-input"
                onChange={(event) => setManagementNo(event.target.value)}
              />
            </label>
            <button className={button} data-testid="employment-search-button">
              조회
            </button>
            <label>
              표시 건수{" "}
              <select
                className={control}
                value={size}
                data-testid="employment-page-size"
                onChange={(event) => {
                  setPage(0);
                  setSize(Number(event.target.value));
                }}
              >
                {[20, 50, 100].map((value) => (
                  <option key={value}>{value}</option>
                ))}
              </select>
            </label>
            <button
              className={button}
              type="button"
              data-testid="employment-download-button"
              onClick={() =>
                void download(
                  `${base}/download?page=${page}&pageSize=${size}&managementNo=${encodeURIComponent(managementNo)}`,
                  "취업률실적.xlsx",
                )
              }
            >
              Excel 다운로드
            </button>
          </form>
          {loading ? (
            <LoadingState title="조회 중" message="실적을 조회하고 있습니다." />
          ) : rows.length === 0 ? (
            <EmptyState
              title="실적 없음"
              message="조회조건에 맞는 실적이 없습니다."
            />
          ) : (
            <div className="overflow-x-auto rounded-md bg-white p-6">
              <table className="w-full text-sm">
                <thead>
                  <tr>
                    <th>관리번호</th>
                    <th>성명</th>
                    <th>관리항목</th>
                    <th>발생일</th>
                    <th>상태</th>
                    <th>상세</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((row) => (
                    <tr
                      key={row.achievementId}
                      data-testid={`employment-row-${row.achievementId}`}
                    >
                      <td>{row.managementNo}</td>
                      <td>{row.teacherName}</td>
                      <td>{row.managementItemCode}</td>
                      <td>{row.achievementDate}</td>
                      <td>{row.achievementStatus}</td>
                      <td>
                        <button
                          className={control}
                          data-testid={`employment-detail-${row.achievementId}`}
                          onClick={() => void select(row.achievementId)}
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
          <div className="flex items-center gap-3">
            <button
              className={control}
              disabled={page === 0}
              data-testid="employment-previous"
              onClick={() => setPage(page - 1)}
            >
              이전
            </button>
            <span>
              {page + 1} 페이지 / 총 {total}건
            </span>
            <button
              className={control}
              disabled={(page + 1) * size >= total}
              data-testid="employment-next"
              onClick={() => setPage(page + 1)}
            >
              다음
            </button>
          </div>
          <form
            className="space-y-4 rounded-md bg-white p-6"
            onSubmit={(event) => {
              event.preventDefault();
              void save();
            }}
          >
            <h2 className="text-lg font-semibold">
              {selected ? "실적 상세·수정" : "실적 등록"}
            </h2>
            {selected && (
              <p>
                관리번호: {selected.managementNo} / 평가연도:{" "}
                {selected.evaluationYear} / 상태: {selected.achievementStatus}
              </p>
            )}
            {locked && (
              <p role="alert">확정 또는 처리중 실적은 수정할 수 없습니다.</p>
            )}
            <div className="grid gap-4 md:grid-cols-2">
              <label>
                업적발생일 *{" "}
                <input
                  className={control}
                  type="date"
                  required
                  value={form.achievementDate}
                  disabled={!canWrite || locked}
                  data-testid="employment-date"
                  onChange={(event) =>
                    setForm({ ...form, achievementDate: event.target.value })
                  }
                />
              </label>
              <label>
                관리항목 *{" "}
                <select
                  className={control}
                  required
                  value={form.managementItemCode}
                  disabled={!canWrite || locked}
                  data-testid="employment-item"
                  onChange={(event) =>
                    setForm({ ...form, managementItemCode: event.target.value })
                  }
                >
                  <option value="">관리항목 선택</option>
                  {selected &&
                    !choices.some(
                      (choice) =>
                        choice.managementItemCode ===
                        selected.managementItemCode,
                    ) && (
                      <option value={selected.managementItemCode}>
                        {selected.managementItemCode}
                      </option>
                    )}
                  {choices.map((choice) => (
                    <option
                      key={choice.managementItemCode}
                      value={choice.managementItemCode}
                    >
                      {choice.managementItemName}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                실적명{" "}
                <input
                  className={control}
                  maxLength={500}
                  value={form.achievementName}
                  disabled={!canWrite || locked}
                  data-testid="employment-name"
                  onChange={(event) =>
                    setForm({ ...form, achievementName: event.target.value })
                  }
                />
              </label>
              <p>
                첨부참조: {selected?.attachmentIds?.join(", ") || "없음"} (새
                첨부 업로드 계약 미승인)
              </p>
            </div>
            {canWrite && (
              <button
                className={button}
                disabled={locked || busy}
                data-testid="employment-save"
              >
                저장
              </button>
            )}
            {canWrite && (
              <button
                className={control}
                type="button"
                data-testid="employment-new"
                onClick={() => {
                  setSelected(null);
                  setForm(blank);
                  setFields({});
                }}
              >
                신규 등록
              </button>
            )}
          </form>
        </div>
      )}
      {tab === "excel" && canExcel && (
        <div
          className="space-y-4 rounded-md bg-white p-6"
          data-testid="employment-excel-panel"
        >
          <h2 className="text-lg font-semibold">
            양식 → 업로드 → 전체 검증 → 결과 확인 → 반영
          </h2>
          <button
            className={button}
            data-testid="employment-template"
            onClick={() =>
              void download(
                `${base}/excel-uploads/template`,
                "취업률실적_양식.xlsx",
              )
            }
          >
            양식 다운로드
          </button>
          <label>
            Excel 파일{" "}
            <input
              type="file"
              accept=".xlsx"
              data-testid="employment-file"
              onChange={(event) => setFile(event.target.files?.[0] ?? null)}
            />
          </label>
          <button
            className={button}
            disabled={!file || busy}
            data-testid="employment-validate"
            onClick={() => void validateUpload()}
          >
            업로드·검증
          </button>
          {upload && (
            <div>
              <p>
                총 {upload.totalCount}건 / 정상 {upload.successCount}건 / 오류{" "}
                {upload.errorCount}건 / 반영 0건
              </p>
              <button
                className={button}
                disabled={busy || upload.errorCount > 0}
                data-testid="employment-commit"
                onClick={() => void commit()}
              >
                검증 결과 확인·전체 반영
              </button>
            </div>
          )}
          {excelErrors.map((row) => (
            <p key={row.rowNumber}>
              행 {row.rowNumber}: {row.errorReason}
            </p>
          ))}
          <h3>업로드 이력</h3>
          {histories.length === 0 && (
            <EmptyState title="이력 없음" message="업로드 이력이 없습니다." />
          )}
          {histories.map((history) => (
            <div
              className="border-b border-ld py-3"
              key={history.uploadId}
              data-testid={`employment-upload-${history.uploadId}`}
            >
              <p>
                {history.originalFileName} / {history.validationStatus}: 총{" "}
                {history.totalCount}, 정상 {history.successCount}, 오류{" "}
                {history.errorCount}, 반영 {history.savedCount}건
              </p>
              {history.errorCount > 0 && (
                <button
                  className={control}
                  data-testid={`employment-errors-${history.uploadId}`}
                  onClick={() =>
                    void download(
                      `${base}/excel-uploads/${history.uploadId}/errors/download`,
                      "취업률실적_오류.xlsx",
                    )
                  }
                >
                  오류 파일 다운로드
                </button>
              )}
              {history.validationStatus === "VALIDATED" && (
                <button
                  className={control}
                  data-testid={`employment-review-${history.uploadId}`}
                  onClick={() => setUpload(history)}
                >
                  검증 결과 확인
                </button>
              )}
            </div>
          ))}
        </div>
      )}
      {tab === "bulk" && canExcel && (
        <div
          className="space-y-4 rounded-md bg-white p-6"
          data-testid="employment-bulk-panel"
        >
          <h2 className="text-lg font-semibold">취업률 일괄 생성·삭제</h2>
          <p role="status">
            생성자격·삭제상태 정책 미승인: 대상 수와 자격은 추정하지 않으며
            실행은 차단됩니다.
          </p>
          <label>
            평가연도 *{" "}
            <input
              className={control}
              value={year}
              data-testid="employment-bulk-year"
              onChange={(event) => setYear(event.target.value)}
            />
          </label>
          <label>
            작업{" "}
            <select
              className={control}
              value={action}
              data-testid="employment-bulk-action"
              onChange={(event) => setAction(event.target.value)}
            >
              <option value="GENERATE">생성</option>
              <option value="DELETE">삭제</option>
            </select>
          </label>
          <label>
            대상 조건{" "}
            <textarea
              className={control}
              value={condition}
              data-testid="employment-bulk-condition"
              onChange={(event) => setCondition(event.target.value)}
            />
          </label>
          <button
            className={button}
            data-testid="employment-bulk-request"
            onClick={() => void requestJob()}
          >
            정책 확인 요청
          </button>
          <label>
            작업 식별자{" "}
            <input
              className={control}
              value={jobId}
              data-testid="employment-job-id"
              onChange={(event) => setJobId(event.target.value)}
            />
          </label>
          <button
            className={button}
            disabled={!jobId}
            data-testid="employment-job-load"
            onClick={() => {
              void apiRequest<Job>(
                `${base}/bulk-jobs/${encodeURIComponent(jobId)}`,
              )
                .then((response) => setJob(response.data ?? null))
                .catch(handle);
            }}
          >
            처리결과 조회
          </button>
          {job && (
            <div>
              <p>
                {job.jobId}: {job.jobStatus} / 총 {job.totalCount} / 처리{" "}
                {job.processedCount} / 미처리 {job.unprocessedCount}
              </p>
              {job.items.map((item) => (
                <p
                  key={item.itemId}
                  data-testid={`employment-job-item-${item.itemId}`}
                >
                  대상 {item.targetUserId} / 처리 {item.processedYn} /{" "}
                  {item.unprocessedReason}
                </p>
              ))}
            </div>
          )}
        </div>
      )}
    </section>
  );
}
