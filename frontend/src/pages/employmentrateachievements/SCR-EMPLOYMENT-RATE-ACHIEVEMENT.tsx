import { useEffect, useState } from "react";
import { ApiClientError } from "../../api/apiClient";
import { useAuth } from "../../app/AuthProvider";
import {
  EmptyState,
  ErrorState,
  LoadingState,
  PermissionState,
  SuccessState,
} from "../../components/States";
import {
  excelApi,
  type ExcelTemplateRow,
  type ExcelUploadErrorRow,
  type ExcelUploadHistoryRow,
  type ExcelUploadResult,
} from "../admin/ExcelOperationsPages";
import {
  BUSINESS_TYPE,
  employmentRateApi,
  saveXlsx,
  type Achievement,
  type AchievementInput,
  type BulkResult,
} from "./employmentRateApi";
const blank = (): AchievementInput => ({
  managementItemCode: "",
  achievementDate: "",
  achievementName: "",
  attachmentIds: [],
});
const button =
  "rounded-md bg-primary px-4 py-2 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-50";
const input =
  "mt-1 w-full rounded-md border border-border bg-white p-2 text-sm text-dark disabled:bg-lightgray";
const panel =
  "space-y-4 rounded-md border border-border bg-white p-5 shadow-sm";
type Tab = "records" | "upload" | "bulk";
export function EmploymentRateAchievementPage() {
  const { user } = useAuth();
  const isAdministrator = user?.roles.includes("R09") ?? false;
  const canRead =
    user?.roles.some((role) => ["R01", "R02", "R04", "R09"].includes(role)) ??
    false;
  const canWrite = (user?.roles.includes("R01") ?? false) || isAdministrator;
  const canUpload = (user?.roles.includes("R07") ?? false) || isAdministrator;
  const [tab, setTab] = useState<Tab>(canRead ? "records" : "upload");
  const [rows, setRows] = useState<Achievement[]>([]);
  const [selected, setSelected] = useState<Achievement | null>(null);
  const [form, setForm] = useState<AchievementInput>(blank);
  const [detailValid, setDetailValid] = useState(true);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [warning, setWarning] = useState("");
  const [fields, setFields] = useState<string[]>([]);
  const [templates, setTemplates] = useState<ExcelTemplateRow[]>([]);
  const [templateId, setTemplateId] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [validation, setValidation] = useState<ExcelUploadResult | null>(null);
  const [uploadAccepted, setUploadAccepted] = useState(false);
  const [committed, setCommitted] = useState(false);
  const [histories, setHistories] = useState<ExcelUploadHistoryRow[]>([]);
  const [uploadId, setUploadId] = useState("");
  const [errors, setErrors] = useState<ExcelUploadErrorRow[]>([]);
  const [jobId, setJobId] = useState("");
  const [job, setJob] = useState<BulkResult | null>(null);
  const [evaluationYear, setEvaluationYear] = useState("");
  const [actionType, setActionType] = useState("GENERATE");
  const [targetCondition, setTargetCondition] = useState("{}");
  const locked =
    !canWrite ||
    busy ||
    !detailValid ||
    (!!selected &&
      (!["DRAFT", "DEPARTMENT_REJECTED", "CERTIFICATION_REJECTED"].includes(
        selected.achievementStatus,
      ) ||
        (!isAdministrator && selected.teacherUserId !== user?.userId)));
  const canCommit =
    canUpload &&
    !busy &&
    !committed &&
    uploadAccepted &&
    !!validation?.uploadId &&
    ["VALIDATED", "VALID", "NORMAL"].includes(validation.validationStatus) &&
    validation.errorCount === 0 &&
    (validation.errors ?? []).length === 0 &&
    validation.savedCount === 0;
  function fail(caught: unknown) {
    setError(
      caught instanceof Error ? caught.message : "요청을 처리하지 못했습니다.",
    );
    setFields(
      caught instanceof ApiClientError
        ? (caught.apiError?.fields ?? []).map(
            (field) => `${field.field}: ${field.message}`,
          )
        : [],
    );
  }
  function clearMessages() {
    setError("");
    setFields([]);
    setSuccess("");
    setWarning("");
  }
  async function load() {
    if (!canRead) return;
    setLoading(true);
    try {
      const response = await employmentRateApi.list(page, pageSize);
      setRows(response.data?.achievements ?? []);
      setTotal(response.data?.totalElements ?? 0);
    } catch (caught) {
      setRows([]);
      setTotal(0);
      fail(caught);
    } finally {
      setLoading(false);
    }
  }
  useEffect(() => {
    if (canRead) void load();
  }, [canRead, page, pageSize]);
  useEffect(() => {
    if (!canUpload || tab !== "upload") return;
    void refreshUpload();
  }, [canUpload, tab]);
  function applyDetail(detail: Achievement) {
    setSelected(detail);
    setForm({
      managementItemCode: detail.managementItemCode,
      achievementDate: detail.achievementDate,
      achievementName: detail.achievementName ?? "",
      attachmentIds: detail.attachmentIds ?? [],
    });
    setDetailValid(true);
  }
  async function select(id: number) {
    if (!canRead || busy) return;
    clearMessages();
    setBusy(true);
    setDetailValid(false);
    try {
      const response = await employmentRateApi.detail(id);
      if (!response.data) throw new Error("상세 응답이 없습니다.");
      applyDetail(response.data);
    } catch (caught) {
      fail(caught);
    } finally {
      setBusy(false);
    }
  }
  async function save() {
    if (locked) return;
    clearMessages();
    if (!form.managementItemCode.trim() || !form.achievementDate) {
      setError("관리항목과 실적일자를 입력하세요.");
      return;
    }
    if (!window.confirm("취업률 실적을 저장하시겠습니까?")) return;
    setBusy(true);
    let accepted = false;
    try {
      const payload: AchievementInput = {
        managementItemCode: form.managementItemCode.trim(),
        achievementDate: form.achievementDate,
        achievementName: form.achievementName.trim(),
        attachmentIds: form.attachmentIds,
      };
      const response = selected
        ? await employmentRateApi.update(selected.achievementId, payload)
        : await employmentRateApi.create(payload);
      accepted = true;
      setDetailValid(false);
      const result = response.data;
      if (result?.occurredDateWarning)
        setWarning(
          result.warningMessage ?? "업적발생일이 평가기간 밖에 있습니다.",
        );
      if (!result?.achievement?.achievementId)
        throw new Error("저장 응답에 실적 ID가 없습니다.");
      const persisted = await employmentRateApi.detail(
        result.achievement.achievementId,
      );
      if (!persisted.data) throw new Error("저장된 상세 응답이 없습니다.");
      applyDetail(persisted.data);
      setSuccess("저장되었습니다. 저장된 상세를 다시 조회했습니다.");
      await load();
    } catch (caught) {
      if (accepted)
        setSuccess(
          "저장은 완료되었으나 상세 재조회에 실패했습니다. 상세를 다시 조회하세요.",
        );
      fail(caught);
    } finally {
      setBusy(false);
    }
  }
  async function refreshUpload() {
    if (!canUpload) return;
    setLoading(true);
    try {
      const [templateResponse, historyResponse] = await Promise.all([
        excelApi.listTemplates({
          businessType: BUSINESS_TYPE,
          size: pageSize,
        }),
        excelApi.listHistories({
          uploadId: uploadId.trim() || undefined,
          size: pageSize,
        }),
      ]);
      setTemplates(templateResponse.data?.templates ?? []);
      setHistories(historyResponse.data?.histories ?? []);
    } catch (caught) {
      fail(caught);
    } finally {
      setLoading(false);
    }
  }
  function invalidateUpload() {
    setValidation(null);
    setUploadAccepted(false);
    setCommitted(false);
    setErrors([]);
    clearMessages();
  }
  async function upload() {
    if (!canUpload || busy || !file) return;
    if (!file.name.toLowerCase().endsWith(".xlsx")) {
      setError(".xlsx 파일을 선택하세요.");
      return;
    }
    if (
      !window.confirm(
        "파일을 검증하시겠습니까? 오류·중복 행이 있으면 전체 반영이 차단됩니다.",
      )
    )
      return;
    clearMessages();
    setBusy(true);
    setValidation(null);
    setUploadAccepted(false);
    setCommitted(false);
    setErrors([]);
    try {
      const { body, ok, status } = await employmentRateApi.upload(file);
      // Preserve diagnostic data even when validation returns HTTP 400.
      if (body.data) {
        setValidation(body.data);
        setUploadId(body.data.uploadId);
        setErrors(body.data.errors ?? []);
        setCommitted(body.data.savedCount > 0);
        setUploadAccepted(ok);
      }
      if (!ok) {
        fail(
          new ApiClientError(
            status,
            body.error?.message ??
              "검증 오류가 있습니다. 업무 자료는 반영되지 않았습니다.",
            body.error,
          ),
        );
      } else if (!body.data) {
        throw new Error("업로드 검증 결과가 없습니다.");
      } else {
        setSuccess(
          body.data.savedCount > 0
            ? `${body.data.savedCount}건 반영되었습니다.`
            : "검증결과를 확인한 뒤 반영하세요.",
        );
      }
      await refreshUpload();
    } catch (caught) {
      fail(caught);
    } finally {
      setBusy(false);
    }
  }
  async function commit() {
    if (
      !canCommit ||
      !validation ||
      !window.confirm("검증된 전체 자료를 반영하시겠습니까?")
    )
      return;
    clearMessages();
    setBusy(true);
    try {
      const response = await excelApi.commitUpload(validation.uploadId);
      setCommitted(true);
      setUploadAccepted(false);
      setSuccess(`${response.data?.savedCount ?? 0}건 반영되었습니다.`);
      await refreshUpload();
      if (canRead) await load();
    } catch (caught) {
      fail(caught);
    } finally {
      setBusy(false);
    }
  }
  async function historyErrors(id: string) {
    if (!canUpload || busy || !id.trim()) return;
    clearMessages();
    setUploadId(id);
    setBusy(true);
    try {
      setErrors(
        (
          await excelApi.listErrors({
            uploadId: id,
            size: pageSize,
          })
        ).data?.errors ?? [],
      );
    } catch (caught) {
      fail(caught);
    } finally {
      setBusy(false);
    }
  }
  async function download(kind: "list" | "template" | "errors") {
    if (busy || (!canRead && !canUpload) || (kind !== "list" && !canUpload))
      return;
    clearMessages();
    setBusy(true);
    try {
      const response =
        kind === "list"
          ? await employmentRateApi.download(page, pageSize)
          : kind === "template"
            ? await excelApi.downloadTemplate(templateId)
            : await excelApi.downloadErrors(uploadId);
      await saveXlsx(response, `employment-rate-${kind}.xlsx`);
      setSuccess("XLSX 파일을 다운로드했습니다.");
    } catch (caught) {
      fail(caught);
    } finally {
      setBusy(false);
    }
  }
  async function readJob() {
    if (!canUpload || busy || !jobId.trim()) return;
    clearMessages();
    setBusy(true);
    setJob(null);
    try {
      const response = await employmentRateApi.bulkResult(jobId.trim());
      if (!response.data) throw new Error("작업 결과가 없습니다.");
      setJob(response.data);
    } catch (caught) {
      fail(caught);
    } finally {
      setBusy(false);
    }
  }
  if (!canRead && !canUpload)
    return (
      <PermissionState
        title="취업률 실적 관리 권한이 없습니다"
        message="R01, R02, R04, R07 또는 R09 역할이 필요합니다."
      />
    );
  return (
    <section
      className="space-y-6"
      data-screen-id="SCR-EMPLOYMENT-RATE-ACHIEVEMENT"
      data-testid="employment-rate-achievement-page"
    >
      <header className="rounded-md bg-lightsecondary p-6">
        <p className="text-sm text-link">업적 입력 관리 / 교육영역</p>
        <h1 className="mt-2 text-xl font-semibold text-dark">
          취업률 실적 관리
        </h1>
        <p className="mt-2 text-sm text-muted">
          개별 실적 조회·등록·수정, Excel 검증·반영 및 일괄 처리결과를
          확인합니다.
        </p>
      </header>
      <div
        className="flex flex-wrap gap-2"
        role="tablist"
        aria-label="취업률 관리 기능"
      >
        {canRead && (
          <button
            data-testid="employment-records-tab"
            className={button}
            role="tab"
            aria-selected={tab === "records"}
            onClick={() => setTab("records")}
          >
            개별 실적
          </button>
        )}
        {canUpload && (
          <button
            data-testid="employment-upload-tab"
            className={button}
            role="tab"
            aria-selected={tab === "upload"}
            onClick={() => setTab("upload")}
          >
            Excel 업로드
          </button>
        )}
        {canUpload && (
          <button
            data-testid="employment-bulk-tab"
            className={button}
            role="tab"
            aria-selected={tab === "bulk"}
            onClick={() => setTab("bulk")}
          >
            일괄 처리결과
          </button>
        )}
        <button
          data-testid="employment-download-list"
          className={button}
          disabled={busy}
          onClick={() => void download("list")}
        >
          목록 XLSX 다운로드
        </button>
      </div>
      {error && <ErrorState title="처리 오류" message={error} />}
      {fields.length > 0 && (
        <ul className="text-sm text-error" role="alert">
          {fields.map((text, index) => (
            <li key={`${index}-${text}`}>{text}</li>
          ))}
        </ul>
      )}
      {success && <SuccessState title="처리 완료" message={success} />}
      {warning && (
        <p
          role="status"
          className="rounded-md bg-lightwarning p-4 text-sm"
          data-testid="employment-date-warning"
        >
          {warning}
        </p>
      )}
      {(loading || busy) && (
        <LoadingState
          title="처리 중"
          message="서버 응답을 기다리고 있습니다."
        />
      )}
      {tab === "records" && canRead && (
        <div
          className="space-y-5"
          role="tabpanel"
          data-testid="employment-records-panel"
        >
          <section className={panel}>
            <div className="flex flex-wrap items-end gap-3">
              <label className="text-sm">
                페이지 크기
                <select
                  data-testid="employment-page-size"
                  className={input}
                  value={pageSize}
                  disabled={busy || loading}
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
              </label>
              <button
                data-testid="employment-search"
                className={button}
                disabled={loading || busy}
                onClick={() => {
                  clearMessages();
                  void load();
                }}
              >
                조회
              </button>
              <span className="text-sm text-muted">총 {total}건</span>
            </div>
            {!loading && rows.length === 0 && (
              <EmptyState
                title="조회된 취업률 실적이 없습니다."
                message="새 실적을 등록하거나 조회를 다시 시도하세요."
              />
            )}
            {rows.length > 0 && (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead className="bg-lightgray">
                    <tr>
                      {[
                        "관리번호",
                        "교원",
                        "관리항목",
                        "실적일자",
                        "실적명",
                        "인증상태",
                        "상세",
                      ].map((heading) => (
                        <th className="p-3" key={heading}>
                          {heading}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {rows.map((row) => (
                      <tr
                        className="border-b border-border"
                        key={row.achievementId}
                        data-testid={`employment-row-${row.achievementId}`}
                      >
                        <td className="p-3">{row.managementNo}</td>
                        <td>{row.teacherName}</td>
                        <td>{row.managementItemCode}</td>
                        <td>{row.achievementDate}</td>
                        <td>{row.achievementName}</td>
                        <td>{row.certificationStatus}</td>
                        <td>
                          <button
                            data-testid={`employment-detail-${row.achievementId}`}
                            className={button}
                            disabled={busy}
                            aria-label={`상세 ${row.managementNo}`}
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
                data-testid="employment-previous"
                className={button}
                disabled={page === 0 || loading || busy}
                onClick={() => setPage((value) => value - 1)}
              >
                이전
              </button>
              <span>{page + 1} 페이지</span>
              <button
                data-testid="employment-next"
                className={button}
                disabled={(page + 1) * pageSize >= total || loading || busy}
                onClick={() => setPage((value) => value + 1)}
              >
                다음
              </button>
            </div>
          </section>
          <section className={panel} data-testid="employment-detail-panel">
            <div className="flex flex-wrap justify-between gap-3">
              <h2 className="font-semibold">실적 상세 / 편집</h2>
              {canWrite && (
                <button
                  data-testid="employment-new"
                  className={button}
                  disabled={busy}
                  onClick={() => {
                    setSelected(null);
                    setForm(blank());
                    setDetailValid(true);
                    clearMessages();
                  }}
                >
                  신규 등록
                </button>
              )}
            </div>
            {selected && (
              <p className="text-sm">
                관리번호: {selected.managementNo} / 인증상태:{" "}
                {selected.certificationStatus}
              </p>
            )}
            {selected && locked && !busy && (
              <p className="text-sm text-muted">
                선택한 실적은 읽기 전용입니다.
              </p>
            )}
            {!detailValid && (
              <p role="alert">상세를 다시 조회하거나 신규 등록을 선택하세요.</p>
            )}
            <div className="grid gap-4 md:grid-cols-2">
              <label className="text-sm" htmlFor="employment-management-code">
                관리항목 코드
                <input
                  data-testid="employment-management-code"
                  id="employment-management-code"
                  className={input}
                  value={form.managementItemCode}
                  disabled={locked}
                  onChange={(event) =>
                    setForm({
                      ...form,
                      managementItemCode: event.target.value,
                    })
                  }
                />
              </label>
              <label className="text-sm" htmlFor="employment-date">
                실적일자
                <input
                  data-testid="employment-date"
                  id="employment-date"
                  className={input}
                  type="date"
                  value={form.achievementDate}
                  disabled={locked}
                  onChange={(event) =>
                    setForm({
                      ...form,
                      achievementDate: event.target.value,
                    })
                  }
                />
              </label>
              <label className="text-sm" htmlFor="employment-name">
                실적명
                <input
                  data-testid="employment-name"
                  id="employment-name"
                  className={input}
                  value={form.achievementName}
                  disabled={locked}
                  onChange={(event) =>
                    setForm({
                      ...form,
                      achievementName: event.target.value,
                    })
                  }
                />
              </label>
            </div>
            <p className="text-sm text-muted">
              첨부파일은 읽기 전용으로 유지합니다. 이 화면에서는 신규 파일을
              첨부하지 않습니다.
            </p>
            <ul className="text-sm">
              {form.attachmentIds.map((id) => (
                <li key={id}>{id}</li>
              ))}
            </ul>
            {canWrite && (
              <button
                data-testid="employment-save"
                className={button}
                disabled={locked}
                onClick={() => void save()}
              >
                저장
              </button>
            )}
          </section>
        </div>
      )}
      {tab === "upload" && canUpload && (
        <div
          className="space-y-5"
          role="tabpanel"
          data-testid="employment-upload-panel"
        >
          <section className={panel}>
            <h2 className="font-semibold">
              표준 템플릿 → 업로드 → 검증결과 확인 → 반영
            </h2>
            <p className="text-sm text-muted">
              오류·중복이 한 건이라도 있으면 전체 반영이 차단되며 기존 실적은
              자동 갱신되지 않습니다.
            </p>
            <label className="block text-sm">
              표준 템플릿
              <select
                data-testid="employment-template"
                className={input}
                value={templateId}
                disabled={busy}
                onChange={(event) => {
                  setTemplateId(event.target.value);
                  invalidateUpload();
                }}
              >
                <option value="">템플릿 선택</option>
                {templates.map((template) => (
                  <option value={template.templateId} key={template.templateId}>
                    {template.templateVersion}
                    {" / "}
                    {template.effectiveDate}
                  </option>
                ))}
              </select>
            </label>
            {templates.length === 0 && !loading && (
              <p className="text-sm">등록된 업무 템플릿이 없습니다.</p>
            )}
            <button
              data-testid="employment-download-template"
              className={button}
              disabled={busy || !templateId}
              onClick={() => void download("template")}
            >
              템플릿 XLSX 다운로드
            </button>
            <label className="block text-sm">
              Excel 파일
              <input
                data-testid="employment-file"
                className={input}
                type="file"
                accept=".xlsx"
                disabled={busy}
                onChange={(event) => {
                  setFile(event.target.files?.[0] ?? null);
                  invalidateUpload();
                }}
              />
            </label>
            <div className="flex flex-wrap gap-3">
              <button
                data-testid="employment-upload"
                className={button}
                disabled={busy || !file}
                onClick={() => void upload()}
              >
                업로드 및 검증
              </button>
              <button
                data-testid="employment-commit"
                className={button}
                disabled={!canCommit}
                onClick={() => void commit()}
              >
                검증 자료 반영
              </button>
            </div>
            {validation && (
              <div
                data-testid="employment-validation-result"
                className="rounded-md bg-lightgray p-4 text-sm"
              >
                <p>
                  업로드 ID: {validation.uploadId} / 검증상태:{" "}
                  {validation.validationStatus}
                </p>
                <p>
                  {"전체 "}
                  {validation.totalCount}
                  {"건 / 정상 "}
                  {validation.successCount}
                  {"건 / 오류 "}
                  {validation.errorCount}
                  {"건 / 제외 "}
                  {validation.excludedCount}
                  {"건 / 반영 "}
                  {validation.savedCount}
                  {"건"}
                </p>
                {committed && (
                  <p>반영 완료: 동일 업로드를 다시 반영할 수 없습니다.</p>
                )}
              </div>
            )}
          </section>
          <section className={panel}>
            <h2 className="font-semibold">업로드 이력 및 오류</h2>
            <label className="block text-sm">
              업로드 ID
              <input
                data-testid="employment-upload-id"
                className={input}
                value={uploadId}
                disabled={busy}
                onChange={(event) => {
                  setUploadId(event.target.value);
                  setErrors([]);
                }}
              />
            </label>
            <div className="flex flex-wrap gap-3">
              <button
                data-testid="employment-history-search"
                className={button}
                disabled={busy || loading}
                onClick={() => {
                  clearMessages();
                  void refreshUpload();
                }}
              >
                이력 조회
              </button>
              <button
                data-testid="employment-errors-search"
                className={button}
                disabled={busy || !uploadId.trim()}
                onClick={() => void historyErrors(uploadId)}
              >
                오류 조회
              </button>
              <button
                data-testid="employment-download-errors"
                className={button}
                disabled={busy || !uploadId.trim()}
                onClick={() => void download("errors")}
              >
                오류 XLSX 다운로드
              </button>
            </div>
            {histories.length === 0 && !loading && (
              <p className="text-sm">업로드 이력이 없습니다.</p>
            )}
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr>
                    <th>파일명</th>
                    <th>전체 / 정상 / 오류 / 반영</th>
                    <th>처리일시</th>
                    <th>오류</th>
                  </tr>
                </thead>
                <tbody>
                  {histories.map((history) => (
                    <tr
                      className="border-b border-border"
                      key={history.uploadId}
                      data-testid={`employment-history-${history.uploadId}`}
                    >
                      <td className="p-3">{history.originalFileName}</td>
                      <td>
                        {history.totalCount}
                        {" / "}
                        {history.successCount}
                        {" / "}
                        {history.errorCount}
                        {" / "}
                        {history.savedCount}
                      </td>
                      <td>{history.processedAt}</td>
                      <td>
                        <button
                          data-testid={`employment-history-errors-${history.uploadId}`}
                          className={button}
                          disabled={busy}
                          onClick={() => void historyErrors(history.uploadId)}
                        >
                          {"오류 "}
                          {history.uploadId}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr>
                    <th>행</th>
                    <th>열</th>
                    <th>입력값</th>
                    <th>오류 코드</th>
                    <th>오류 사유</th>
                    <th>수정 안내</th>
                  </tr>
                </thead>
                <tbody>
                  {errors.map((row, index) => (
                    <tr
                      className="border-b border-border"
                      key={row.errorId ?? index}
                      data-testid={`employment-upload-error-${row.errorId ?? index}`}
                    >
                      <td className="p-3">{row.rowNumber}</td>
                      <td>{row.columnName}</td>
                      <td>{row.inputValue}</td>
                      <td>{row.errorCode}</td>
                      <td>{row.errorReason}</td>
                      <td>{row.correctionGuide}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        </div>
      )}
      {tab === "bulk" && canUpload && (
        <section
          className={panel}
          role="tabpanel"
          data-testid="employment-bulk-panel"
        >
          <h2 className="font-semibold">일괄 생성·삭제 / 기존 처리결과</h2>
          <p role="alert" className="rounded-md bg-lightwarning p-4 text-sm">
            생성 대상·삭제 허용 상태 정책이 미확정입니다. 정책 승인 및 서버 대상
            미리보기 계약이 마련될 때까지 일괄 실행은 차단됩니다. 기존 작업
            결과는 조회할 수 있습니다.
          </p>
          <div className="grid gap-4 md:grid-cols-2">
            <label className="text-sm">
              평가연도
              <input
                data-testid="employment-evaluation-year"
                className={input}
                value={evaluationYear}
                onChange={(event) => setEvaluationYear(event.target.value)}
              />
            </label>
            <label className="text-sm">
              일괄 작업
              <select
                data-testid="employment-action-type"
                className={input}
                value={actionType}
                onChange={(event) => setActionType(event.target.value)}
              >
                <option value="GENERATE">생성</option>
                <option value="DELETE">삭제</option>
              </select>
            </label>
            <label className="text-sm md:col-span-2">
              대상 조건 (JSON)
              <textarea
                data-testid="employment-target-condition"
                className={input}
                value={targetCondition}
                onChange={(event) => setTargetCondition(event.target.value)}
              />
            </label>
          </div>
          <p className="text-sm text-muted">
            {"입력 조건 요약 (대상 미리보기 아님): "}
            {evaluationYear || "연도 미입력"}
            {" / "}
            {actionType}
            {" / "}
            {targetCondition}
          </p>
          <button
            data-testid="employment-bulk-execute"
            className={button}
            disabled
          >
            일괄 실행 (정책 승인 대기)
          </button>
          <label className="block text-sm">
            작업 ID
            <input
              data-testid="employment-job-id"
              className={input}
              value={jobId}
              disabled={busy}
              onChange={(event) => {
                setJobId(event.target.value);
                setJob(null);
              }}
            />
          </label>
          <button
            data-testid="employment-job-search"
            className={button}
            disabled={busy || !jobId.trim()}
            onClick={() => void readJob()}
          >
            결과 조회
          </button>
          {!job && !busy && (
            <p className="text-sm">기존 작업 ID로 처리결과를 조회하세요.</p>
          )}
          {job && (
            <div className="space-y-3" data-testid="employment-bulk-result">
              <p>
                작업 {job.jobId} / 상태 {job.status}
              </p>
              <p>
                처리 {job.processedCount}건 / 미처리 {job.unprocessedCount}건
              </p>
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead>
                    <tr>
                      <th>대상 교원</th>
                      <th>처리 여부</th>
                      <th>미처리 사유</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(job.items ?? []).map((item, index) => (
                      <tr
                        key={`${item.targetUserId}-${index}`}
                        data-testid={`employment-bulk-item-${item.targetUserId}-${index}`}
                      >
                        <td className="p-3">{item.targetUserId}</td>
                        <td>{item.processedYn}</td>
                        <td>{item.unprocessedReason}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </section>
      )}
    </section>
  );
}
