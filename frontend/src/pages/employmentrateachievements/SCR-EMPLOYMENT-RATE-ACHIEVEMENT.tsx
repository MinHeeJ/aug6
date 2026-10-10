import { useEffect, useState } from "react";
import { ApiClientError, type CurrentUser } from "../../api/apiClient";
import {
  employmentRateApi,
  type EmploymentAchievement,
  type EmploymentInput,
  type EmploymentSearch,
  type EmploymentUpload,
  type EmploymentJob,
} from "./employmentRateApi";

const emptyForm: EmploymentInput = {
  managementItemCode: "",
  achievementDate: "",
  title: "",
  attachmentRef: null,
};
const control =
  "rounded-md border border-border bg-white px-3 py-2 text-sm text-dark";
const button =
  "rounded-md bg-primary px-4 py-2 text-sm text-white disabled:opacity-50";

export function EmploymentRateAchievementPage({
  user,
}: {
  user: CurrentUser | null;
}) {
  const roles = user?.roles ?? [];
  const canRead = roles.some((role) =>
    ["R01", "R02", "R04", "R09"].includes(role),
  );
  const canWrite = roles.some((role) => ["R01", "R09"].includes(role));
  const canExecute = roles.some((role) => ["R07", "R09"].includes(role));
  const [tab, setTab] = useState(canRead ? "individual" : "excel");
  const [data, setData] = useState<EmploymentSearch>();
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [filter, setFilter] = useState("");
  const [selected, setSelected] = useState<EmploymentAchievement>();
  const [form, setForm] = useState<EmploymentInput>(emptyForm);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [fields, setFields] = useState<Record<string, string>>({});
  const [file, setFile] = useState<File>();
  const [upload, setUpload] = useState<EmploymentUpload>();
  const [histories, setHistories] = useState<EmploymentUpload[]>([]);
  const [jobId, setJobId] = useState("");
  const [job, setJob] = useState<EmploymentJob>();
  const editable =
    !selected ||
    ["DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED"].includes(
      selected.achievementStatus,
    );
  const owned =
    !selected ||
    selected.teacherUserId === user?.userId ||
    roles.includes("R09");
  const locked = !canWrite || !editable || !owned || busy;
  const query = new URLSearchParams({
    page: String(page),
    pageSize: String(pageSize),
  });
  if (filter) query.set("managementNo", filter);

  function fail(caught: unknown) {
    setError(caught instanceof Error ? caught.message : "요청에 실패했습니다.");
    if (caught instanceof ApiClientError) {
      setFields(
        Object.fromEntries(
          (caught.apiError?.fields ?? []).map((f) => [f.field, f.message]),
        ),
      );
      if (caught.status === 403)
        setError("권한이 없습니다. 역할과 데이터 범위를 확인하세요.");
    }
  }
  async function load() {
    setLoading(true);
    setError("");
    try {
      if (canRead) setData((await employmentRateApi.list(query)).data);
      if (canExecute)
        setHistories((await employmentRateApi.histories()).data ?? []);
    } catch (caught) {
      fail(caught);
    } finally {
      setLoading(false);
    }
  }
  useEffect(() => {
    void load();
  }, [page, pageSize]);

  async function detail(id: number) {
    setBusy(true);
    setError("");
    try {
      const row = (await employmentRateApi.detail(id)).data;
      if (row) {
        setSelected(row);
        setForm({
          managementItemCode: row.managementItemCode,
          achievementDate: row.achievementDate,
          title: row.title,
          attachmentRef: row.attachmentRef ?? null,
        });
        setFields({});
      }
    } catch (caught) {
      fail(caught);
    } finally {
      setBusy(false);
    }
  }
  async function save() {
    if (locked) return;
    const missing = Object.fromEntries(
      ["managementItemCode", "achievementDate", "title"]
        .filter((key) => !form[key as keyof EmploymentInput])
        .map((key) => [key, "필수 항목을 입력하세요."]),
    );
    setFields(missing);
    if (
      Object.keys(missing).length ||
      !window.confirm("실적을 저장하시겠습니까?")
    )
      return;
    setBusy(true);
    setError("");
    setSuccess("");
    try {
      const result = (
        await employmentRateApi.save(form, selected?.achievementId)
      ).data;
      setSuccess(
        result?.occurredDateWarning ? result.warningMessage : "저장했습니다.",
      );
      if (result?.achievement) await detail(result.achievement.achievementId);
      await load();
    } catch (caught) {
      fail(caught);
    } finally {
      setBusy(false);
    }
  }
  async function validate() {
    if (!file) {
      setError("XLSX 파일을 선택하세요.");
      return;
    }
    setBusy(true);
    setError("");
    setUpload(undefined);
    try {
      const result = await employmentRateApi.upload(file);
      setUpload(result.data);
      await load();
      if (!result.success)
        setError("오류행이 있어 업무 데이터는 0건 반영되었습니다.");
    } catch (caught) {
      fail(caught);
    } finally {
      setBusy(false);
    }
  }
  async function commit() {
    if (
      !upload ||
      upload.validationStatus !== "VALIDATED" ||
      !window.confirm("검증된 전체 행을 원자 반영하시겠습니까?")
    )
      return;
    setBusy(true);
    setError("");
    try {
      const result = (await employmentRateApi.commit(upload.uploadId)).data;
      setSuccess(`${result?.savedCount ?? 0}건 반영했습니다.`);
      setUpload(undefined);
      await load();
    } catch (caught) {
      fail(caught);
    } finally {
      setBusy(false);
    }
  }
  async function download(suffix: string, name: string) {
    try {
      await employmentRateApi.download(suffix, name);
    } catch (caught) {
      fail(caught);
    }
  }
  async function result() {
    if (!jobId.trim()) {
      setError("작업 ID를 입력하세요.");
      return;
    }
    setBusy(true);
    setError("");
    setJob(undefined);
    try {
      setJob((await employmentRateApi.job(jobId.trim())).data);
    } catch (caught) {
      fail(caught);
    } finally {
      setBusy(false);
    }
  }
  if (!canRead && !canExecute)
    return <p role="alert">취업률 실적 접근 권한이 없습니다.</p>;
  return (
    <section
      data-screen-id="SCR-EMPLOYMENT-RATE-ACHIEVEMENT"
      data-testid="employment-rate-page"
    >
      <div className="mb-6 rounded-md bg-lightsecondary p-6">
        <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          취업률 실적 관리
        </h1>
      </div>
      <div
        className="mb-4 flex flex-wrap gap-2"
        role="tablist"
        aria-label="취업률 실적"
      >
        {canRead && (
          <button
            className={button}
            data-testid="individual-tab"
            onClick={() => setTab("individual")}
            role="tab"
            aria-selected={tab === "individual"}
          >
            개별 실적
          </button>
        )}
        {canExecute && (
          <button
            className={button}
            data-testid="excel-tab"
            onClick={() => setTab("excel")}
            role="tab"
            aria-selected={tab === "excel"}
          >
            Excel 등록
          </button>
        )}
        {canExecute && (
          <button
            className={button}
            data-testid="bulk-tab"
            onClick={() => setTab("bulk")}
            role="tab"
            aria-selected={tab === "bulk"}
          >
            일괄 처리결과
          </button>
        )}
      </div>
      {loading && <p role="status">조회 중입니다.</p>}
      {error && (
        <p className="mb-3 text-error" role="alert">
          {error}
        </p>
      )}
      {success && (
        <p className="mb-3 text-success" role="status">
          {success}
        </p>
      )}
      {tab === "individual" && canRead && (
        <div
          className="space-y-4"
          role="tabpanel"
          data-testid="individual-panel"
        >
          <div className="flex flex-wrap gap-2">
            <input
              aria-label="관리번호 검색"
              className={control}
              data-testid="management-no-filter"
              value={filter}
              onChange={(event) => setFilter(event.target.value)}
            />
            <button
              className={button}
              data-testid="search-button"
              onClick={() => void load()}
            >
              조회
            </button>
            <select
              aria-label="표시 건수"
              className={control}
              data-testid="page-size"
              value={pageSize}
              onChange={(event) => {
                setPage(0);
                setPageSize(Number(event.target.value));
              }}
            >
              {[20, 50, 100].map((size) => (
                <option key={size} value={size}>
                  {size}건
                </option>
              ))}
            </select>
            <button
              className={button}
              data-testid="download-button"
              onClick={() =>
                void download(`/download?${query}`, "취업률_실적.xlsx")
              }
            >
              Excel 다운로드
            </button>
            {canWrite && (
              <button
                className={button}
                data-testid="new-button"
                onClick={() => {
                  setSelected(undefined);
                  setForm(emptyForm);
                  setFields({});
                }}
              >
                신규
              </button>
            )}
          </div>
          {!loading && data?.achievements.length === 0 && (
            <p>조회된 실적이 없습니다.</p>
          )}
          <div className="overflow-x-auto rounded-md bg-white p-4">
            <table className="w-full text-left text-sm">
              <thead>
                <tr>
                  <th>관리번호</th>
                  <th>교원</th>
                  <th>실적명</th>
                  <th>상태</th>
                  <th>상세</th>
                </tr>
              </thead>
              <tbody>
                {data?.achievements.map((row) => (
                  <tr
                    key={row.achievementId}
                    data-testid={`achievement-row-${row.achievementId}`}
                  >
                    <td>{row.managementNo}</td>
                    <td>{row.teacherName}</td>
                    <td>{row.title}</td>
                    <td>{row.achievementStatus}</td>
                    <td>
                      <button
                        className={button}
                        data-testid={`detail-${row.achievementId}`}
                        disabled={busy}
                        onClick={() => void detail(row.achievementId)}
                      >
                        상세
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="flex gap-3">
            <button
              data-testid="previous-page"
              disabled={page === 0}
              onClick={() => setPage(page - 1)}
            >
              이전
            </button>
            <span>
              {page + 1} 페이지 / {data?.totalElements ?? 0}건
            </span>
            <button
              data-testid="next-page"
              disabled={(page + 1) * pageSize >= (data?.totalElements ?? 0)}
              onClick={() => setPage(page + 1)}
            >
              다음
            </button>
          </div>
          <div className="grid gap-3 rounded-md bg-white p-6 sm:grid-cols-2">
            {selected && (
              <p>
                관리번호: {selected.managementNo} / 평가연도:{" "}
                {selected.evaluationYear}
              </p>
            )}
            {!editable && (
              <p role="status">확정 또는 제출된 실적은 수정할 수 없습니다.</p>
            )}
            {!owned && <p>본인 실적만 수정할 수 있습니다.</p>}
            <label>
              관리항목 *
              <select
                className={control}
                data-testid="management-item"
                disabled={locked}
                value={form.managementItemCode}
                onChange={(event) =>
                  setForm({ ...form, managementItemCode: event.target.value })
                }
              >
                <option value="">선택하세요</option>
                {(data?.managementItems ?? []).map((item) => (
                  <option
                    key={`${item.code}-${item.evaluationYear}`}
                    value={item.code}
                  >
                    {item.name}
                  </option>
                ))}
              </select>
              {fields.managementItemCode && (
                <span role="alert">{fields.managementItemCode}</span>
              )}
            </label>
            <label>
              업적발생일 *
              <input
                type="date"
                className={control}
                data-testid="achievement-date"
                disabled={locked}
                value={form.achievementDate}
                onChange={(event) =>
                  setForm({ ...form, achievementDate: event.target.value })
                }
              />
              {fields.achievementDate && (
                <span role="alert">{fields.achievementDate}</span>
              )}
            </label>
            <label>
              실적명 *
              <input
                className={control}
                data-testid="achievement-title"
                disabled={locked}
                value={form.title}
                onChange={(event) =>
                  setForm({ ...form, title: event.target.value })
                }
              />
              {fields.title && <span role="alert">{fields.title}</span>}
            </label>
            <label>
              첨부 참조
              <input
                className={control}
                data-testid="attachment-ref"
                disabled={locked}
                value={form.attachmentRef ?? ""}
                onChange={(event) =>
                  setForm({
                    ...form,
                    attachmentRef: event.target.value || null,
                  })
                }
              />
              {fields.attachmentRef && (
                <span role="alert">{fields.attachmentRef}</span>
              )}
            </label>
            {canWrite && (
              <button
                className={button}
                data-testid="save-button"
                disabled={locked}
                onClick={() => void save()}
              >
                저장
              </button>
            )}
          </div>
        </div>
      )}
      {tab === "excel" && canExecute && (
        <div
          className="space-y-4 rounded-md bg-white p-6"
          role="tabpanel"
          data-testid="excel-panel"
        >
          <p>템플릿 다운로드 → 파일 선택 → 전체 검증 → 결과 확인 → 반영</p>
          <button
            className={button}
            data-testid="template-download"
            onClick={() =>
              void download("/excel-uploads/template", "취업률_템플릿.xlsx")
            }
          >
            템플릿 다운로드
          </button>
          <input
            type="file"
            accept=".xlsx"
            aria-label="Excel 파일"
            data-testid="excel-file"
            onChange={(event) => {
              setFile(event.target.files?.[0]);
              setUpload(undefined);
            }}
          />
          <button
            className={button}
            data-testid="validate-button"
            disabled={busy || !file}
            onClick={() => void validate()}
          >
            검증
          </button>
          {upload && (
            <div data-testid="upload-result">
              <p>
                전체 {upload.totalCount} / 정상 {upload.successCount} / 오류{" "}
                {upload.errorCount}건
              </p>
              <button
                className={button}
                data-testid="commit-button"
                disabled={busy || upload.validationStatus !== "VALIDATED"}
                onClick={() => void commit()}
              >
                전체 반영
              </button>
              {upload.errorCount > 0 && (
                <button
                  className={button}
                  data-testid="error-download"
                  onClick={() =>
                    void download(
                      `/excel-uploads/${encodeURIComponent(upload.uploadId)}/errors/download`,
                      "취업률_오류결과.xlsx",
                    )
                  }
                >
                  오류파일 다운로드
                </button>
              )}
            </div>
          )}
          <h2 className="text-lg font-semibold">업로드 이력</h2>
          {histories.length === 0 && <p>업로드 이력이 없습니다.</p>}
          {histories.map((history) => (
            <div
              key={history.uploadId}
              data-testid={`upload-history-${history.uploadId}`}
            >
              {history.originalFileName} / {history.validationStatus} / 반영{" "}
              {history.savedCount}건
            </div>
          ))}
        </div>
      )}
      {tab === "bulk" && canExecute && (
        <div
          className="space-y-4 rounded-md bg-white p-6"
          role="tabpanel"
          data-testid="bulk-panel"
        >
          <p>
            생성조건과 삭제 허용 상태가 미승인 상태입니다. 신규 일괄 실행은
            차단됩니다.
          </p>
          <button className={button} data-testid="bulk-execute" disabled>
            일괄 실행 (미승인)
          </button>
          <label>
            기존 작업 ID
            <input
              className={control}
              data-testid="job-id"
              value={jobId}
              onChange={(event) => setJobId(event.target.value)}
            />
          </label>
          <button
            className={button}
            data-testid="job-search"
            disabled={busy}
            onClick={() => void result()}
          >
            결과 조회
          </button>
          {job && (
            <div data-testid="job-result">
              <p>
                전체 {job.totalCount} / 처리 {job.processedCount} / 미처리{" "}
                {job.unprocessedCount}건
              </p>
              {job.items.map((item) => (
                <p
                  key={item.targetUserId}
                  data-testid={`job-item-${item.targetUserId}`}
                >
                  대상 {item.targetUserId} /{" "}
                  {item.processedYn === "Y" ? "처리" : "미처리"} /{" "}
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
