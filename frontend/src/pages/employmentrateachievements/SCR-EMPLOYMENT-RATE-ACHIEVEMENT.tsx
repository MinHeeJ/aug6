import { useEffect, useState } from "react";
import { ApiClientError, type CurrentUser } from "../../api/apiClient";
import {
  employmentRateDownload,
  employmentRateRequest,
} from "./employmentRateApi";

export type EmploymentRateRow = {
  achievementId: number;
  managementNo: string;
  teacherUserId: number;
  teacherName: string;
  evaluationYear: string;
  managementItemCode: string;
  achievementDate: string;
  achievementName: string;
  achievementStatus: string;
};
type Item = {
  managementItemCode: string;
  managementItemName: string;
  evaluationYear: string;
};
type Listing = {
  achievements: EmploymentRateRow[];
  totalElements: number;
  managementItems: Item[];
};
type Upload = {
  uploadId: string;
  totalCount: number;
  successCount: number;
  errorCount: number;
  savedCount: number;
};
type History = Upload & { originalFileName: string; validationStatus: string };
type Job = {
  jobId: string;
  processedCount: number;
  unprocessedCount: number;
  items: {
    targetUserId: number;
    processedYn: string;
    unprocessedReason?: string;
  }[];
};
const initial = {
  managementItemCode: "",
  achievementDate: "",
  achievementName: "",
};
const inputClass =
  "mt-1 w-full rounded-md border border-ld bg-white px-3 py-2 text-sm";
const buttonClass =
  "rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:opacity-40";

/** Uses the existing shell; roles decide which individual or Excel/batch panel is reachable. */
export function EmploymentRateAchievementPage({
  user,
}: {
  user: CurrentUser | null;
}) {
  const allowed = (roles: string[]) =>
    user?.roles.some((role) => roles.includes(role) || role === "R09") ?? false;
  const canRead = allowed(["R01", "R02", "R04"]);
  const canWrite = allowed(["R01"]);
  const canExcel = allowed(["R07"]);
  const [tab, setTab] = useState(canRead ? "individual" : "excel");
  const [rows, setRows] = useState<EmploymentRateRow[]>([]);
  const [items, setItems] = useState<Item[]>([]);
  const [selected, setSelected] = useState<EmploymentRateRow | null>(null);
  const [form, setForm] = useState(initial);
  const [filter, setFilter] = useState("");
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [denied, setDenied] = useState(false);
  const [fields, setFields] = useState<Record<string, string>>({});
  const [success, setSuccess] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [upload, setUpload] = useState<Upload | null>(null);
  const [histories, setHistories] = useState<History[]>([]);
  const [errors, setErrors] = useState<
    { rowNumber: number; errorReason: string }[]
  >([]);
  const [jobId, setJobId] = useState("");
  const [job, setJob] = useState<Job | null>(null);
  const locked =
    !!selected &&
    !["DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED"].includes(
      selected.achievementStatus,
    );
  const owned =
    !selected ||
    selected.teacherUserId === user?.userId ||
    user?.roles.includes("R09");
  const query = `?page=${page}&pageSize=${size}&managementNo=${encodeURIComponent(filter)}`;
  const fail = (caught: unknown) => {
    setError(
      caught instanceof Error ? caught.message : "요청을 처리하지 못했습니다.",
    );
    if (caught instanceof ApiClientError) {
      setDenied(caught.status === 403);
      setFields(
        Object.fromEntries(
          (caught.apiError?.fields ?? []).map((field) => [
            field.field,
            field.message,
          ]),
        ),
      );
    }
  };
  const act = async (work: () => Promise<void>) => {
    setBusy(true);
    setError("");
    setDenied(false);
    setSuccess("");
    setFields({});
    try {
      await work();
    } catch (caught) {
      fail(caught);
    } finally {
      setBusy(false);
    }
  };
  const load = async () => {
    setLoading(true);
    try {
      if (canRead) {
        const result = await employmentRateRequest<Listing>(query);
        setRows(result.achievements);
        setTotal(result.totalElements);
        setItems(result.managementItems ?? []);
      }
      if (canExcel)
        setHistories(
          await employmentRateRequest<History[]>("/excel-uploads/histories"),
        );
    } catch (caught) {
      fail(caught);
    } finally {
      setLoading(false);
    }
  };
  useEffect(() => {
    void load();
  }, [page, size]);
  const select = (id: number) =>
    act(async () => {
      const row = await employmentRateRequest<EmploymentRateRow>(`/${id}`);
      setSelected(row);
      setForm({
        managementItemCode: row.managementItemCode,
        achievementDate: row.achievementDate,
        achievementName: row.achievementName,
      });
    });
  const save = () => {
    if (!window.confirm("취업률 실적을 저장하시겠습니까?")) return;
    void act(async () => {
      const saved = await employmentRateRequest<{
        achievement: EmploymentRateRow;
        occurredDateWarning: boolean;
        warningMessage: string;
      }>(selected ? `/${selected.achievementId}` : "", {
        method: selected ? "PUT" : "POST",
        body: JSON.stringify(form),
      });
      setSelected(saved.achievement);
      await load();
      setSuccess(
        saved.occurredDateWarning ? saved.warningMessage : "저장되었습니다.",
      );
    });
  };
  const validate = () =>
    act(async () => {
      if (!file) return;
      const data = new FormData();
      data.append("file", file);
      const result = await employmentRateRequest<Upload>("/excel-uploads", {
        method: "POST",
        body: data,
      });
      setUpload(result);
      setErrors(
        await employmentRateRequest(
          `/excel-uploads/${encodeURIComponent(result.uploadId)}/errors`,
        ),
      );
      await load();
      setSuccess("검증이 완료되었습니다. 결과를 확인한 뒤 반영하세요.");
    });
  const commit = () => {
    if (!upload || !window.confirm("오류 없는 전체 실적을 반영하시겠습니까?"))
      return;
    void act(async () => {
      const result = await employmentRateRequest<{ savedCount: number }>(
        `/excel-uploads/${encodeURIComponent(upload.uploadId)}/commit`,
        { method: "POST" },
      );
      setUpload({ ...upload, savedCount: result.savedCount });
      await load();
      setSuccess(`${result.savedCount}건을 반영했습니다.`);
    });
  };
  if (!canRead && !canExcel)
    return <section role="alert">취업률 실적 관리 권한이 없습니다.</section>;
  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-EMPLOYMENT-RATE-ACHIEVEMENT"
      data-testid="employment-rate-page"
    >
      <header className="rounded-md bg-lightsecondary p-6">
        <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          취업률 실적 관리
        </h1>
      </header>
      <div role="tablist" className="flex gap-3">
        {canRead && (
          <button
            type="button"
            role="tab"
            aria-selected={tab === "individual"}
            data-testid="employment-rate-individual-tab"
            onClick={() => setTab("individual")}
          >
            개별 실적
          </button>
        )}
        {canExcel && (
          <button
            type="button"
            role="tab"
            aria-selected={tab === "excel"}
            data-testid="employment-rate-excel-tab"
            onClick={() => setTab("excel")}
          >
            Excel 등록·일괄 결과
          </button>
        )}
      </div>
      {loading && <p role="status">조회 중입니다.</p>}
      {error && (
        <p role="alert" className="rounded-md bg-lighterror p-3">
          {denied ? "권한 없음: " : ""}
          {error}
        </p>
      )}
      {success && (
        <p role="status" className="rounded-md bg-lightsuccess p-3">
          {success}
        </p>
      )}
      {tab === "individual" && canRead && (
        <div
          role="tabpanel"
          data-testid="employment-rate-individual-panel"
          className="space-y-4"
        >
          <div className="flex flex-wrap gap-3 rounded-md border border-ld p-4">
            <label>
              관리번호
              <input
                value={filter}
                className={inputClass}
                data-testid="employment-rate-filter"
                onChange={(event) => setFilter(event.target.value)}
              />
            </label>
            <button
              type="button"
              className={buttonClass}
              data-testid="employment-rate-search"
              onClick={() => {
                setPage(0);
                void load();
              }}
            >
              조회
            </button>
            <button
              type="button"
              className={buttonClass}
              data-testid="employment-rate-download"
              onClick={() =>
                void act(() =>
                  employmentRateDownload(
                    `/download${query}`,
                    "employment-rate.xlsx",
                  ),
                )
              }
            >
              Excel 다운로드
            </button>
            <label>
              표시 건수
              <select
                value={size}
                className={inputClass}
                data-testid="employment-rate-page-size"
                onChange={(event) => {
                  setPage(0);
                  setSize(Number(event.target.value));
                }}
              >
                {[20, 50, 100].map((value) => (
                  <option key={value} value={value}>
                    {value}건
                  </option>
                ))}
              </select>
            </label>
          </div>
          {!loading && !rows.length && !error && <p>조회 결과가 없습니다.</p>}
          <div className="overflow-x-auto rounded-md border border-ld">
            <table className="w-full text-left text-sm">
              <thead>
                <tr>
                  <th>관리번호</th>
                  <th>교원</th>
                  <th>실적명</th>
                  <th>발생일</th>
                  <th>상태</th>
                  <th>상세</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr
                    key={row.achievementId}
                    data-testid={`employment-rate-row-${row.achievementId}`}
                  >
                    <td>{row.managementNo}</td>
                    <td>{row.teacherName}</td>
                    <td>{row.achievementName}</td>
                    <td>{row.achievementDate}</td>
                    <td>{row.achievementStatus}</td>
                    <td>
                      <button
                        type="button"
                        data-testid={`employment-rate-detail-${row.achievementId}`}
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
          <div className="flex items-center gap-3">
            <button
              type="button"
              disabled={page === 0}
              data-testid="employment-rate-prev"
              onClick={() => setPage(page - 1)}
            >
              이전
            </button>
            <span>총 {total}건</span>
            <button
              type="button"
              disabled={(page + 1) * size >= total}
              data-testid="employment-rate-next"
              onClick={() => setPage(page + 1)}
            >
              다음
            </button>
          </div>
          <form
            className="space-y-4 rounded-md border border-ld p-4"
            onSubmit={(event) => {
              event.preventDefault();
              save();
            }}
          >
            <h2 className="text-lg font-semibold">
              {selected ? "실적 상세·수정" : "실적 등록"}
            </h2>
            {locked && (
              <p role="status">확정 또는 제출된 실적은 변경할 수 없습니다.</p>
            )}
            {!canWrite && <p>조회 전용입니다.</p>}
            {selected && <p>평가연도: {selected.evaluationYear} (수정 불가)</p>}
            {!items.length && <p>교원 입력 가능한 관리항목 설정이 없습니다.</p>}
            <label className="block">
              관리항목 *
              <select
                value={form.managementItemCode}
                required
                className={inputClass}
                disabled={locked || !canWrite || !owned}
                data-testid="employment-rate-item"
                onChange={(event) =>
                  setForm({ ...form, managementItemCode: event.target.value })
                }
              >
                <option value="">선택하세요</option>
                {items.map((item) => (
                  <option
                    key={`${item.evaluationYear}-${item.managementItemCode}`}
                    value={item.managementItemCode}
                  >
                    {item.managementItemName} ({item.evaluationYear})
                  </option>
                ))}
              </select>
              {fields.managementItemCode && (
                <span role="alert">{fields.managementItemCode}</span>
              )}
            </label>
            <label className="block">
              업적발생일 *
              <input
                type="date"
                required
                value={form.achievementDate}
                className={inputClass}
                disabled={locked || !canWrite || !owned}
                data-testid="employment-rate-date"
                onChange={(event) =>
                  setForm({ ...form, achievementDate: event.target.value })
                }
              />
              {fields.achievementDate && (
                <span role="alert">{fields.achievementDate}</span>
              )}
            </label>
            <label className="block">
              실적명 *
              <input
                required
                value={form.achievementName}
                className={inputClass}
                disabled={locked || !canWrite || !owned}
                data-testid="employment-rate-name"
                onChange={(event) =>
                  setForm({ ...form, achievementName: event.target.value })
                }
              />
              {fields.achievementName && (
                <span role="alert">{fields.achievementName}</span>
              )}
            </label>
            {canWrite && (
              <div className="flex gap-3">
                <button
                  className={buttonClass}
                  data-testid="employment-rate-save"
                  disabled={busy || locked || !owned || !items.length}
                  type="submit"
                >
                  저장
                </button>
                <button
                  type="button"
                  data-testid="employment-rate-new"
                  onClick={() => {
                    setSelected(null);
                    setForm(initial);
                    setFields({});
                  }}
                >
                  신규
                </button>
              </div>
            )}
          </form>
        </div>
      )}
      {tab === "excel" && canExcel && (
        <div
          role="tabpanel"
          data-testid="employment-rate-excel-panel"
          className="space-y-4 rounded-md border border-ld p-4"
        >
          <h2 className="text-lg font-semibold">
            Excel 검증 → 결과 확인 → 전체 반영
          </h2>
          <div className="flex flex-wrap gap-3">
            <button
              type="button"
              className={buttonClass}
              data-testid="employment-rate-template"
              onClick={() =>
                void act(() =>
                  employmentRateDownload(
                    "/excel-uploads/template",
                    "template.xlsx",
                  ),
                )
              }
            >
              양식 다운로드
            </button>
            <input
              type="file"
              accept=".xlsx"
              aria-label="Excel 파일"
              data-testid="employment-rate-file"
              onChange={(event) => setFile(event.target.files?.[0] ?? null)}
            />
            <button
              type="button"
              className={buttonClass}
              disabled={!file || busy}
              data-testid="employment-rate-validate"
              onClick={() => void validate()}
            >
              전체 검증
            </button>
          </div>
          {upload && (
            <div className="space-y-3">
              <p>
                전체 {upload.totalCount}건 / 정상 {upload.successCount}건 / 오류{" "}
                {upload.errorCount}건 / 반영 {upload.savedCount}건
              </p>
              {errors.map((row, index) => (
                <p key={index} data-testid={`employment-rate-error-${index}`}>
                  {row.rowNumber}행: {row.errorReason}
                </p>
              ))}
              <button
                type="button"
                disabled={
                  busy || upload.errorCount > 0 || upload.savedCount > 0
                }
                className={buttonClass}
                data-testid="employment-rate-commit"
                onClick={commit}
              >
                확인 후 전체 반영
              </button>
              <button
                type="button"
                data-testid="employment-rate-errors-download"
                onClick={() =>
                  void act(() =>
                    employmentRateDownload(
                      `/excel-uploads/${encodeURIComponent(upload.uploadId)}/errors/download`,
                      "errors.xlsx",
                    ),
                  )
                }
              >
                오류파일 다운로드
              </button>
            </div>
          )}
          <h3 className="font-semibold">업로드 이력</h3>
          {!histories.length && <p>업로드 이력이 없습니다.</p>}
          {histories.map((history) => (
            <p
              key={history.uploadId}
              data-testid={`employment-rate-history-${history.uploadId}`}
            >
              {history.originalFileName}: 전체 {history.totalCount} / 오류{" "}
              {history.errorCount} / 반영 {history.savedCount}
            </p>
          ))}
          <h3 className="font-semibold">일괄 생성·삭제</h3>
          <p>
            생성 자격 및 삭제 허용 상태 정책이 승인되지 않아 실행할 수 없습니다.
          </p>
          <button
            type="button"
            disabled
            className={buttonClass}
            data-testid="employment-rate-bulk-execute"
          >
            일괄 실행
          </button>
          <label className="block">
            보존 작업 식별자
            <input
              value={jobId}
              className={inputClass}
              data-testid="employment-rate-job-id"
              onChange={(event) => setJobId(event.target.value)}
            />
          </label>
          <button
            type="button"
            disabled={!jobId || busy}
            data-testid="employment-rate-job-search"
            onClick={() =>
              void act(async () => {
                setJob(
                  await employmentRateRequest<Job>(
                    `/bulk-jobs/${encodeURIComponent(jobId)}`,
                  ),
                );
              })
            }
          >
            처리결과 조회
          </button>
          {job && (
            <div>
              <p>
                처리 {job.processedCount}건 / 미처리 {job.unprocessedCount}건
              </p>
              {job.items.map((item) => (
                <p
                  key={item.targetUserId}
                  data-testid={`employment-rate-job-item-${item.targetUserId}`}
                >
                  대상 {item.targetUserId}:{" "}
                  {item.processedYn === "Y" ? "처리" : "미처리"}{" "}
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
